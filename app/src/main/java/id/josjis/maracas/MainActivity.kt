package id.josjis.maracas

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
    private companion object {
        const val DISABLED_CONTENT_ALPHA = 0.38f
    }

    private val ui = Handler(Looper.getMainLooper())
    private var prox: Sensor? = null
    private var tracking = false                      // slider is being dragged

    // Header
    private lateinit var headerCard: MaterialCardView
    private lateinit var headerTitle: TextView
    private lateinit var masterSwitch: ImageButton   // circular power toggle
    private lateinit var btnMaterialYou: ImageButton // Material You toggle
    private lateinit var btnTheme: ImageButton       // theme toggle (sun/moon/contrast)
    private var headerColor = 0
    private var masterOn = false  // tracks ON/OFF state

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
    private lateinit var btnShakeActions: MaterialCardView
    private lateinit var ivShakeActionIcon: ImageView
    private lateinit var tvShakeActionLabel: TextView

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
    private lateinit var rowReachFlipSwitch: MaterialSwitch
    private lateinit var rowPocketSwitch: MaterialSwitch

    // Proximity card (the FAR/NEAR indicator itself intentionally keeps its original colors).
    private lateinit var cardProx: MaterialCardView
    private lateinit var tvProxTitle: TextView

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
            if (masterOn != enabled) {
                masterOn = enabled
                if (!masterOn) expandedSensMode = null
                renderAllUiState(animateHeader = true)
            }
        }
    }

    // Persists the slider value once, after dragging stops or after keyboard/accessibility changes settle.
    private val commitSens = Runnable {
        val action = prefs().getString("shake_action", "default") ?: "default"
        val isFlashlight = action == "flashlight"
        val maxSens = if (isFlashlight) 45 else 100
        val v = sensSlider.value.roundToInt().coerceIn(0, maxSens)
        val prefKey = if (isFlashlight) "sens_flashlight" else "sens_default"
        val p = prefs()
        if (v != p.getInt(prefKey, -1) || v != p.getInt("sens", -1)) {
            p.edit()
                .putInt(prefKey, v)
                .putInt("sens", v)
                .apply()
            syncService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Material You and theme BEFORE super/setContentView
        val p0 = getSharedPreferences("p", Context.MODE_PRIVATE)
        applyAppThemeAndDynamicColor(p0)
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

        // Hide Material You button on devices where DynamicColors isn't available.
        // DynamicColors.isDynamicColorAvailable() returns false on OEM skins that ship
        // Android 12+ without the Monet engine — e.g. Samsung One UI, MIUI/HyperOS,
        // ColorOS, Realme UI, etc. — so API-level alone is not a reliable check.
        if (!DynamicColors.isDynamicColorAvailable()) {
            btnMaterialYou.visibility = View.GONE
        }

        // Master power toggle
        masterOn = p.getBoolean("enabled", false)
        masterSwitch.setOnClickListener {
            it.animateTap()
            masterOn = !masterOn
            if (!masterOn) expandedSensMode = null
            prefs().edit().putBoolean("enabled", masterOn).apply()
            syncService()
            renderAllUiState(animateHeader = true)
        }
        // Material You toggle
        btnMaterialYou.setOnClickListener {
            it.animateTap()
            val p2 = prefs()
            val newVal = !p2.getBoolean("material_you", true)
            p2.edit().putBoolean("material_you", newVal).apply()
            recreate()  // DynamicColors must be applied/removed at Activity start
        }
        // Theme toggle: tap = light<->dark (icon updates immediately); long-press = system
        btnTheme.setOnClickListener {
            it.animateTap()
            val p2 = prefs()
            val current = p2.getString("theme", "system") ?: "system"
            val next = if (current == "dark") "light" else "dark"
            p2.edit().putString("theme", next).apply()
            // Update icon immediately — don't wait for recreate
            btnTheme.setImageResource(
                if (next == "dark") R.drawable.ic_theme_dark else R.drawable.ic_theme_light
            )
            AppCompatDelegate.setDefaultNightMode(
                if (next == "dark") AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        btnTheme.setOnLongClickListener {
            it.animateTap()
            prefs().edit().putString("theme", "system").apply()
            // Update icon immediately to contrast — don't wait for recreate
            btnTheme.setImageResource(R.drawable.ic_theme_contrast)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            Toast.makeText(this, R.string.theme_set_system, Toast.LENGTH_SHORT).show()
            true
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
            showInfo(btnShakeInfo, R.string.shake_title, R.string.shake_info)
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
            showInfo(btnLiftInfo, R.string.lift_title, R.string.lift_info)
        }

        renderWakeTiles()

        // Shake sensitivity ("sens", 0..100, default 50). Left = harder shake (4.0 g), right = lighter (1.8 g).
        val action = p.getString("shake_action", "default") ?: "default"
        val isFlashlight = action == "flashlight"
        val maxSens = if (isFlashlight) 45 else 100
        val initialSens = if (isFlashlight) {
            p.getInt("sens_flashlight", p.getInt("sens", 45).coerceIn(0, 45))
        } else {
            p.getInt("sens_default", p.getInt("sens", 50))
        }.coerceIn(0, maxSens)

        if (isFlashlight) {
            sensSlider.value = initialSens.toFloat()
            sensSlider.valueTo = 45f
        } else {
            sensSlider.value = initialSens.toFloat()
            sensSlider.valueTo = 100f
        }
        updateSensLabel(initialSens)
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
            setOnClickListener { showInfo(sensInfo, R.string.sens_title, R.string.sens_info) }
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
            showInfo(btnLiftSensInfo, R.string.lift_sens_title, R.string.lift_sens_info)
        }
        renderLiftSens()

        updateSensPanels()

        // Reach / Flip and Pocket Protection rows
        rowReachFlipSwitch = bindRow(
            rowReachFlip,
            R.drawable.ic_mobile_hand,
            R.string.reach_flip_title,
            R.string.reach_flip_subtitle,
            "reach_flip",
            true,
            R.string.reach_flip_info
        )
        rowPocketSwitch = bindRow(
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
        renderAllUiState(animateHeader = false)
    }

    override fun onResume() {
        super.onResume()
        val p = prefs()
        val enabled = p.getBoolean("enabled", false)
        if (masterOn != enabled) {
            masterOn = enabled
            renderAllUiState(animateHeader = false)
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
        btnMaterialYou = findViewById(R.id.btnMaterialYou)
        btnTheme       = findViewById(R.id.btnTheme)

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
        btnShakeActions = findViewById(R.id.btnShakeActions)
        ivShakeActionIcon = findViewById(R.id.ivShakeActionIcon)
        tvShakeActionLabel = findViewById(R.id.tvShakeActionLabel)

        btnShakeActions.setOnClickListener {
            showShakeActionsDialog(btnShakeActions)
        }
        updateShakeActionView()

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

        cardProx = findViewById(R.id.cardProx)
        tvProxTitle = findViewById(R.id.proxTitle)
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
            renderProtectionRows()
            syncService()
        }
        // Only the toggle switch and info button are interactive; row labels are non-pressable.
        row.findViewById<ImageButton>(R.id.rowInfo).apply {
            contentDescription = getString(R.string.info_cd, titleText)
            setOnClickListener { showInfo(it, title, info) }
        }
        return sw
    }

    private fun showInfo(anchor: View, @StringRes title: Int, @StringRes message: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.got_it, null)
            .create()
        showExpandingDialog(anchor, dialog)
    }

    private fun showExpandingDialog(anchor: View, dialog: AlertDialog) {
        dialog.show()
        val window = dialog.window ?: return
        window.setWindowAnimations(R.style.Animation_Maracas_Dialog)
        val decorView = window.decorView
        decorView.alpha = 0f

        decorView.post {
            if (!animationsEnabled()) {
                decorView.alpha = 1f
                return@post
            }
            val anchorLoc = IntArray(2)
            anchor.getLocationOnScreen(anchorLoc)
            val anchorCenterX = anchorLoc[0] + anchor.width / 2f
            val anchorCenterY = anchorLoc[1] + anchor.height / 2f

            val dialogLoc = IntArray(2)
            decorView.getLocationOnScreen(dialogLoc)

            val pivotX = anchorCenterX - dialogLoc[0]
            val pivotY = anchorCenterY - dialogLoc[1]

            decorView.pivotX = pivotX
            decorView.pivotY = pivotY

            val startScaleX = (anchor.width.toFloat() / decorView.width.coerceAtLeast(1)).coerceIn(0.08f, 0.4f)
            val startScaleY = (anchor.height.toFloat() / decorView.height.coerceAtLeast(1)).coerceIn(0.08f, 0.4f)

            decorView.scaleX = startScaleX
            decorView.scaleY = startScaleY
            decorView.alpha = 0f

            decorView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(280L)
                .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
                .start()
        }
    }

    private fun animationsEnabled() =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    // ---- Monet tonal palette ---------------------------------------------------------------

    /**
     * Android exposes the real wallpaper-derived Monet palettes as system color resources.
     * Their suffixes are palette indices, not Android view alpha values:
     * 0, 10, 50, 100 ... 1000 map to tones 100, 99, 95, 90 ... 0.
     * Keeping this lookup centralized lets the UI use actual Monet samples instead of
     * applying tints to semantic role colors.
     */
    private fun shouldUseMonetPalette(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            prefs().getBoolean("material_you", true) &&
            DynamicColors.isDynamicColorAvailable()

    private fun monetResourceIndexForTone(tone: Int): Int? = when (tone) {
        0 -> 1000
        10 -> 900
        20 -> 800
        30 -> 700
        40 -> 600
        50 -> 500
        60 -> 400
        70 -> 300
        80 -> 200
        90 -> 100
        95 -> 50
        99 -> 10
        100 -> 0
        else -> null
    }

    /** Read a real Android Monet palette sample; intermediate tones are not framework resources. */
    private fun monetPaletteSample(family: String, index: Int): Int? {
        val resourceName = "system_${family}_$index"
        val resourceId = resources.getIdentifier(resourceName, "color", "android")
        if (resourceId == 0) return null
        return try {
            resources.getColor(resourceId, theme)
        } catch (_: Exception) {
            null
        }
    }

    /** Interpolate between adjacent real Monet samples for tones Android does not expose. */
    private fun interpolatedMonetTone(family: String, lowerIndex: Int, upperIndex: Int): Int? {
        val lower = monetPaletteSample(family, lowerIndex) ?: return null
        val upper = monetPaletteSample(family, upperIndex) ?: return null
        return ColorUtils.blendARGB(lower, upper, 0.5f)
    }

    /** Resolve a Monet palette tone, falling back to its bundled Material 3 sample or role. */
    private fun monetTone(
        family: String,
        tone: Int,
        anchor: View,
        fallbackRole: Int
    ): Int {
        if (shouldUseMonetPalette()) {
            // Android exposes a fixed set of Monet samples (0, 10, 20, 30, ...),
            // not arbitrary 25/75 resources. Derive those two intermediate tones
            // from their neighboring live wallpaper-derived samples.
            val dynamicColor = when (tone) {
                25 -> interpolatedMonetTone(family, 800, 700) // midpoint of tones 20 and 30
                75 -> interpolatedMonetTone(family, 300, 200) // midpoint of tones 70 and 80
                else -> monetResourceIndexForTone(tone)?.let { monetPaletteSample(family, it) }
            }
            if (dynamicColor != null) return dynamicColor
        }

        // Keep tone relationships coherent when dynamic color is switched off or
        // Monet is unavailable. These are baseline Material 3 samples, not tinted
        // light/dark role colors (which would invert the intended disabled treatment).
        val fallbackSample = when ("$family:$tone") {
            "accent1:20" -> R.color.maracas_primary_tone20
            "accent1:25" -> R.color.maracas_primary_tone25
            "accent1:30" -> R.color.maracas_primary_tone30
            "accent1:75" -> R.color.maracas_primary_tone75
            "accent1:90" -> R.color.maracas_primary_tone90
            "accent1:95" -> R.color.maracas_primary_tone95
            "accent2:30" -> R.color.maracas_secondary_tone30
            "accent2:95" -> R.color.maracas_secondary_tone95
            "accent3:20" -> R.color.maracas_tertiary_tone20
            "accent3:30" -> R.color.maracas_tertiary_tone30
            "accent3:90" -> R.color.maracas_tertiary_tone90
            "accent3:95" -> R.color.maracas_tertiary_tone95
            else -> 0
        }
        return if (fallbackSample != 0) ContextCompat.getColor(this, fallbackSample)
        else MaterialColors.getColor(anchor, fallbackRole)
    }

    private fun isNightTheme(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun primaryButtonBackground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)

    private fun primaryButtonForeground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary)

    private fun neutralSurface(view: View): Int =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainer)

    private fun neutralSurfaceHigh(view: View): Int =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerHigh)

    private fun neutralOnSurface(view: View): Int =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface)

    private fun neutralOnSurfaceVariant(view: View): Int =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant)

    /** Material's standard disabled foreground treatment for text and controls. */
    private fun disabledForeground(view: View): Int =
        ColorUtils.setAlphaComponent(neutralOnSurface(view), (DISABLED_CONTENT_ALPHA * 255).roundToInt())

    /** Keep enabled subtitles on the same accent hue, but one visual step quieter than titles. */
    private fun mutedSubtitleForeground(foreground: Int, background: Int): Int =
        ColorUtils.blendARGB(foreground, background, 0.20f)

    /**
     * One icon-state pipeline for the whole screen.
     *
     * The semantic tint describes what the icon normally represents. Disabled state
     * never swaps that tint for a hard-coded/muted color; it only reduces view alpha.
     * Pass null for enabled when a parent content group already owns the disabled alpha.
     */
    private fun styleIcon(
        icon: ImageView,
        semanticTint: Int,
        enabled: Boolean? = null
    ) {
        icon.clearColorFilter()
        icon.imageTintList = ColorStateList.valueOf(semanticTint)
        enabled?.let { icon.alpha = if (it) 1f else DISABLED_CONTENT_ALPHA }
    }


    /** Dim interactive content recursively while leaving MaterialCardView surfaces untouched. */
    private fun setDescendantContentAlpha(container: View, alpha: Float) {
        if (container !is ViewGroup) return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is ViewGroup) {
                // Nested cards keep their neutral surface fill; only their contents are muted.
                child.alpha = 1f
                setDescendantContentAlpha(child, alpha)
            } else {
                child.alpha = alpha
            }
        }
    }

    private fun dottedOutline(radius: Float, color: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.TRANSPARENT)
            setStroke(
                (1.5f * resources.displayMetrics.density).roundToInt(),
                color,
                6f * resources.displayMetrics.density,
                6f * resources.displayMetrics.density
            )
        }

    // The Shake/Lift method tiles use the standard active surface tones.
    // Keep this helper unchanged by the expanded sensitivity-card styling.
    private fun wakeEnabledBackground(view: View): Int =
        monetTone(
            "accent1", if (isNightTheme()) 30 else 85, view,
            com.google.android.material.R.attr.colorPrimaryContainer
        )

    // Expanded sensitivity panels sit one half-step away from the method tiles:
    // lighter in dark mode (25 vs 20), darker in light mode (75 vs 80).
    // monetTone resolves these from live Monet samples, with the matching bundled
    // baseline samples used only when dynamic color is unavailable.
    private fun sensitivityPanelBackground(view: View): Int =
        monetTone(
            "accent1", if (isNightTheme()) 20 else 90, view,
            com.google.android.material.R.attr.colorPrimaryContainer
        )

    private fun wakeEnabledForeground(view: View): Int =
        monetTone(
            "accent1", if (isNightTheme()) 95 else 10, view,
            com.google.android.material.R.attr.colorOnPrimaryContainer
        )

    private fun secondaryButtonBackground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondary)

    private fun secondaryButtonForeground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSecondary)

    private fun tertiaryButtonBackground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorTertiary)

    private fun tertiaryButtonForeground(view: View) =
        MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnTertiary)
    private fun renderAllUiState(animateHeader: Boolean) {
        renderHeader(animate = animateHeader)
        renderWakeTiles()
        updateSensPanels()
        renderLiftSens()
        renderProtectionRows()
        renderProximityCard()
        refreshBatteryButton()
    }

    // ---- Header ---------------------------------------------------------------------------

    private fun renderHeader(animate: Boolean) {
        val on = masterOn
        val bg = if (on) {
            MaterialColors.getColor(headerCard, com.google.android.material.R.attr.colorPrimaryContainer)
        } else {
            neutralSurface(headerCard)
        }
        val fg = if (on) {
            MaterialColors.getColor(headerCard, com.google.android.material.R.attr.colorOnPrimaryContainer)
        } else {
            neutralOnSurface(headerCard)
        }
        headerTitle.setTextColor(fg)
        headerCard.foreground = if (on) null else dottedOutline(
            resources.getDimension(R.dimen.radius_header),
            MaterialColors.getColor(headerCard, com.google.android.material.R.attr.colorOutlineVariant)
        )

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

        // While off, controls retain a muted outlined appearance; all remain actionable.
        styleHeaderButton(masterSwitch, "accent1", actionable = true, visuallyMuted = !on)
        styleHeaderButton(btnMaterialYou, "accent2", actionable = true, visuallyMuted = !on)
        styleHeaderButton(btnTheme, "accent3", actionable = true, visuallyMuted = !on)

        val themeKey = prefs().getString("theme", "system") ?: "system"
        btnTheme.setImageResource(when (themeKey) {
            "light" -> R.drawable.ic_theme_light
            "dark"  -> R.drawable.ic_theme_dark
            else    -> R.drawable.ic_theme_contrast
        })
    }
    private fun styleHeaderButton(
        btn: ImageButton,
        family: String,
        actionable: Boolean,
        visuallyMuted: Boolean = !actionable
    ) {
        val bg: Int
        val fg: Int
        if (visuallyMuted) {
            // No accent fill in the disabled state: transparent surface, neutral outline,
            // and a standard disabled foreground. The master remains tappable but matches.
            bg = Color.TRANSPARENT
            fg = neutralOnSurfaceVariant(btn)
        } else {
            when (family) {
                "accent2" -> {
                    bg = secondaryButtonBackground(btn)
                    fg = secondaryButtonForeground(btn)
                }
                "accent3" -> {
                    bg = tertiaryButtonBackground(btn)
                    fg = tertiaryButtonForeground(btn)
                }
                else -> {
                    bg = primaryButtonBackground(btn)
                    fg = primaryButtonForeground(btn)
                }
            }
        }
        val density = resources.displayMetrics.density
        btn.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(bg)
            if (visuallyMuted) {
                setStroke(
                    (1.5f * density).roundToInt(),
                    MaterialColors.getColor(btn, com.google.android.material.R.attr.colorOutline)
                )
            }
        }
        styleIcon(btn, fg, enabled = true)
        btn.isEnabled = actionable
        btn.isClickable = actionable
        btn.alpha = 1f
    }
    private fun applyAppThemeAndDynamicColor(p: SharedPreferences) {
        // Material You (dynamic color) – only meaningful on Android 12+
        val materialYou = p.getBoolean("material_you", true)
        if (materialYou && Build.VERSION.SDK_INT >= 31) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
        // Night mode
        val themeMode = when (p.getString("theme", "system")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
            else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun showAppearanceDialog(anchor: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_appearance, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        showExpandingDialog(anchor, dialog)

        val p = prefs()
        // Track whether Material You was toggled (requires recreate on Done)
        val initialMaterialYou = p.getBoolean("material_you", true)
        var pendingMaterialYouChanged = false

        // ── Material You switch ─────────────────────────────────────────────
        val switchMY = dialogView.findViewById<MaterialSwitch>(R.id.switchMaterialYou)
        switchMY.isChecked = initialMaterialYou
        switchMY.setOnCheckedChangeListener { _, checked ->
            p.edit().putBoolean("material_you", checked).apply()
            pendingMaterialYouChanged = checked != initialMaterialYou
        }

        // ── Theme chips ─────────────────────────────────────────────────────
        val currentTheme = p.getString("theme", "system") ?: "system"

        data class ThemeChip(
            val key: String,
            val card: MaterialCardView,
            val icon: ImageView,
            val check: ImageView,
            val label: TextView
        )

        val chips = listOf(
            ThemeChip("system",
                dialogView.findViewById(R.id.chipThemeSystem),
                dialogView.findViewById(R.id.ivThemeIconSystem),
                dialogView.findViewById(R.id.ivCheckThemeSystem),
                dialogView.findViewById(R.id.tvThemeSystem)),
            ThemeChip("light",
                dialogView.findViewById(R.id.chipThemeLight),
                dialogView.findViewById(R.id.ivThemeIconLight),
                dialogView.findViewById(R.id.ivCheckThemeLight),
                dialogView.findViewById(R.id.tvThemeLight)),
            ThemeChip("dark",
                dialogView.findViewById(R.id.chipThemeDark),
                dialogView.findViewById(R.id.ivThemeIconDark),
                dialogView.findViewById(R.id.ivCheckThemeDark),
                dialogView.findViewById(R.id.tvThemeDark))
        )

        fun renderChips(sel: String) {
            chips.forEach { chip ->
                val isSel = chip.key == sel
                val bg = MaterialColors.getColor(
                    chip.card,
                    if (isSel) com.google.android.material.R.attr.colorPrimaryContainer
                    else com.google.android.material.R.attr.colorSurfaceContainerHigh)
                val fg = MaterialColors.getColor(
                    chip.card,
                    if (isSel) com.google.android.material.R.attr.colorOnPrimaryContainer
                    else com.google.android.material.R.attr.colorOnSurface)
                chip.card.setCardBackgroundColor(bg)
                chip.card.strokeWidth = if (isSel) 0 else
                    (resources.displayMetrics.density).toInt()
                chip.card.strokeColor = MaterialColors.getColor(
                    chip.card, com.google.android.material.R.attr.colorOutlineVariant)
                styleIcon(chip.icon, fg, enabled = true)
                chip.label.setTextColor(fg)
                chip.check.visibility = if (isSel) View.VISIBLE else View.INVISIBLE
                styleIcon(chip.check, fg, enabled = true)
            }
        }

        renderChips(currentTheme)

        // Track selection locally; prefs written immediately for persistence
        var selectedTheme = currentTheme
        chips.forEach { chip ->
            chip.card.setOnClickListener {
                selectedTheme = chip.key
                p.edit().putString("theme", chip.key).apply()
                renderChips(chip.key)
                // ⚠️ Do NOT call AppCompatDelegate.setDefaultNightMode() here —
                // it triggers activity recreation which dismisses the dialog.
                // Mode is applied safely in Done below.
            }
        }

        // ── Done — TextButton right-aligned, same appearance as system "Got it" positive button ──
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAppearanceDone)
            .setOnClickListener {
                dialog.dismiss()
                // Apply theme mode now (dialog gone, no recreation risk)
                AppCompatDelegate.setDefaultNightMode(when (selectedTheme) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
                    else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                })
                // Material You toggle needs full recreate to (un)apply DynamicColors
                if (pendingMaterialYouChanged) recreate()
            }
    }

    // ---- Wake Method Tiles (Shake & Lift) -------------------------------------------------

    private fun renderWakeTiles() {
        val p = prefs()
        styleTile(
            card = cardWakeShake,
            icon = ivShakeIcon,
            check = ivShakeCheck,
            info = btnShakeInfo,
            title = tvShakeTitle,
            subtitle = tvShakeSubtitle,
            selected = p.getBoolean("shake", true)
        )
        styleTile(
            card = cardWakeLift,
            icon = ivLiftIcon,
            check = ivLiftCheck,
            info = btnLiftInfo,
            title = tvLiftTitle,
            subtitle = tvLiftSubtitle,
            selected = p.getBoolean("lift", true)
        )
    }

    private fun roundedRect(color: Int, radiusDp: Float): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(color)
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
        val active = masterOn && selected
        val bg = if (active) wakeEnabledBackground(card) else neutralSurface(card)
        val fg = when {
            active -> wakeEnabledForeground(card)
            masterOn -> neutralOnSurface(card)
            else -> disabledForeground(card)
        }
        val iconBg = if (active) primaryButtonBackground(card) else neutralSurfaceHigh(card)
        // Keep the same semantic foreground in both master states. Disabled is alpha-only.
        val iconFg = if (active) primaryButtonForeground(card) else neutralOnSurfaceVariant(card)

        card.setCardBackgroundColor(bg)
        card.isEnabled = masterOn
        card.isClickable = masterOn
        card.isFocusable = masterOn
        icon.background = roundedRect(iconBg, 8f)
        val iconPadding = (4f * resources.displayMetrics.density).roundToInt()
        icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        styleIcon(icon, iconFg, enabled = masterOn)
        title.setTextColor(fg)
        subtitle.setTextColor(
            if (active) mutedSubtitleForeground(fg, bg)
            else if (masterOn) neutralOnSurfaceVariant(card)
            else disabledForeground(card)
        )
        check.visibility = if (active) View.VISIBLE else View.GONE
        styleIcon(check, fg, enabled = masterOn)
        info.isEnabled = masterOn
        info.isClickable = masterOn
        styleIcon(info, fg, enabled = masterOn)
    }
    private fun updateSensPanels() {
        val p = prefs()
        val shakeOn = p.getBoolean("shake", true)
        val liftOn = p.getBoolean("lift", true)
        val shakeUsable = masterOn && shakeOn
        val liftUsable = masterOn && liftOn

        val isShakeActive = expandedSensMode == "shake"
        val isLiftActive = expandedSensMode == "lift"
        val isPlaceholderActive = expandedSensMode == null

        layoutSensPlaceholder.visibility = if (isPlaceholderActive) View.VISIBLE else View.GONE
        layoutSensPlaceholder.background = if (masterOn) {
            // The dotted idle outline is shown only while Maracas is enabled.
            dottedOutline(
                resources.getDimension(R.dimen.radius_card),
                MaterialColors.getColor(layoutSensPlaceholder, com.google.android.material.R.attr.colorOutlineVariant)
            ).apply { setColor(neutralSurface(layoutSensPlaceholder)) }
        } else {
            // Disabled placeholder is a plain surface, with no dotted border.
            roundedRect(neutralSurface(layoutSensPlaceholder), resources.getDimension(R.dimen.radius_card) /
                resources.displayMetrics.density)
        }
        tvSensPlaceholder.setTextColor(neutralOnSurfaceVariant(tvSensPlaceholder))
        tvSensPlaceholder.alpha = if (masterOn) 1f else DISABLED_CONTENT_ALPHA

        cardShakeSens.visibility = if (isShakeActive) View.VISIBLE else View.GONE
        if (isShakeActive) {
            cardShakeSens.setCardBackgroundColor(
                if (masterOn) sensitivityPanelBackground(cardShakeSens) else neutralSurface(cardShakeSens)
            )
            layoutShakeSensContent.alpha = 1f
            setDescendantContentAlpha(layoutShakeSensContent, if (masterOn) 1f else DISABLED_CONTENT_ALPHA)
            sensSlider.isEnabled = shakeUsable
            btnShakeActions.isEnabled = shakeUsable
            btnShakeActions.isClickable = shakeUsable
            sensInfo.isEnabled = shakeUsable
            sensInfo.isClickable = shakeUsable
            sensTitleValue.setTextColor(
                if (masterOn) wakeEnabledForeground(cardShakeSens) else neutralOnSurface(cardShakeSens)
            )
            styleIcon(
                sensInfo,
                if (masterOn) wakeEnabledForeground(cardShakeSens) else neutralOnSurfaceVariant(cardShakeSens)
            )
            styleSensSlider(shakeUsable)
            updateShakeActionView()
        }

        cardLiftSens.visibility = if (isLiftActive) View.VISIBLE else View.GONE
        if (isLiftActive) {
            cardLiftSens.setCardBackgroundColor(
                if (masterOn) sensitivityPanelBackground(cardLiftSens) else neutralSurface(cardLiftSens)
            )
            layoutLiftSensContent.alpha = 1f
            setDescendantContentAlpha(layoutLiftSensContent, if (masterOn) 1f else DISABLED_CONTENT_ALPHA)
            btnLiftGentle.isEnabled = liftUsable
            btnLiftGentle.isClickable = liftUsable
            btnLiftFirm.isEnabled = liftUsable
            btnLiftFirm.isClickable = liftUsable
            btnLiftSensInfo.isEnabled = liftUsable
            btnLiftSensInfo.isClickable = liftUsable
            styleIcon(
                btnLiftSensInfo,
                if (masterOn) wakeEnabledForeground(cardLiftSens) else neutralOnSurfaceVariant(cardLiftSens)
            )
            cardLiftSens.findViewById<TextView>(R.id.liftSensTitle)?.setTextColor(
                if (masterOn) wakeEnabledForeground(cardLiftSens) else neutralOnSurface(cardLiftSens)
            )
        }

        styleAdjustButton(btnShakeAdjust, cardWakeShake, tileActive = shakeOn, adjustActive = isShakeActive)
        styleAdjustButton(btnLiftAdjust, cardWakeLift, tileActive = liftOn, adjustActive = isLiftActive)
    }
    private fun styleSensSlider(active: Boolean) {
        val neutralTrack = neutralSurfaceHigh(sensSlider)
        val trackActive = if (active) tertiaryButtonBackground(sensSlider) else neutralTrack
        val trackInactive = neutralSurface(sensSlider)
        val thumb = if (active) tertiaryButtonBackground(sensSlider) else neutralOnSurfaceVariant(sensSlider)
        sensSlider.trackActiveTintList = ColorStateList.valueOf(trackActive)
        sensSlider.trackInactiveTintList = ColorStateList.valueOf(trackInactive)
        sensSlider.thumbTintList = ColorStateList.valueOf(thumb)
        sensSlider.haloTintList = ColorStateList.valueOf(
            if (active) tertiaryButtonBackground(sensSlider) else neutralTrack
        )
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
                val tint = neutralOnSurfaceVariant(tvSensPlaceholder)
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
        val usable = masterOn && tileActive
        btn.isEnabled = usable
        btn.isClickable = usable

        when {
            // Selection is independent of master power: keep the selected inspector
            // marker Tertiary while the actual inspector controls remain disabled.
            adjustActive -> {
                btn.background = roundedRect(tertiaryButtonBackground(tile), 8f)
                styleIcon(btn, tertiaryButtonForeground(tile), enabled = true)
            }
            usable -> {
                btn.background = roundedRect(wakeEnabledBackground(tile), 8f)
                styleIcon(btn, wakeEnabledForeground(tile), enabled = true)
            }
            else -> {
                btn.background = null
                // No disabled-specific color. Retain the ordinary semantic tint and
                // let this view's single disabled-alpha rule communicate inactivity.
                styleIcon(btn, neutralOnSurfaceVariant(tile), enabled = false)
            }
        }
    }
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
        val active = masterOn && selected
        val panelBg = if (masterOn) sensitivityPanelBackground(card) else neutralSurface(card)
        val bg = if (active) tertiaryButtonBackground(card) else panelBg
        val fg = when {
            active -> tertiaryButtonForeground(card)
            masterOn -> wakeEnabledForeground(card)
            else -> neutralOnSurface(card) // layoutLiftSensContent applies the disabled dimming.
        }
        card.setCardBackgroundColor(bg)
        card.radius = resources.displayMetrics.density * 16f
        card.isEnabled = masterOn
        card.isClickable = masterOn
        if (!active && masterOn) {
            card.strokeWidth = (resources.displayMetrics.density * 1f).roundToInt()
            val borderColor = primaryButtonBackground(card)
            card.strokeColor = ColorUtils.setAlphaComponent(
                borderColor,
                (Color.alpha(borderColor) * 0.85f).roundToInt()
            )
        } else {
            card.strokeWidth = 0
        }
        title.setTextColor(fg)
        check.visibility = if (active) View.VISIBLE else View.GONE
        styleIcon(check, fg)
    }
    private fun getActionIcon(action: String): Int = when (action) {
        "flashlight" -> R.drawable.ic_flashlight
        else -> R.drawable.ic_power
    }

    private fun updateShakeActionView() {
        val action = prefs().getString("shake_action", "default") ?: "default"
        ivShakeActionIcon.setImageResource(getActionIcon(action))
        styleActionsButton(btnShakeActions, ivShakeActionIcon, tvShakeActionLabel)
    }

    private fun styleActionsButton(card: MaterialCardView, icon: ImageView, label: TextView) {
        val active = masterOn && prefs().getBoolean("shake", true)
        val bg = if (active) tertiaryButtonBackground(card) else neutralSurface(card)
        val fg = when {
            active -> tertiaryButtonForeground(card)
            masterOn -> disabledForeground(card)
            else -> neutralOnSurfaceVariant(card) // the parent sensitivity content applies the dimming.
        }
        card.setCardBackgroundColor(bg)
        card.radius = resources.displayMetrics.density * 16f
        card.strokeWidth = 0
        card.isEnabled = active
        card.isClickable = active
        styleIcon(icon, fg)
        // Same semantic text role as the Low/High captions below the slider.
        label.setTextColor(if (masterOn && !active) disabledForeground(label) else neutralOnSurfaceVariant(label))
        label.alpha = 1f
    }
    private fun renderProtectionRows() {
        if (!::rowReachFlipSwitch.isInitialized || !::rowPocketSwitch.isInitialized) return
        styleProtectionRow(cardReachFlip, rowReachFlip, rowReachFlipSwitch)
        styleProtectionRow(cardPocket, rowPocket, rowPocketSwitch)
    }

    private fun styleProtectionRow(
        card: MaterialCardView,
        row: View,
        toggle: MaterialSwitch
    ) {
        val selected = masterOn && toggle.isChecked
        val rowBg = if (selected) {
            monetTone(
                "accent2", if (isNightTheme()) 20 else 85, card,
                com.google.android.material.R.attr.colorSecondaryContainer
            )
        } else {
            neutralSurface(card)
        }
        val rowFg = when {
            selected -> monetTone(
                "accent2", if (isNightTheme()) 95 else 10, card,
                com.google.android.material.R.attr.colorOnSecondaryContainer
            )
            masterOn -> neutralOnSurface(card)
            else -> disabledForeground(card)
        }
        card.setCardBackgroundColor(rowBg)
        row.alpha = 1f
        row.findViewById<TextView>(R.id.rowTitle).setTextColor(rowFg)
        row.findViewById<TextView>(R.id.rowSubtitle).setTextColor(
            when {
                selected -> mutedSubtitleForeground(rowFg, rowBg)
                masterOn -> neutralOnSurfaceVariant(card)
                else -> rowFg
            }
        )

        val icon = row.findViewById<ImageView>(R.id.rowIcon)
        if (selected) {
            icon.background = roundedRect(secondaryButtonBackground(icon), 12f)
            styleIcon(icon, secondaryButtonForeground(icon), enabled = true)
        } else {
            icon.background = roundedRect(neutralSurfaceHigh(icon), 12f)
            styleIcon(icon, neutralOnSurfaceVariant(icon), enabled = masterOn)
        }

        val info = row.findViewById<ImageButton>(R.id.rowInfo)
        styleIcon(info, neutralOnSurfaceVariant(info), enabled = masterOn)
        info.isEnabled = masterOn
        info.isClickable = masterOn

        toggle.isEnabled = masterOn
        toggle.isClickable = masterOn
        val disabledThumb = disabledForeground(toggle)
        val disabledTrack = MaterialColors.getColor(toggle, com.google.android.material.R.attr.colorSurfaceVariant)
        val tertiary = tertiaryButtonBackground(toggle)
        val onTertiary = tertiaryButtonForeground(toggle)
        val uncheckedThumb = MaterialColors.getColor(toggle, com.google.android.material.R.attr.colorOutline)
        val uncheckedTrack = MaterialColors.getColor(toggle, com.google.android.material.R.attr.colorSurfaceVariant)
        toggle.thumbTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(disabledThumb, onTertiary, uncheckedThumb)
        )
        toggle.trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(disabledTrack, tertiary, uncheckedTrack)
        )
    }
    private fun renderProximityCard() {
        // The sensor card is informational, not an actionable control. Keep its
        // text readable while using the same neutral surface as the inactive rows.
        val bg = if (masterOn) {
            monetTone(
                "accent3", if (isNightTheme()) 20 else 85, cardProx,
                com.google.android.material.R.attr.colorTertiaryContainer
            )
        } else {
            neutralSurface(cardProx)
        }
        val fg = if (masterOn) {
            monetTone(
                "accent3", if (isNightTheme()) 80 else 20, cardProx,
                com.google.android.material.R.attr.colorOnTertiaryContainer
            )
        } else {
            neutralOnSurface(cardProx)
        }
        cardProx.setCardBackgroundColor(bg)
        tvProxTitle.setTextColor(fg)
        proxStatus.setTextColor(fg)
        // proxIndicator is deliberately untouched: its FAR/NEAR dot colors stay as designed.
    }
    private fun showShakeActionsDialog(anchor: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_shake_actions, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        showExpandingDialog(anchor, dialog)

        val p = prefs()
        val current = p.getString("shake_action", "default") ?: "default"

        data class ActionItem(
            val key: String,
            val card: MaterialCardView,
            val icon: ImageView,
            val label: TextView,
            val check: ImageView
        )

        // Migrate any removed actions saved from a previous install back to default
        val sanitized = if (current in listOf("default", "flashlight")) current else {
            prefs().edit().putString("shake_action", "default").apply()
            "default"
        }

        val items = listOf(
            ActionItem("default", dialogView.findViewById(R.id.btnActionDefault), dialogView.findViewById(R.id.ivIconDefault), dialogView.findViewById(R.id.tvLabelDefault), dialogView.findViewById(R.id.ivCheckDefault)),
            ActionItem("flashlight", dialogView.findViewById(R.id.btnActionFlashlight), dialogView.findViewById(R.id.ivIconFlashlight), dialogView.findViewById(R.id.tvLabelFlashlight), dialogView.findViewById(R.id.ivCheckFlashlight))
        )

        fun renderItems(selectedKey: String) {
            for (item in items) {
                val isSel = item.key == selectedKey
                val bg = MaterialColors.getColor(
                    item.card,
                    if (isSel) com.google.android.material.R.attr.colorPrimaryContainer
                    else com.google.android.material.R.attr.colorSurfaceContainerHigh
                )
                val fg = MaterialColors.getColor(
                    item.card,
                    if (isSel) com.google.android.material.R.attr.colorOnPrimaryContainer
                    else com.google.android.material.R.attr.colorOnSurface
                )
                item.card.setCardBackgroundColor(bg)
                item.card.radius = resources.displayMetrics.density * 25f
                if (isSel) {
                    item.card.strokeWidth = 0
                } else {
                    item.card.strokeWidth = (resources.displayMetrics.density * 1f).toInt()
                    item.card.strokeColor = MaterialColors.getColor(item.card, com.google.android.material.R.attr.colorOutlineVariant)
                }
                styleIcon(item.icon, fg, enabled = true)
                item.label.setTextColor(fg)
                item.check.visibility = if (isSel) View.VISIBLE else View.GONE
                styleIcon(item.check, fg, enabled = true)
            }
        }

        renderItems(sanitized)

        for (item in items) {
            item.card.setOnClickListener {
                p.edit().putString("shake_action", item.key).apply()
                updateShakeActionView()
                applyShakeActionSensitivityCap()
                syncService()
                dialog.dismiss()
            }
        }
    }

    private fun applyShakeActionSensitivityCap() {
        val action = prefs().getString("shake_action", "default") ?: "default"
        val isFlashlight = action == "flashlight"
        val p = prefs()
        if (isFlashlight) {
            val savedSens = p.getInt("sens_flashlight", p.getInt("sens", 45).coerceIn(0, 45)).coerceIn(0, 45)
            sensSlider.value = savedSens.toFloat()
            sensSlider.valueTo = 45f
            p.edit().putInt("sens", savedSens).putInt("sens_flashlight", savedSens).apply()
            updateSensLabel(savedSens)
        } else {
            val savedSens = p.getInt("sens_default", p.getInt("sens", 50)).coerceIn(0, 100)
            sensSlider.valueTo = 100f
            sensSlider.value = savedSens.toFloat()
            p.edit().putInt("sens", savedSens).putInt("sens_default", savedSens).apply()
            updateSensLabel(savedSens)
        }
    }

    // ---- Sensitivity ----------------------------------------------------------------------

    private fun updateSensLabel(v: Int) {
        val isFlashlight = prefs().getString("shake_action", "default") == "flashlight"
        val effectiveV = if (isFlashlight) v.coerceAtMost(45) else v
        var g = 4.0f - 2.2f * (effectiveV / 100f)      // same mapping as WakeService
        if (isFlashlight) {
            g = g.coerceAtLeast(3.0f)
        }
        sensTitleValue.text = getString(R.string.sens_title_value, g)
    }

    // ---- Proximity Sensor Live Status -----------------------------------------------------

    private fun renderProximity(covered: Boolean) {
        if (prox == null) {
            proxIndicator.setBackgroundResource(R.drawable.dot_unavailable)
            proxStatus.setText(R.string.status_unavailable)
            return
        }

        // When Maracas is disabled, restore the original proximity-dot appearance.
        // The custom Monet backing treatment is only used while Maracas is enabled.
        if (!masterOn) {
            proxIndicator.setBackgroundResource(
                if (covered) R.drawable.dot_on else R.drawable.dot_off
            )
            proxStatus.setText(if (covered) R.string.status_on else R.string.status_off)
            return
        }

        // Keep the indicator's yellow identity, but key its outline/backing to the
        // same primary Monet color used by the Master Power button.
        // Use the dark primary tone consistently in both themes. This matches the
        // dark-primary backing used by the Master Power button in light mode.
        val primary = monetTone(
            "accent1", 30, masterSwitch,
            com.google.android.material.R.attr.colorPrimary
        )
        val yellow = ContextCompat.getColor(this, R.color.maracas_yellow)
        val density = resources.displayMetrics.density
        val layers = if (covered) {
            // NEAR: primary outline around a solid static-yellow dot.
            arrayOf(
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(yellow)
                    setStroke((2f * density).roundToInt(), primary)
                }
            )
        } else {
            // FAR: primary backing, static-yellow ring, primary inner fill.
            arrayOf(
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(primary)
                },
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(yellow)
                },
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(primary)
                }
            )
        }
        proxIndicator.background = if (covered) {
            layers[0]
        } else {
            android.graphics.drawable.LayerDrawable(layers).apply {
                setLayerInset(1, (2f * density).roundToInt(), (2f * density).roundToInt(),
                    (2f * density).roundToInt(), (2f * density).roundToInt())
                setLayerInset(2, (5f * density).roundToInt(), (5f * density).roundToInt(),
                    (5f * density).roundToInt(), (5f * density).roundToInt())
            }
        }
        proxStatus.setText(if (covered) R.string.status_on else R.string.status_off)
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
        btnBattery.isEnabled = masterOn && !unrestricted
        btnBattery.setText(if (unrestricted) R.string.battery_granted else R.string.action_battery)
        // Keep the Material outlined-button state list intact. It supplies its own
        // neutral disabled foreground; do not paint a disabled background or border.
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

/** Subtle scale-bounce tap feedback for circular icon buttons. */
private fun View.animateTap() {
    animate().cancel()
    scaleX = 0.84f
    scaleY = 0.84f
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(300)
        .setInterpolator(OvershootInterpolator(4f))
        .start()
}
