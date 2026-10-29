package id.josjis.maracas

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import kotlin.math.abs
import kotlin.math.sqrt

@Suppress("DEPRECATION")
class WakeService : Service(), SensorEventListener {
    private lateinit var sm: SensorManager
    private lateinit var pm: PowerManager
    private var cpuLock: PowerManager.WakeLock? = null

    // Shake detector state
    private var shakeEnabled = true
    private var shakeCount = 0
    private var lastShake = 0L
    private var shakeThreshold = 3.0f      // in g (lower = more sensitive)

    // Lift detector state
    private var liftEnabled = true
    private var liftFirm = false
    private var liftArmed = false
    private var restStartTime = 0L
    private var liftMotionStartTime = 0L

    // Reach / Flip state
    private var reachFlipEnabled = true
    private var lastFaceDownTime = 0L          // Last time phone was face-down (z <= -5.0) while near
    private var lastPocketOrientationTime = 0L  // Last time phone was vertically oriented (|y| >= 5.0) while near
    private var lastExtractionMotionTime = 0L   // Last time significant linear motion was detected
    private var pendingReachFlipUntil = 0L     // Short window after near->far to verify flip rotation or extraction motion
    private var curX = 0f
    private var curY = 0f
    private var curZ = 0f

    // Pocket protection (proximity near suppresses shake and lift)
    private var pocketProtection = true
    private var near = false

    // General cooldown
    private var lastWake = 0L

