package id.josjis.maracas

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val ui = Handler(Looper.getMainLooper())
    private var prox: Sensor? = null
    private var proxNear: Boolean? = null            // null until the first sensor reading arrives
    private var tracking = false                      // slider is being dragged

    private lateinit var headerCard: MaterialCardView
    private lateinit var headerIcon: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var masterSwitch: MaterialSwitch
    private lateinit var masterState: TextView
    private lateinit var shakeSwitch: MaterialSwitch
    private lateinit var pickupRow: View
    private lateinit var pocketRow: View
    private lateinit var sensSlider: Slider
    private lateinit var sensValue: TextView
    private lateinit var proxDot: View
    private lateinit var proxValue: TextView
    private lateinit var proxCaption: TextView
    private lateinit var batteryValue: TextView
    private lateinit var batteryCaption: TextView
    private lateinit var batteryOpt: TextView
    private lateinit var adminStatus: TextView
    private lateinit var btnAdminRemove: MaterialButton

    private var headerColor = 0

    private val adminComponent by lazy { ComponentName(this, MaracasAdminReceiver::class.java) }

    private val adminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshAdmin()
            toast(if (isAdminActive()) R.string.admin_enabled else R.string.admin_not_enabled)
        }

    private fun prefs() = getSharedPreferences("p", Context.MODE_PRIVATE)

    // Persists the slider value once, after dragging stops or after keyboard/accessibility changes settle.
    private val commitSens = Runnable {
        val v = sensSlider.value.roundToInt()
        if (v != prefs().getInt("sens", 50)) {
            prefs().edit().putInt("sens", v).apply()
            syncService()
        }
    }

    override fun onCreate(b: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)   // Material You on Android 12+, fallback palette otherwise
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        setContentView(R.layout.activity_main)
        styleSystemBars()
        bindViews()

        val p = prefs()

        // Master switch ("enabled", default false)
        masterSwitch.isChecked = p.getBoolean("enabled", false)
        masterSwitch.setOnCheckedChangeListener { _, v ->
            prefs().edit().putBoolean("enabled", v).apply()
            syncService()
            renderHeader(animate = true, wiggle = v)
        }
        renderHeader(animate = false, wiggle = false)

        // Gesture rows: shake / pickup / pocket (defaults unchanged: all true)
        shakeSwitch = bindRow(findViewById(R.id.rowShake), R.drawable.ic_vibration,
            R.string.shake_title, R.string.shake_summary, "shake", true, R.string.shake_info)
        bindRow(pickupRow, R.drawable.ic_smartphone,
            R.string.pickup_title, R.string.pickup_summary, "pickup", true, R.string.pickup_info)
        bindRow(pocketRow, R.drawable.ic_pocket,
            R.string.pocket_title, R.string.pocket_summary, "pocket", true, R.string.pocket_info)

        // Sensitivity ("sens", 0..100, default 50). Left = harder shake (4.0 g), right = lighter (1.8 g).
        sensSlider.value = p.getInt("sens", 50).coerceIn(0, 100).toFloat()
        updateSensLabel(sensSlider.value.roundToInt())
        sensSlider.isEnabled = shakeSwitch.isChecked
        sensSlider.addOnChangeListener { _, value, _ ->
            updateSensLabel(value.roundToInt())
            if (!tracking) {
                ui.removeCallbacks(commitSens)
                ui.postDelayed(commitSens, 400)
            }
        }
        sensSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                tracking = true
                ui.removeCallbacks(commitSens)
            }
            override fun onStopTrackingTouch(slider: Slider) {
                tracking = false
                commitSens.run()
            }
        })
        findViewById<ImageButton>(R.id.sensInfo).apply {
            contentDescription = getString(R.string.info_cd, getString(R.string.sens_title))
            setOnClickListener { showInfo(R.string.sens_title, R.string.sens_info) }
        }
        findViewById<ImageButton>(R.id.batteryInfo).apply {
            contentDescription = getString(R.string.info_cd, getString(R.string.battery_title))
            setOnClickListener { showInfo(R.string.battery_title, R.string.battery_info) }
        }

        findViewById<MaterialButton>(R.id.btnBattery).setOnClickListener { requestUnrestrictedBattery() }
        findViewById<MaterialButton>(R.id.btnScreenOff).setOnClickListener { onScreenOff() }
        btnAdminRemove.setOnClickListener {
            getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(adminComponent)
            refreshAdmin()
            toast(R.string.admin_removed)
        }

        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (prox == null) {
            for (row in arrayOf(pickupRow, pocketRow))
                row.findViewById<TextView>(R.id.rowNote).apply { setText(R.string.no_proximity_note); visibility = View.VISIBLE }
        }
        refreshStatus()
        refreshAdmin()
    }

    override fun onResume() {
        super.onResume()
        prox?.let { (getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        refreshStatus()     // service/battery state can change while we were away
        refreshAdmin()      // admin can be revoked from system settings
    }

    override fun onPause() {
        super.onPause()
        (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
        ui.removeCallbacks(commitSens)
        commitSens.run()          // idempotent: only writes if the slider value differs from the saved one
        tracking = false
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ---- Views ----------------------------------------------------------------------------

    private fun bindViews() {
        headerCard = findViewById(R.id.headerCard)
        headerIcon = findViewById(R.id.headerIcon)
        headerTitle = findViewById(R.id.headerTitle)
        headerSubtitle = findViewById(R.id.headerSubtitle)
        masterSwitch = findViewById(R.id.masterSwitch)
        masterState = findViewById(R.id.masterState)
        pickupRow = findViewById(R.id.rowPickup)
        pocketRow = findViewById(R.id.rowPocket)
        sensSlider = findViewById(R.id.sensSlider)
        sensValue = findViewById(R.id.sensValue)
        proxDot = findViewById(R.id.proxDot)
        proxValue = findViewById(R.id.proxValue)
        proxCaption = findViewById(R.id.proxCaption)
        batteryValue = findViewById(R.id.batteryValue)
        batteryCaption = findViewById(R.id.batteryCaption)
        batteryOpt = findViewById(R.id.batteryOpt)
        adminStatus = findViewById(R.id.adminStatus)
        btnAdminRemove = findViewById(R.id.btnAdminRemove)
    }

    @Suppress("DEPRECATION")
    private fun styleSystemBars() {
        val surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        window.statusBarColor = surface
        window.navigationBarColor = surface
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    private fun bindRow(
        row: View, @DrawableRes icon: Int, @StringRes title: Int, @StringRes summary: Int,
        key: String, def: Boolean, @StringRes info: Int
    ): MaterialSwitch {
        val titleText = getString(title)
        row.findViewById<ImageView>(R.id.rowIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.rowTitle).text = titleText
        row.findViewById<TextView>(R.id.rowSummary).setText(summary)
        val sw = row.findViewById<MaterialSwitch>(R.id.rowSwitch)
        sw.contentDescription = titleText
        sw.isChecked = prefs().getBoolean(key, def)
        sw.setOnCheckedChangeListener { _, v ->
            prefs().edit().putBoolean(key, v).apply()
            if (key == "shake") sensSlider.isEnabled = v
            syncService()
            refreshStatus()
        }
        row.findViewById<View>(R.id.rowText).setOnClickListener { sw.toggle() }
        row.findViewById<ImageButton>(R.id.rowInfo).apply {
            contentDescription = getString(R.string.info_cd, titleText)
            setOnClickListener { showInfo(title, info) }   // separate view: never toggles the switch
        }
        return sw
    }

    private fun showInfo(@StringRes title: Int, @StringRes message: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.got_it, null)
            .show()
    }

    private fun toast(@StringRes res: Int, long: Boolean = false) =
        Toast.makeText(this, res, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    private fun animationsEnabled() =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    // ---- Header ---------------------------------------------------------------------------

    private fun renderHeader(animate: Boolean, wiggle: Boolean) {
        val on = masterSwitch.isChecked
        val bg = MaterialColors.getColor(headerCard,
            if (on) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHigh)
        val fg = MaterialColors.getColor(headerCard,
            if (on) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface)
        headerTitle.setTextColor(fg)
        headerSubtitle.setTextColor(fg)
        masterState.setTextColor(fg)
        masterState.setText(if (on) R.string.state_on else R.string.state_off)

        val motion = animate && animationsEnabled()
        if (motion && headerColor != 0 && headerColor != bg) {
            ValueAnimator.ofObject(ArgbEvaluator(), headerColor, bg).apply {
                duration = 250
                addUpdateListener { headerCard.setCardBackgroundColor(it.animatedValue as Int) }
            }.start()
        } else {
            headerCard.setCardBackgroundColor(bg)
        }
        headerColor = bg
        if (motion && wiggle) {   // a short maraca-style shake of the icon when turned on
            ObjectAnimator.ofFloat(headerIcon, View.ROTATION, 0f, -14f, 12f, -8f, 5f, 0f)
                .setDuration(450).start()
        }
    }

    // ---- Sensitivity ----------------------------------------------------------------------

    private fun updateSensLabel(v: Int) {
        val g = 4.0f - 2.2f * (v / 100f)      // same mapping as WakeService
        val level = when { v < 34 -> R.string.sens_low; v < 67 -> R.string.sens_medium; else -> R.string.sens_high }
        sensValue.text = getString(R.string.sens_value, getString(level), g)
    }

    // ---- Status ---------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun serviceRunning(): Boolean =
        getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == WakeService::class.java.name }

    private fun refreshStatus() {
        val p = prefs()
        val enabled = p.getBoolean("enabled", false)
        val shake = p.getBoolean("shake", true)
        val pickup = p.getBoolean("pickup", true)
        val pocket = p.getBoolean("pocket", true)

        renderProximity(proximityNeeded = enabled && (pickup || (shake && pocket)))

        val gesture = shake || pickup
        val running = enabled && gesture && serviceRunning()
        batteryValue.setText(if (running) R.string.battery_active else R.string.battery_inactive)
        batteryCaption.setText(when {
            running -> R.string.battery_caption_active
            !enabled -> R.string.battery_caption_off
            !gesture -> R.string.battery_caption_no_gesture
            else -> R.string.battery_caption_not_running
        })
        val unrestricted = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        batteryOpt.setText(if (unrestricted) R.string.battery_opt_unrestricted else R.string.battery_opt_optimized)
    }

    private fun renderProximity(proximityNeeded: Boolean = lastProxNeeded) {
        lastProxNeeded = proximityNeeded
        val near = proxNear
        when {
            prox == null -> {
                proxDot.setBackgroundResource(R.drawable.dot_unavailable)
                proxValue.setText(R.string.prox_unavailable)
                proxCaption.setText(R.string.prox_caption_unavailable)
                return
            }
            near == null -> { proxDot.setBackgroundResource(R.drawable.dot_clear); proxValue.setText(R.string.prox_checking) }
            near == true -> { proxDot.setBackgroundResource(R.drawable.dot_covered); proxValue.setText(R.string.prox_covered) }
            else -> { proxDot.setBackgroundResource(R.drawable.dot_clear); proxValue.setText(R.string.prox_clear) }
        }
        proxCaption.setText(if (proximityNeeded) R.string.prox_caption_used else R.string.prox_caption_unused)
    }

    private var lastProxNeeded = false

    override fun onSensorChanged(e: SensorEvent) {
        // Same near/far test as the original app and WakeService
        proxNear = e.values[0] < minOf(5f, e.sensor.maximumRange)
        renderProximity()
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // ---- Service control (unchanged semantics) --------------------------------------------

    private fun syncService() {
        val i = Intent(this, WakeService::class.java)
        val p = prefs()
        val on = p.getBoolean("enabled", false) && (p.getBoolean("shake", true) || p.getBoolean("pickup", true))
        if (on) startForegroundService(i) else stopService(i)
        // The service starts/stops asynchronously, so re-read its real state shortly afterwards.
        ui.postDelayed({ refreshStatus() }, 400)
        ui.postDelayed({ refreshStatus() }, 1500)
    }

    // ---- Battery optimisation (unchanged behaviour) ---------------------------------------

    private fun requestUnrestrictedBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast(R.string.battery_already)
        } else {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e2: Exception) {
                    toast(R.string.battery_manual, long = true)
                }
            }
        }
    }

    // ---- Turn screen off (device admin, force-lock only) ----------------------------------

    private fun isAdminActive() =
        getSystemService(DevicePolicyManager::class.java).isAdminActive(adminComponent)

    private fun refreshAdmin() {
        val active = isAdminActive()
        adminStatus.setText(if (active) R.string.admin_status_on else R.string.admin_status_off)
        btnAdminRemove.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun onScreenOff() {
        if (isAdminActive()) {
            try {
                getSystemService(DevicePolicyManager::class.java).lockNow()
            } catch (e: SecurityException) {
                refreshAdmin()
                toast(R.string.admin_lock_failed, long = true)
            }
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_dialog_title)
                .setMessage(R.string.admin_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.admin_continue) { _, _ -> launchAdminActivation() }
                .show()
        }
    }

    private fun launchAdminActivation() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_system_explanation))
        try {
            adminLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.admin_unavailable, long = true)
        }
    }
}
