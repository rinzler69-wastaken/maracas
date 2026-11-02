package id.josjis.maracas.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager
import android.service.quicksettings.TileService
import id.josjis.maracas.MaracasTileService
import id.josjis.maracas.WakeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsData(
    val masterEnabled: Boolean = false,
    val materialYou: Boolean = true,
    val theme: String = "system", // "system", "light", "dark"
    val shakeEnabled: Boolean = true,
    val liftEnabled: Boolean = true,
    val shakeAction: String = "default", // "default", "flashlight"
    val sensDefault: Int = 50,
    val sensFlashlight: Int = 45,
    val liftSens: String = "gentle", // "gentle", "firm"
    val reachFlip: Boolean = true,
    val pocket: Boolean = true
) {
    val currentSens: Int
        get() = if (shakeAction == "flashlight") sensFlashlight.coerceIn(0, 45)
        else sensDefault.coerceIn(0, 100)
}

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("p", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readCurrentSettings())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = readCurrentSettings()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    fun readCurrentSettings(): SettingsData {
        val master = prefs.getBoolean("enabled", false)
        val my = prefs.getBoolean("material_you", true)
        val theme = prefs.getString("theme", "system") ?: "system"
        val shake = prefs.getBoolean("shake", true)
        val lift = prefs.getBoolean("lift", true)
        val rawAction = prefs.getString("shake_action", "default") ?: "default"
        val action = if (rawAction in listOf("default", "flashlight")) rawAction else "default"

        val sensFlashlight = prefs.getInt("sens_flashlight", prefs.getInt("sens", 45).coerceIn(0, 45)).coerceIn(0, 45)
        val sensDefault = prefs.getInt("sens_default", prefs.getInt("sens", 50)).coerceIn(0, 100)
        val liftSens = prefs.getString("lift_sens", "gentle") ?: "gentle"
        val reachFlip = prefs.getBoolean("reach_flip", prefs.getBoolean("pickup", true))
        val pocket = prefs.getBoolean("pocket", true)

        return SettingsData(
            masterEnabled = master,
            materialYou = my,
            theme = theme,
            shakeEnabled = shake,
            liftEnabled = lift,
            shakeAction = action,
            sensDefault = sensDefault,
            sensFlashlight = sensFlashlight,
            liftSens = liftSens,
            reachFlip = reachFlip,
            pocket = pocket
        )
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setMaterialYou(enabled: Boolean) {
        prefs.edit().putBoolean("material_you", enabled).apply()
        _settings.value = readCurrentSettings()
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _settings.value = readCurrentSettings()
    }

    fun setShakeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shake", enabled).apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setLiftEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lift", enabled).apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setShakeAction(action: String) {
        val validAction = if (action in listOf("default", "flashlight")) action else "default"
        val isFlashlight = validAction == "flashlight"
        val editor = prefs.edit().putString("shake_action", validAction)
        if (isFlashlight) {
            val savedSens = prefs.getInt("sens_flashlight", prefs.getInt("sens", 45).coerceIn(0, 45)).coerceIn(0, 45)
            editor.putInt("sens", savedSens).putInt("sens_flashlight", savedSens)
        } else {
            val savedSens = prefs.getInt("sens_default", prefs.getInt("sens", 50)).coerceIn(0, 100)
            editor.putInt("sens", savedSens).putInt("sens_default", savedSens)
        }
        editor.apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setSensValue(value: Int) {
        val isFlashlight = settings.value.shakeAction == "flashlight"
        val maxSens = if (isFlashlight) 45 else 100
        val clamped = value.coerceIn(0, maxSens)
        val prefKey = if (isFlashlight) "sens_flashlight" else "sens_default"
        prefs.edit()
            .putInt(prefKey, clamped)
            .putInt("sens", clamped)
            .apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setLiftSens(mode: String) {
        val valid = if (mode == "firm") "firm" else "gentle"
        prefs.edit().putString("lift_sens", valid).apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setReachFlip(enabled: Boolean) {
        prefs.edit()
            .putBoolean("reach_flip", enabled)
            .putBoolean("pickup", enabled) // legacy key compatibility
            .apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun setPocket(enabled: Boolean) {
        prefs.edit().putBoolean("pocket", enabled).apply()
        _settings.value = readCurrentSettings()
        syncService()
    }

    fun syncService() {
        val i = Intent(context, WakeService::class.java)
        val s = readCurrentSettings()
        val on = s.masterEnabled && (s.shakeEnabled || s.liftEnabled || s.reachFlip)
        if (on) {
            context.startForegroundService(i)
        } else {
            context.stopService(i)
        }
        try {
            TileService.requestListeningState(context, ComponentName(context, MaracasTileService::class.java))
        } catch (_: Exception) {}
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
}