    // Fallback gravity filter if linear acceleration sensor is unavailable
    private var hasLinearSensor = false
    private val gravity = FloatArray(3)

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("w", "Maracas", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "w")
            .setContentTitle("Maracas is active")
            .setSmallIcon(R.drawable.ic_stat_maracas)
            .setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)

        pm = getSystemService(PowerManager::class.java)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.unregisterListener(this)

        val p = getSharedPreferences("p", Context.MODE_PRIVATE)
        val masterEnabled = p.getBoolean("enabled", false)

        shakeEnabled = p.getBoolean("shake", true)
        liftEnabled = p.getBoolean("lift", true)
        liftFirm = p.getString("lift_sens", "gentle") == "firm"
        reachFlipEnabled = p.getBoolean("reach_flip", p.getBoolean("pickup", true))
        pocketProtection = p.getBoolean("pocket", true)

        // Shake slider 0..100 -> threshold 4.0g (hard) .. 1.8g (light)
        shakeThreshold = 4.0f - 2.2f * (p.getInt("sens", 50) / 100f)

        val anyModeActive = masterEnabled && (shakeEnabled || liftEnabled || reachFlipEnabled)
        if (!anyModeActive) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (cpuLock == null) {
            cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "maracas:cpu").apply { acquire() }
        }

        // Register sensors
        val needAccel = shakeEnabled || reachFlipEnabled || liftEnabled
        if (needAccel) {
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        if (liftEnabled || reachFlipEnabled) {
            val lin = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            if (lin != null) {
                hasLinearSensor = true
                sm.registerListener(this, lin, SensorManager.SENSOR_DELAY_GAME)
            } else {
                hasLinearSensor = false
            }
        }

        if (reachFlipEnabled || pocketProtection) {
            val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, true) ?: sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            prox?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }

        return START_STICKY
    }

    override fun onSensorChanged(e: SensorEvent) {
        val now = System.currentTimeMillis()
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Low-pass filter to extract gravity / orientation
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * e.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * e.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * e.values[2]
                curX = gravity[0]
                curY = gravity[1]
                curZ = gravity[2]

                if (near) {
                    if (curZ <= -5.0f) lastFaceDownTime = now
                    if (abs(curY) >= 5.0f) lastPocketOrientationTime = now
                }

                // Shake detection
                if (shakeEnabled && !pm.isInteractive) {
                    if (!(pocketProtection && near)) {
                        val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / 9.81f
                        if (g > shakeThreshold) {
                            if (now - lastShake >= 150) {
                                if (now - lastShake > 1000) shakeCount = 0
                                lastShake = now
                                if (++shakeCount >= 3) {
                                    shakeCount = 0
                                    wake()
                                    return
                                }
                            }
                        }
                    }
                }

                // High-pass linear acceleration if hardware linear sensor is absent
                val lx = e.values[0] - gravity[0]
                val ly = e.values[1] - gravity[1]
                val lz = e.values[2] - gravity[2]
                val linMag = sqrt(lx * lx + ly * ly + lz * lz)
                if (linMag >= 1.3f) {
                    lastExtractionMotionTime = now
                }

                if (liftEnabled && !hasLinearSensor && !pm.isInteractive) {
                    processLinearAcceleration(lx, ly, lz, now)
                }

                // Check pending reach/flip
                if (reachFlipEnabled && pendingReachFlipUntil > now && !pm.isInteractive) {
                    if (checkReachOrFlip(now)) {
                        pendingReachFlipUntil = 0L
                        wake()
                    }
                }
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val linMag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                if (linMag >= 1.3f) {
                    lastExtractionMotionTime = now
                }

                if (liftEnabled && !pm.isInteractive) {
                    processLinearAcceleration(e.values[0], e.values[1], e.values[2], now)
                }

                // Check pending reach/flip
                if (reachFlipEnabled && pendingReachFlipUntil > now && !pm.isInteractive) {
                    if (checkReachOrFlip(now)) {
                        pendingReachFlipUntil = 0L
                        wake()
                    }
                }
            }

            Sensor.TYPE_PROXIMITY -> {
                val wasNear = near
                near = e.values[0] < minOf(5f, e.sensor.maximumRange)

                // Reach / Flip: near-to-far transition verified with physical motion or flip rotation
                if (reachFlipEnabled && wasNear && !near && !pm.isInteractive) {
                    if (checkReachOrFlip(now)) {
                        wake()
                    } else {
                        // Arm short window to allow completing the flip rotation or extraction movement
                        pendingReachFlipUntil = now + 650L
                    }
                }
            }
        }
    }

    private fun checkReachOrFlip(now: Long): Boolean {
        if (pm.isInteractive) return false

        // 1. Flip: Phone was resting face-down (z <= -5.0 m/s²) within last 1500ms, and is now face-up (z >= 2.0 m/s²)
        val wasFaceDown = (now - lastFaceDownTime) <= 1500L
        val isFaceUpNow = curZ >= 2.0f
        if (wasFaceDown && isFaceUpNow) {
            lastFaceDownTime = 0L
            return true
        }

        // 2. Reach: Phone was vertically oriented in pocket (|y| >= 5.0 m/s²) within last 1500ms,
        // and concurrent/recent extraction motion was detected (within 650ms), without still being face-down
        val wasInPocket = (now - lastPocketOrientationTime) <= 1500L
        val hasExtractionMotion = (now - lastExtractionMotionTime) <= 650L
        val notFaceDown = curZ > -3.0f
        if (wasInPocket && hasExtractionMotion && notFaceDown) {
            lastPocketOrientationTime = 0L
            return true
        }

        return false
    }

    private fun processLinearAcceleration(x: Float, y: Float, z: Float, now: Long) {
        if (pocketProtection && near) {
            liftArmed = false
            return
        }

        val mag = sqrt(x * x + y * y + z * z)

        // Rest threshold: device must rest quietly (e.g. on desk or stationary in hand)
        val restThreshold = 0.45f
        if (mag < restThreshold) {
            if (restStartTime == 0L) {
                restStartTime = now
            } else if (now - restStartTime > 250) {
                liftArmed = true
            }
            liftMotionStartTime = 0L
            return
        }

        // Device is moving
        restStartTime = 0L
        if (!liftArmed) return

        // Pickup threshold: Gentle vs Firm
        // Gentle: lighter pickup movement (~1.3 m/s²)
        // Firm: pronounced pickup movement (~2.6 m/s²)
        val triggerThreshold = if (liftFirm) 2.6f else 1.3f

        if (mag >= triggerThreshold) {
            if (liftMotionStartTime == 0L) {
                liftMotionStartTime = now
            } else if (now - liftMotionStartTime >= 120) {
                // Plausible sustained pickup movement detected
                liftArmed = false
                liftMotionStartTime = 0L
                wake()
            }
        }
    }

    private fun wake() {
        val now = System.currentTimeMillis()
        if (now - lastWake < 2000) return
        lastWake = now
        pendingReachFlipUntil = 0L
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "maracas:screen"
        ).acquire(3000)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onDestroy() {
        sm.unregisterListener(this)
        cpuLock?.release()
        cpuLock = null
        super.onDestroy()
    }
}
