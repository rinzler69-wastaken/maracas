package com.example.maracas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity(), SensorEventListener {
    private val yellow = Color.parseColor("#FFC107")
    private lateinit var dot: GradientDrawable
    private lateinit var status: TextView
    private var prox: Sensor? = null

    private fun prefs() = getSharedPreferences("p", Context.MODE_PRIVATE)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val d = resources.displayMetrics.density
        val pad = (24 * d).toInt()
        val p = prefs()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun sw(label: String, key: String, def: Boolean) = Switch(this).apply {
            text = label; textSize = 18f; isChecked = p.getBoolean(key, def)
            setPadding(0, pad / 2, 0, pad / 2)
            setOnCheckedChangeListener { _, v -> p.edit().putBoolean(key, v).apply(); apply() }
        }

        root.addView(sw("Enabled", "enabled", false))
        root.addView(sw("Shake to wake", "shake", true))
        root.addView(sw("Pick up / flip to wake", "pickup", true))
        root.addView(sw("Ignore shakes while in pocket", "pocket", true))

        root.addView(TextView(this).apply {
            text = "Shake sensitivity"; textSize = 18f; setPadding(0, pad, 0, 0)
        })
        root.addView(SeekBar(this).apply {
            max = 100; progress = p.getInt("sens", 50)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {}
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    p.edit().putInt("sens", s?.progress ?: 50).apply(); apply()
                }
            })
        })
        root.addView(TextView(this).apply { text = "Left: harder shake needed. Right: lighter shake."; textSize = 12f })

        root.addView(TextView(this).apply {
            text = "Pick up uses the proximity sensor: the screen wakes when the phone leaves a pocket or is flipped up from face-down."
            setPadding(0, pad, 0, pad)
        })

        // Proximity status row
        val size = (20 * d).toInt()
        dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke((2 * d).toInt(), yellow)
        }
        status = TextView(this).apply { text = "OFF"; textSize = 16f; minWidth = (40 * d).toInt() }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, pad / 2)
            addView(TextView(this@MainActivity).apply {
                text = "Proximity Sensor"; textSize = 18f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(View(this@MainActivity).apply { background = dot },
                LinearLayout.LayoutParams(size, size).apply { marginEnd = (12 * d).toInt() })
            addView(status)
        })

        root.addView(Button(this).apply {
            text = "Allow unrestricted battery"
            setOnClickListener {
                val pm = getSystemService(PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    Toast.makeText(this@MainActivity, "Already allowed", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")))
                    } catch (e: Exception) {
                        try {
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (e2: Exception) {
                            Toast.makeText(this@MainActivity,
                                "Open Settings > Apps > Maracas > Battery and set Unrestricted",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })

        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (prox == null) status.text = "N/A"
    }

    override fun onResume() {
        super.onResume()
        prox?.let { (getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val near = e.values[0] < minOf(5f, e.sensor.maximumRange)
        dot.setColor(if (near) yellow else Color.TRANSPARENT)
        status.text = if (near) "ON" else "OFF"
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun apply() {
        val i = Intent(this, WakeService::class.java)
        val p = prefs()
        val on = p.getBoolean("enabled", false) && (p.getBoolean("shake", true) || p.getBoolean("pickup", true))
        if (on) startForegroundService(i) else stopService(i)
    }
}
