package id.josjis.maracas

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class MainActivity : Activity(), android.hardware.SensorEventListener {
    private val yellow = Color.parseColor("#FFC107")
    private lateinit var proximityDot: View
    private lateinit var proximityValue: TextView
    private lateinit var batteryValue: TextView
    private lateinit var masterSwitch: MaterialSwitch
    private var proximityNear = false
    private var proximitySensor: android.hardware.Sensor? = null
    private lateinit var sensorManager: android.hardware.SensorManager

    private fun prefs() = getSharedPreferences("p", Context.MODE_PRIVATE)
    private val dp: Float get() = resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val p = prefs()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(20), px(12), px(20), px(28))
        }

        // Large, expressive app header with the master control in the same row.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, px(8), 0, px(20))
        }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(TextView(this).apply {
            text = "Maracas"
            textSize = 32f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        titleColumn.addView(TextView(this).apply {
            text = "Wake your screen with a gesture"
            textSize = 14f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        masterSwitch = MaterialSwitch(this).apply {
            text = "Enabled"
            textSize = 14f
            isChecked = p.getBoolean("enabled", false)
            setOnCheckedChangeListener { _, checked ->
                p.edit().putBoolean("enabled", checked).apply()
                applyServiceState()
                updateBatteryStatus()
            }
        }
        header.addView(masterSwitch)
        page.addView(header)

        page.addView(sectionHeading("Wake gestures"))
        page.addView(settingCard(
            "Shake to wake",
            "A few deliberate shakes wake the screen while it is off. Higher sensitivity makes lighter shakes count.",
            "shake", true
        ), verticalParams(bottom = 10))
        page.addView(settingCard(
            "Pick up / flip to wake",
            "Uses the proximity sensor to detect a near-to-far transition while the screen is off. Sensor behavior varies by device; this is not a dedicated lift-detection sensor.",
            "pickup", true
        ), verticalParams(bottom = 10))
        page.addView(settingCard(
            "Ignore shakes while in pocket",
            "When the proximity sensor reports that something is close to the phone, Maracas ignores shake events to reduce accidental wake-ups in a pocket or bag. This also depends on the sensor being present and behaving correctly.",
            "pocket", true
        ), verticalParams(bottom = 10))

        val sensitivityCard = card()
        val sensitivityContent = column(16, 14).apply {
            addView(TextView(this@MainActivity).apply {
                text = "Shake sensitivity"
                textSize = 18f
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Adjust how much movement is needed"
                textSize = 14f
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
            val slider = Slider(this@MainActivity).apply {
                valueFrom = 0f
                valueTo = 100f
                stepSize = 1f
                value = p.getInt("sens", 50).toFloat()
                contentDescription = "Shake sensitivity"
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        p.edit().putInt("sens", value.roundToInt()).apply()
                    }
                }
                addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {}
                    override fun onStopTrackingTouch(slider: Slider) { applyServiceState() }
                })
            }
            addView(slider)
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(TextView(this@MainActivity).apply {
                    text = "Harder shake"
                    textSize = 12f
                    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(TextView(this@MainActivity).apply {
                    text = "Lighter shake"
                    textSize = 12f
                    gravity = Gravity.END
                    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                }, LinearLayout.LayoutParams(0, -2, 1f))
            })
        }
        sensitivityCard.addView(sensitivityContent)
        page.addView(sensitivityCard, verticalParams(bottom = 12))

        page.addView(sectionHeading("Live status"))
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val proximityCard = card()
        val proximityCol = column(12, 10).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(TextView(this@MainActivity).apply {
                text = "Proximity sensor"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            proximityDot = View(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(px(2).coerceAtLeast(2), yellow)
                }
            }
            addView(proximityDot, LinearLayout.LayoutParams(px(22), px(22)))
            proximityValue = TextView(this@MainActivity).apply {
                text = "Checking…"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            }
            addView(proximityValue)
        }
        proximityCard.addView(proximityCol)

        val batteryCard = card().apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { showInfo("Battery impact", "Maracas does not currently measure battery drain in mAh or report an estimated battery percentage. When enabled, it keeps a foreground service and sensor listeners active; actual impact depends on your phone, sensor hardware, ROM and usage. Use Android’s Battery settings for measured app usage.") }
        }
        val batteryCol = column(10, 8)
        val batteryTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "Battery impact"
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(infoButton { showInfo("Battery impact", "Maracas does not currently measure battery drain in mAh or report an estimated battery percentage. When enabled, it keeps a foreground service and sensor listeners active; actual impact depends on your phone, sensor hardware, ROM and usage. Use Android’s Battery settings for measured app usage.") })
        }
        batteryCol.addView(batteryTitleRow)
        batteryValue = TextView(this).apply {
            textSize = 14f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        batteryCol.addView(batteryValue)
        batteryCard.addView(batteryCol)
        statusRow.addView(proximityCard, LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = px(6) })
        statusRow.addView(batteryCard, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = px(6) })
        page.addView(statusRow, verticalParams(bottom = 18))

        page.addView(sectionHeading("More controls"))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val batteryButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Allow unrestricted battery"
            textSize = 13f
            insetTop = 0; insetBottom = 0
            setOnClickListener { openBatterySettings() }
        }
        val lockButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Turn screen off"
            textSize = 13f
            insetTop = 0; insetBottom = 0
            setOnClickListener { lockScreen() }
        }
        actions.addView(batteryButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = px(6) })
        actions.addView(lockButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = px(6) })
        page.addView(actions)

        setContentView(ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            addView(page)
        })

        updateBatteryStatus()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        proximitySensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            proximityValue.text = "Unavailable"
            proximityDot.visibility = View.INVISIBLE
        } else {
            proximityValue.text = "Waiting for sensor…"
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL) }
        updateBatteryStatus()
    }

    override fun onPause() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        if (event.sensor.type != android.hardware.Sensor.TYPE_PROXIMITY) return
        proximityNear = event.values[0] < minOf(5f, event.sensor.maximumRange)
        (proximityDot.background as? GradientDrawable)?.apply {
            setColor(if (proximityNear) yellow else Color.TRANSPARENT)
        }
        proximityValue.text = if (proximityNear) "NEAR" else "FAR"
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}

    private fun settingCard(title: String, description: String, key: String, default: Boolean): View {
        val p = prefs()
        val c = card()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), px(10), px(8), px(10))
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(infoButton { showInfo(title, description) })
        val toggle = MaterialSwitch(this).apply {
            isChecked = p.getBoolean(key, default)
            contentDescription = title
            setOnCheckedChangeListener { _, checked ->
                p.edit().putBoolean(key, checked).apply()
                applyServiceState()
                updateBatteryStatus()
            }
        }
        row.addView(toggle)
        c.addView(row)
        return c
    }

    private fun card(): MaterialCardView = MaterialCardView(this).apply {
        radius = px(24).toFloat()
        cardElevation = 0f
        strokeWidth = px(1)
        strokeColor = themeColor(com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainerLow))
        useCompatPadding = false
    }

    private fun column(horizontal: Int, vertical: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(px(horizontal), px(vertical), px(horizontal), px(vertical))
    }

    private fun sectionHeading(label: String) = TextView(this).apply {
        text = label
        textSize = 21f
        setPadding(px(2), px(12), px(2), px(10))
        setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun infoButton(onClick: () -> Unit) = android.widget.ImageButton(this).apply {
        setImageResource(android.R.drawable.ic_menu_info_details)
        imageTintList = android.content.res.ColorStateList.valueOf(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(themeColor(android.R.attr.colorControlHighlight)),
            null, null
        )
        contentDescription = "More information"
        setPadding(px(10), px(10), px(10), px(10))
        setOnClickListener { onClick() }
    }

    private fun showInfo(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun verticalParams(bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        bottomMargin = px(bottom)
    }

    private fun themeColor(attr: Int): Int {
        val a = android.util.TypedValue()
        theme.resolveAttribute(attr, a, true)
        if (a.resourceId != 0) return ContextCompat.getColor(this, a.resourceId)
        return a.data
    }

    private fun updateBatteryStatus() {
        val active = prefs().getBoolean("enabled", false) &&
            (prefs().getBoolean("shake", true) || prefs().getBoolean("pickup", true))
        batteryValue.text = if (active) "Sensor monitoring active" else "Monitoring paused"
    }

    private fun applyServiceState() {
        val p = prefs()
        val on = p.getBoolean("enabled", false) &&
            (p.getBoolean("shake", true) || p.getBoolean("pickup", true))
        val i = Intent(this, WakeService::class.java)
        if (on) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
        } else stopService(i)
    }

    private fun openBatterySettings() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Unrestricted battery use is already allowed", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Open Settings > Apps > Maracas > Battery and select Unrestricted", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MaracasAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Allow screen lock")
                .setMessage("Android requires Maracas to be enabled as a device administrator before it can turn the screen off. This permission is only used by the Turn screen off button.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows Maracas to lock the screen when you tap Turn screen off.")
                    })
                }
                .show()
        }
    }
}
