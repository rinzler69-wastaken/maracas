package id.josjis.maracas

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import kotlin.math.sqrt

@Suppress("DEPRECATION")
class WakeService : Service(), SensorEventListener {
    private lateinit var sm: SensorManager
    private lateinit var pm: PowerManager
    private var cpuLock: PowerManager.WakeLock? = null
    private var shakeCount = 0
    private var lastShake = 0L
    private var near = false
    private var lastWake = 0L
    private var threshold = 3.0f      // in g; lower = more sensitive
    private var ignoreInPocket = true

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("w", "Maracas", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "w")
            .setContentTitle("Maracas is active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)

        pm = getSystemService(PowerManager::class.java)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.unregisterListener(this)
        val p = getSharedPreferences("p", Context.MODE_PRIVATE)

        // slider 0..100 -> threshold 4.0g (hard) .. 1.8g (light)
        threshold = 4.0f - 2.2f * (p.getInt("sens", 50) / 100f)
        ignoreInPocket = p.getBoolean("pocket", true)
        val shake = p.getBoolean("shake", true)
        val pickup = p.getBoolean("pickup", true)

        if (cpuLock == null)
            cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "maracas:cpu").apply { acquire() }
        if (shake)
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        // proximity is needed for pickup wake and for the in-pocket check
        if (pickup || (shake && ignoreInPocket)) {
            val prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, true) ?: sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            prox?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
        pickupEnabled = pickup
        return START_STICKY
    }

    private var pickupEnabled = true

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (pm.isInteractive) return
                if (ignoreInPocket && near) return
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / 9.81f
                if (g > threshold) {
                    val now = System.currentTimeMillis()
                    if (now - lastShake < 150) return
                    if (now - lastShake > 1000) shakeCount = 0
                    lastShake = now
                    if (++shakeCount >= 3) { shakeCount = 0; wake() }
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val wasNear = near
                near = e.values[0] < minOf(5f, e.sensor.maximumRange)
                if (pickupEnabled && wasNear && !near && !pm.isInteractive) wake()
            }
        }
    }

    private fun wake() {
        val now = System.currentTimeMillis()
        if (now - lastWake < 2000) return
        lastWake = now
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "maracas:screen"
        ).acquire(3000)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onDestroy() {
        sm.unregisterListener(this)
        cpuLock?.release(); cpuLock = null
        super.onDestroy()
    }
}
