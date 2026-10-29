package id.josjis.maracas

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
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
import android.service.quicksettings.TileService
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val ui = Handler(Looper.getMainLooper())
    private var prox: Sensor? = null
    private var tracking = false                      // slider is being dragged

    // Header
    private lateinit var headerCard: MaterialCardView
    private lateinit var headerTitle: TextView
    private lateinit var masterSwitch: MaterialSwitch
    private lateinit var masterState: TextView
    private var headerColor = 0

    // Wake method tiles (Shake & Lift)
    private lateinit var cardWakeShake: MaterialCardView
    private lateinit var ivShakeIcon: ImageView
    private lateinit var ivShakeCheck: ImageView
    private lateinit var tvShakeTitle: TextView
    private lateinit var tvShakeSubtitle: TextView
    private lateinit var btnShakeAdjust: ImageButton
    private lateinit var btnShakeInfo: ImageButton

    private lateinit var cardWakeLift: MaterialCardView
    private lateinit var ivLiftIcon: ImageView
    private lateinit var ivLiftCheck: ImageView
    private lateinit var tvLiftTitle: TextView
    private lateinit var tvLiftSubtitle: TextView
    private lateinit var btnLiftAdjust: ImageButton
    private lateinit var btnLiftInfo: ImageButton

    // Sensitivity panels (toggled on-demand via Adjust buttons)
    private var expandedSensMode: String? = null     // null, "shake", or "lift"

    private lateinit var layoutSensPlaceholder: LinearLayout
    private lateinit var tvSensPlaceholder: TextView
    private lateinit var cardShakeSens: MaterialCardView
    private lateinit var layoutShakeSensContent: View
    private lateinit var sensTitleValue: TextView
    private lateinit var sensSlider: Slider
    private lateinit var sensInfo: ImageButton

    private lateinit var cardLiftSens: MaterialCardView
    private lateinit var layoutLiftSensContent: View
    private lateinit var btnLiftSensInfo: ImageButton
    private lateinit var btnLiftGentle: MaterialCardView
    private lateinit var ivGentleCheck: ImageView
    private lateinit var tvGentleTitle: TextView
    private lateinit var btnLiftFirm: MaterialCardView
    private lateinit var ivFirmCheck: ImageView
    private lateinit var tvFirmTitle: TextView

    // Segmented rows (Reach / Flip & Pocket Protection)
    private lateinit var cardReachFlip: MaterialCardView
    private lateinit var cardPocket: MaterialCardView
    private lateinit var rowReachFlip: View
    private lateinit var rowPocket: View

    // Proximity
    private lateinit var proxIndicator: View
    private lateinit var proxStatus: TextView

    // Bottom action
    private lateinit var btnBattery: MaterialButton

    private fun prefs() = getSharedPreferences("p", Context.MODE_PRIVATE)

    // Synchronize UI immediately if enabled state is toggled externally (e.g. from Quick Settings tile)
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") {
            val enabled = prefs().getBoolean("enabled", false)
            if (masterSwitch.isChecked != enabled) {
                masterSwitch.isChecked = enabled
                renderHeader(animate = true)
            }
        }
    }

    // Persists the slider value once, after dragging stops or after keyboard/accessibility changes settle.
    private val commitSens = Runnable {
        val v = sensSlider.value.roundToInt()
        if (v != prefs().getInt("sens", 50)) {
            prefs().edit().putInt("sens", v).apply()
            syncService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)   // Material You on Android 12+, fallback palette otherwise
        super.onCreate(savedInstanceState)
        prefs().registerOnSharedPreferenceChangeListener(prefListener)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContentView(R.layout.activity_main)
        styleSystemBars()
        bindViews()

        val p = prefs()

        // Master switch ("enabled", default false)
        masterSwitch.isChecked = p.getBoolean("enabled", false)
        masterSwitch.setOnCheckedChangeListener { _, v ->
            if (prefs().getBoolean("enabled", false) != v) {
                prefs().edit().putBoolean("enabled", v).apply()
                syncService()
                renderHeader(animate = true)
            }
        }
        renderHeader(animate = false)

        // Wake method tiles (Shake & Lift)
        cardWakeShake.setOnClickListener {
            val next = !p.getBoolean("shake", true)
            prefs().edit().putBoolean("shake", next).apply()
            if (!next && expandedSensMode == "shake") {
                expandedSensMode = null
            }
            renderWakeTiles()
            updateSensPanels()
            syncService()
        }
        btnShakeAdjust.apply {
            contentDescription = getString(R.string.adjust_cd, getString(R.string.shake_title))
            setOnClickListener {
                if (!prefs().getBoolean("shake", true)) return@setOnClickListener
                expandedSensMode = if (expandedSensMode == "shake") null else "shake"
                updateSensPanels()
            }
        }
        btnShakeInfo.setOnClickListener {
            showInfo(R.string.shake_title, R.string.shake_info)
        }

        cardWakeLift.setOnClickListener {
            val next = !p.getBoolean("lift", true)
            prefs().edit().putBoolean("lift", next).apply()
            if (!next && expandedSensMode == "lift") {
                expandedSensMode = null
            }
            renderWakeTiles()
            updateSensPanels()
            syncService()
        }
        btnLiftAdjust.apply {
            contentDescription = getString(R.string.adjust_cd, getString(R.string.lift_title))
            setOnClickListener {
                if (!prefs().getBoolean("lift", true)) return@setOnClickListener
                expandedSensMode = if (expandedSensMode == "lift") null else "lift"
                updateSensPanels()
            }
        }
        btnLiftInfo.setOnClickListener {
            showInfo(R.string.lift_title, R.string.lift_info)
        }

        renderWakeTiles()

        // Shake sensitivity ("sens", 0..100, default 50). Left = harder shake (4.0 g), right = lighter (1.8 g).
        sensSlider.value = p.getInt("sens", 50).coerceIn(0, 100).toFloat()
        updateSensLabel(sensSlider.value.roundToInt())
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
        sensInfo.apply {
            contentDescription = getString(R.string.info_cd, getString(R.string.sens_title))
            setOnClickListener { showInfo(R.string.sens_title, R.string.sens_info) }
        }

        // Lift sensitivity buttonlets (Gentle vs Firm)
        btnLiftGentle.setOnClickListener {
            prefs().edit().putString("lift_sens", "gentle").apply()
            renderLiftSens()
            syncService()
        }
        btnLiftFirm.setOnClickListener {
            prefs().edit().putString("lift_sens", "firm").apply()
            renderLiftSens()
            syncService()
        }
        btnLiftSensInfo.setOnClickListener {
            showInfo(R.string.lift_sens_title, R.string.lift_sens_info)
        }
        renderLiftSens()

        updateSensPanels()

        // Reach / Flip and Pocket Protection rows
        bindRow(
            rowReachFlip,
            R.drawable.ic_mobile_hand,
            R.string.reach_flip_title,
            R.string.reach_flip_subtitle,
            "reach_flip",
            true,
            R.string.reach_flip_info
        )
        bindRow(
            rowPocket,
            R.drawable.ic_shield,
            R.string.pocket_protection_title,
            R.string.pocket_protection_subtitle,
            "pocket",
            true,
            R.string.pocket_protection_info
        )

        // Bottom action
        btnBattery.setOnClickListener { requestUnrestrictedBattery() }

        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        renderProximity(covered = false)
        refreshBatteryButton()
    }

    override fun onResume() {
        super.onResume()
        val p = prefs()
        val enabled = p.getBoolean("enabled", false)
        if (masterSwitch.isChecked != enabled) {
            masterSwitch.isChecked = enabled
            renderHeader(animate = false)
        }
        refreshBatteryButton()
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        prox?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.unregisterListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        prefs().unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    // ---- Live Proximity Sensor Events -----------------------------------------------------

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type == Sensor.TYPE_PROXIMITY) {
            val near = e.values[0] < minOf(5f, e.sensor.maximumRange)
            renderProximity(covered = near)
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // ---- Views Binding --------------------------------------------------------------------

    private fun bindViews() {
        headerCard = findViewById(R.id.headerCard)
        headerTitle = findViewById(R.id.headerTitle)
        masterSwitch = findViewById(R.id.masterSwitch)
        masterState = findViewById(R.id.masterState)

        cardWakeShake = findViewById(R.id.cardWakeShake)
        ivShakeIcon = findViewById(R.id.ivShakeIcon)
        ivShakeCheck = findViewById(R.id.ivShakeCheck)
        tvShakeTitle = findViewById(R.id.tvShakeTitle)
        tvShakeSubtitle = findViewById(R.id.tvShakeSubtitle)
        btnShakeAdjust = findViewById(R.id.btnShakeAdjust)
        btnShakeInfo = findViewById(R.id.btnShakeInfo)

        cardWakeLift = findViewById(R.id.cardWakeLift)
        ivLiftIcon = findViewById(R.id.ivLiftIcon)
        ivLiftCheck = findViewById(R.id.ivLiftCheck)
        tvLiftTitle = findViewById(R.id.tvLiftTitle)
        tvLiftSubtitle = findViewById(R.id.tvLiftSubtitle)
        btnLiftAdjust = findViewById(R.id.btnLiftAdjust)
        btnLiftInfo = findViewById(R.id.btnLiftInfo)

        layoutSensPlaceholder = findViewById(R.id.layoutSensPlaceholder)
        tvSensPlaceholder = findViewById(R.id.tvSensPlaceholder)
        setupSensPlaceholderText()

        cardShakeSens = findViewById(R.id.cardShakeSens)
        layoutShakeSensContent = findViewById(R.id.layoutShakeSensContent)
        sensTitleValue = findViewById(R.id.sensTitleValue)
        sensSlider = findViewById(R.id.sensSlider)
        sensInfo = findViewById(R.id.sensInfo)

        cardLiftSens = findViewById(R.id.cardLiftSens)
        layoutLiftSensContent = findViewById(R.id.layoutLiftSensContent)
        btnLiftSensInfo = findViewById(R.id.btnLiftSensInfo)
        btnLiftGentle = findViewById(R.id.btnLiftGentle)
        ivGentleCheck = findViewById(R.id.ivGentleCheck)
        tvGentleTitle = findViewById(R.id.tvGentleTitle)
        btnLiftFirm = findViewById(R.id.btnLiftFirm)
        ivFirmCheck = findViewById(R.id.ivFirmCheck)
        tvFirmTitle = findViewById(R.id.tvFirmTitle)

        cardReachFlip = findViewById(R.id.cardReachFlip)
        cardPocket = findViewById(R.id.cardPocket)
        rowReachFlip = findViewById(R.id.rowReachFlip)
        rowPocket = findViewById(R.id.rowPocket)

        proxIndicator = findViewById(R.id.proxIndicator)
        proxStatus = findViewById(R.id.proxStatus)

        btnBattery = findViewById(R.id.btnBattery)

        // Enforce Android 16 segmented group corner radii for Reach/Flip and Pocket rows:
        val radius = resources.getDimension(R.dimen.radius_card)
        cardReachFlip.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, radius)
            .setTopRightCorner(CornerFamily.ROUNDED, radius)
            .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
            .build()

        cardPocket.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
            .setTopRightCorner(CornerFamily.ROUNDED, 0f)
            .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
            .setBottomRightCorner(CornerFamily.ROUNDED, radius)
            .build()
    }

    private fun styleSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
        val rootView = findViewById<View>(R.id.rootView)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = bars.top,
                bottom = bars.bottom
            )
            insets
        }
    }

    private fun bindRow(
        row: View,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int?,
        key: String,
        def: Boolean,
        @StringRes info: Int
    ): MaterialSwitch {
        val titleText = getString(title)
        row.findViewById<ImageView>(R.id.rowIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.rowTitle).text = titleText
        val subView = row.findViewById<TextView>(R.id.rowSubtitle)
        if (subtitle != null) {
            subView.text = getString(subtitle)
            subView.visibility = View.VISIBLE
        } else {
            subView.visibility = View.GONE
        }
        val sw = row.findViewById<MaterialSwitch>(R.id.rowSwitch)
        sw.contentDescription = titleText
        sw.isChecked = prefs().getBoolean(key, def)
        sw.setOnCheckedChangeListener { _, v ->
            prefs().edit().putBoolean(key, v).apply()
            if (key == "reach_flip") {
                prefs().edit().putBoolean("pickup", v).apply() // legacy key compatibility
            }
            syncService()
        }
        // Only the toggle switch and info button are interactive; row labels are non-pressable.
        row.findViewById<ImageButton>(R.id.rowInfo).apply {
            contentDescription = getString(R.string.info_cd, titleText)
            setOnClickListener { showInfo(title, info) }
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

    private fun animationsEnabled() =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    // ---- Header ---------------------------------------------------------------------------

    private fun renderHeader(animate: Boolean) {
        val on = masterSwitch.isChecked
        val bg = MaterialColors.getColor(headerCard,
            if (on) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHigh)
        val fg = MaterialColors.getColor(headerCard,
            if (on) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface)
        headerTitle.setTextColor(fg)
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
    }

    // ---- Wake Method Tiles (Shake & Lift) -------------------------------------------------

    private fun renderWakeTiles() {
        val p = prefs()
        val shakeOn = p.getBoolean("shake", true)
        val liftOn = p.getBoolean("lift", true)

        styleTile(
            card = cardWakeShake,
            icon = ivShakeIcon,
            check = ivShakeCheck,
            info = btnShakeInfo,
            title = tvShakeTitle,
            subtitle = tvShakeSubtitle,
            selected = shakeOn
        )

        styleTile(
            card = cardWakeLift,
            icon = ivLiftIcon,
            check = ivLiftCheck,
            info = btnLiftInfo,
            title = tvLiftTitle,
            subtitle = tvLiftSubtitle,
            selected = liftOn
        )
    }

    private fun styleTile(
        card: MaterialCardView,
        icon: ImageView,
        check: ImageView,
        info: ImageButton,
        title: TextView,
        subtitle: TextView,
        selected: Boolean
    ) {
        val bg = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHigh
        )
        val fg = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface
        )
        val subFg = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        card.setCardBackgroundColor(bg)
        icon.setColorFilter(fg)
        title.setTextColor(fg)
        subtitle.setTextColor(subFg)
        check.visibility = if (selected) View.VISIBLE else View.GONE
        check.setColorFilter(fg)
        info.setColorFilter(fg)
    }

    // ---- On-demand Sensitivity Panels (Toggled via Adjust buttons) -----------------------

    private fun updateSensPanels() {
        val p = prefs()
        val shakeOn = p.getBoolean("shake", true)
        val liftOn = p.getBoolean("lift", true)

        val isShakeActive = expandedSensMode == "shake"
        val isLiftActive = expandedSensMode == "lift"
        val isPlaceholderActive = expandedSensMode == null

        // Sensitivity placeholder panel (shown when neither Adjust button is selected)
        layoutSensPlaceholder.visibility = if (isPlaceholderActive) View.VISIBLE else View.GONE

        // Shake sensitivity panel
        cardShakeSens.visibility = if (isShakeActive) View.VISIBLE else View.GONE
        if (isShakeActive) {
            layoutShakeSensContent.alpha = if (shakeOn) 1.0f else 0.45f
            sensSlider.isEnabled = shakeOn
        }

        // Lift sensitivity panel
        cardLiftSens.visibility = if (isLiftActive) View.VISIBLE else View.GONE
        if (isLiftActive) {
            layoutLiftSensContent.alpha = if (liftOn) 1.0f else 0.45f
            btnLiftGentle.isEnabled = liftOn
            btnLiftFirm.isEnabled = liftOn
        }

        // Style the Adjust buttons
        styleAdjustButton(btnShakeAdjust, cardWakeShake, tileActive = shakeOn, adjustActive = isShakeActive)
        styleAdjustButton(btnLiftAdjust, cardWakeLift, tileActive = liftOn, adjustActive = isLiftActive)
    }

    private fun setupSensPlaceholderText() {
        val fullText = getString(R.string.sens_placeholder_text)
        val placeholder = "{adjust}"
        val index = fullText.indexOf(placeholder)
        if (index != -1) {
            val ssb = SpannableStringBuilder(fullText)
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_tune)?.mutate()
            if (drawable != null) {
                val size = (tvSensPlaceholder.textSize * 1.15f).roundToInt()
                drawable.setBounds(0, 0, size, size)
                val tint = MaterialColors.getColor(tvSensPlaceholder, com.google.android.material.R.attr.colorOnSurfaceVariant)
                drawable.setTint(tint)
                val span = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
                ssb.setSpan(span, index, index + placeholder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tvSensPlaceholder.text = ssb
            }
        }
    }

    private fun styleAdjustButton(
        btn: ImageButton,
        tile: MaterialCardView,
        tileActive: Boolean,
        adjustActive: Boolean
    ) {
        if (!tileActive) {
            btn.isEnabled = false
            btn.isClickable = false
            btn.alpha = 0.38f
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            btn.setBackgroundResource(outValue.resourceId)
            val disabledColor = MaterialColors.getColor(tile, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btn.setColorFilter(disabledColor)
        } else {
            btn.isEnabled = true
            btn.isClickable = true
            btn.alpha = 1.0f
            if (adjustActive) {
                btn.setBackgroundResource(R.drawable.bg_row_icon)
                val iconTint = MaterialColors.getColor(btn, com.google.android.material.R.attr.colorOnSecondaryContainer)
                btn.setColorFilter(iconTint)
            } else {
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                btn.setBackgroundResource(outValue.resourceId)
                // When wake method is active and adjust button is not chosen:
                // same color as the inverted info button on the active tile (colorOnPrimaryContainer)
                val infoColor = MaterialColors.getColor(tile, com.google.android.material.R.attr.colorOnPrimaryContainer)
                btn.setColorFilter(infoColor)
            }
        }
    }

    // ---- Lift Sensitivity (Gentle vs Firm) -------------------------------------------------

    private fun renderLiftSens() {
        val p = prefs()
        val mode = p.getString("lift_sens", "gentle")
        val isGentle = mode != "firm"

        styleButtonlet(btnLiftGentle, ivGentleCheck, tvGentleTitle, isGentle)
        styleButtonlet(btnLiftFirm, ivFirmCheck, tvFirmTitle, !isGentle)
    }

    private fun styleButtonlet(
        card: MaterialCardView,
        check: ImageView,
        title: TextView,
        selected: Boolean
    ) {
        val bg = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHigh
        )
        val fg = MaterialColors.getColor(
            card,
            if (selected) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurface
        )
        card.setCardBackgroundColor(bg)
        title.setTextColor(fg)
        check.visibility = if (selected) View.VISIBLE else View.GONE
        check.setColorFilter(fg)
    }

    // ---- Sensitivity ----------------------------------------------------------------------

    private fun updateSensLabel(v: Int) {
        val g = 4.0f - 2.2f * (v / 100f)      // same mapping as WakeService
        sensTitleValue.text = getString(R.string.sens_title_value, g)
    }

    // ---- Proximity Sensor Live Status -----------------------------------------------------

    private fun renderProximity(covered: Boolean) {
        if (prox == null) {
            proxIndicator.setBackgroundResource(R.drawable.dot_unavailable)
            proxStatus.setText(R.string.status_unavailable)
            return
        }
        if (covered) {
            proxIndicator.setBackgroundResource(R.drawable.dot_on)
            proxStatus.setText(R.string.status_on)
        } else {
            proxIndicator.setBackgroundResource(R.drawable.dot_off)
            proxStatus.setText(R.string.status_off)
        }
    }

    // ---- Service control ------------------------------------------------------------------

    private fun syncService() {
        val i = Intent(this, WakeService::class.java)
        val p = prefs()
        val master = p.getBoolean("enabled", false)
        val shake = p.getBoolean("shake", true)
        val lift = p.getBoolean("lift", true)
        val reachFlip = p.getBoolean("reach_flip", p.getBoolean("pickup", true))
        val on = master && (shake || lift || reachFlip)
        if (on) startForegroundService(i) else stopService(i)
        refreshBatteryButton()
        try {
            TileService.requestListeningState(this, ComponentName(this, MaracasTileService::class.java))
        } catch (_: Exception) {}
    }

    // ---- Battery optimisation -------------------------------------------------------------

    private fun refreshBatteryButton() {
        val pm = getSystemService(PowerManager::class.java)
        val unrestricted = pm.isIgnoringBatteryOptimizations(packageName)
        btnBattery.isEnabled = !unrestricted
        btnBattery.setText(if (unrestricted) R.string.battery_granted else R.string.action_battery)
    }

    private fun requestUnrestrictedBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast(R.string.battery_already)
        } else {
            try {
                startActivity(Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                ))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e2: Exception) {
                    toast(R.string.battery_manual, long = true)
                }
            }
        }
    }

    private fun toast(@StringRes msg: Int, long: Boolean = false) {
        Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}
