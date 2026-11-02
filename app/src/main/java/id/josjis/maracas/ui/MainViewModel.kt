package id.josjis.maracas.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.color.DynamicColors
import id.josjis.maracas.data.SettingsData
import id.josjis.maracas.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class SensMode { SHAKE, LIFT }
enum class ProximityState { UNAVAILABLE, FAR, NEAR }

sealed interface ActiveDialog {
    data class Info(@StringRes val titleRes: Int, @StringRes val messageRes: Int) : ActiveDialog
}

data class MainUiState(
    val settings: SettingsData = SettingsData(),
    val expandedSensMode: SensMode? = null,
    val proximityState: ProximityState = ProximityState.UNAVAILABLE,
    val isBatteryUnrestricted: Boolean = false,
    val activeDialog: ActiveDialog? = null,
    val isDynamicColorAvailable: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val expandedSensMode = MutableStateFlow<SensMode?>(null)
    private val proximityState = MutableStateFlow(ProximityState.UNAVAILABLE)
    private val isBatteryUnrestricted = MutableStateFlow(repository.isIgnoringBatteryOptimizations())
    private val activeDialog = MutableStateFlow<ActiveDialog?>(null)
    private val isDynamicColorAvailable = DynamicColors.isDynamicColorAvailable()

    val uiState: StateFlow<MainUiState> = combine(
        repository.settings,
        expandedSensMode,
        proximityState,
        isBatteryUnrestricted,
        activeDialog
    ) { settings, sensMode, prox, battery, dialog ->
        MainUiState(
            settings = settings,
            expandedSensMode = if (settings.masterEnabled) sensMode else null,
            proximityState = prox,
            isBatteryUnrestricted = battery,
            activeDialog = dialog,
            isDynamicColorAvailable = isDynamicColorAvailable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            settings = repository.readCurrentSettings(),
            isBatteryUnrestricted = repository.isIgnoringBatteryOptimizations(),
            isDynamicColorAvailable = isDynamicColorAvailable
        )
    )

    fun toggleMaster() {
        val next = !uiState.value.settings.masterEnabled
        if (!next) {
            expandedSensMode.value = null
        }
        repository.setMasterEnabled(next)
    }

    fun toggleMaterialYou() {
        val next = !uiState.value.settings.materialYou
        repository.setMaterialYou(next)
    }

    fun setTheme(theme: String) {
        repository.setTheme(theme)
    }

    fun cycleThemeQuick() {
        val current = uiState.value.settings.theme
        val next = when (current) {
            "light" -> "dark"
            "dark" -> "system"
            else -> "light"
        }
        repository.setTheme(next)
    }

    fun setThemeSystem() {
        repository.setTheme("system")
    }

    fun toggleShake() {
        val next = !uiState.value.settings.shakeEnabled
        if (!next && expandedSensMode.value == SensMode.SHAKE) {
            expandedSensMode.value = null
        }
        repository.setShakeEnabled(next)
    }

    fun toggleLift() {
        val next = !uiState.value.settings.liftEnabled
        if (!next && expandedSensMode.value == SensMode.LIFT) {
            expandedSensMode.value = null
        }
        repository.setLiftEnabled(next)
    }

    fun toggleSensMode(mode: SensMode) {
        expandedSensMode.value = if (expandedSensMode.value == mode) null else mode
    }

    fun setSensValue(value: Int) {
        repository.setSensValue(value)
    }

    fun setShakeAction(action: String) {
        repository.setShakeAction(action)
    }

    fun setLiftSens(mode: String) {
        repository.setLiftSens(mode)
    }

    fun setReachFlip(enabled: Boolean) {
        repository.setReachFlip(enabled)
    }

    fun setPocket(enabled: Boolean) {
        repository.setPocket(enabled)
    }

    fun updateProximity(covered: Boolean, hasSensor: Boolean) {
        proximityState.value = if (!hasSensor) {
            ProximityState.UNAVAILABLE
        } else if (covered) {
            ProximityState.NEAR
        } else {
            ProximityState.FAR
        }
    }

    fun refreshBatteryStatus() {
        isBatteryUnrestricted.value = repository.isIgnoringBatteryOptimizations()
    }

    fun showDialog(dialog: ActiveDialog) {
        activeDialog.value = dialog
    }

    fun dismissDialog() {
        activeDialog.value = null
    }
}
