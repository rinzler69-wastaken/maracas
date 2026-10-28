package com.example.maracas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private fun prefs() = getSharedPreferences("p", Context.MODE_PRIVATE)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val p = prefs()
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
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
        root.addView(Button(this).apply {
            text = "Allow unrestricted battery"
            setOnClickListener {
                val pm = getSystemService(PowerManager::class.java)
                if (!pm.isIgnoringBatteryOptimizations(packageName))
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                else Toast.makeText(this@MainActivity, "Already allowed", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun apply() {
        val i = Intent(this, WakeService::class.java)
        val p = prefs()
        val on = p.getBoolean("enabled", false) && (p.getBoolean("shake", true) || p.getBoolean("pickup", true))
        if (on) startForegroundService(i) else stopService(i)
    }
}
