package id.josjis.maracas.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R
import id.josjis.maracas.ui.components.BatteryOptimizationButton
import id.josjis.maracas.ui.components.HeaderCard
import id.josjis.maracas.ui.components.InfoDialog
import id.josjis.maracas.ui.components.ProtectionSection
import id.josjis.maracas.ui.components.ProximitySensorCard
import id.josjis.maracas.ui.components.SensitivitySection
import id.josjis.maracas.ui.components.WakeMethodSelector

@Composable
fun MaracasScreen(
    uiState: MainUiState,
    onToggleMaster: () -> Unit,
    onToggleMaterialYou: () -> Unit,
    onCycleThemeQuick: () -> Unit,
    onSetThemeSystem: () -> Unit,
    onToggleShake: () -> Unit,
    onToggleLift: () -> Unit,
    onToggleSensMode: (SensMode) -> Unit,
    onSensChanged: (Int) -> Unit,
    onSetShakeAction: (String) -> Unit,
    onSetLiftSens: (String) -> Unit,
    onToggleReachFlip: (Boolean) -> Unit,
    onTogglePocket: (Boolean) -> Unit,
    onRequestUnrestrictedBattery: () -> Unit,
    onShowDialog: (ActiveDialog) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val pageMargin = dimensionResource(R.dimen.page_margin)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val minHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = pageMargin, top = 8.dp, end = pageMargin, bottom = 16.dp)
                    .defaultMinSize(minHeight = minHeight - 24.dp)
            ) {
                // 1. Header Card (App Title, Master switch, Material You, Theme)
                HeaderCard(
                    masterOn = settings.masterEnabled,
                    materialYouOn = settings.materialYou,
                    themeMode = settings.theme,
                    isDynamicColorAvailable = uiState.isDynamicColorAvailable,
                    onToggleMaster = onToggleMaster,
                    onToggleMaterialYou = onToggleMaterialYou,
                    onCycleThemeQuick = onCycleThemeQuick,
                    onSetThemeSystem = onSetThemeSystem
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Wake Method Selector (Shake & Lift tiles)
                WakeMethodSelector(
                    masterOn = settings.masterEnabled,
                    shakeEnabled = settings.shakeEnabled,
                    liftEnabled = settings.liftEnabled,
                    expandedSensMode = uiState.expandedSensMode,
                    onToggleShake = onToggleShake,
                    onToggleLift = onToggleLift,
                    onAdjustShake = { onToggleSensMode(SensMode.SHAKE) },
                    onAdjustLift = { onToggleSensMode(SensMode.LIFT) },
                    onShowShakeInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.shake_title, R.string.shake_info))
                    },
                    onShowLiftInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.lift_title, R.string.lift_info))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Sensitivity Section (Placeholder, Shake slider panel, or Lift buttonlets panel)
                SensitivitySection(
                    masterOn = settings.masterEnabled,
                    shakeEnabled = settings.shakeEnabled,
                    liftEnabled = settings.liftEnabled,
                    expandedSensMode = uiState.expandedSensMode,
                    shakeSens = settings.currentSens,
                    shakeAction = settings.shakeAction,
                    liftSens = settings.liftSens,
                    onSensChanged = onSensChanged,
                    onSetShakeAction = onSetShakeAction,
                    onSetLiftSens = onSetLiftSens,
                    onShowShakeSensInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.sens_title, R.string.sens_info))
                    },
                    onShowLiftSensInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.lift_sens_title, R.string.lift_sens_info))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Protection Rows (Reach / Flip & Pocket Protection)
                ProtectionSection(
                    masterOn = settings.masterEnabled,
                    reachFlipEnabled = settings.reachFlip,
                    pocketEnabled = settings.pocket,
                    onToggleReachFlip = onToggleReachFlip,
                    onTogglePocket = onTogglePocket,
                    onShowReachFlipInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.reach_flip_title, R.string.reach_flip_info))
                    },
                    onShowPocketInfo = {
                        onShowDialog(ActiveDialog.Info(R.string.pocket_protection_title, R.string.pocket_protection_info))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Proximity Sensor Status Card
                ProximitySensorCard(
                    masterOn = settings.masterEnabled,
                    proximityState = uiState.proximityState
                )

                // Pushes the bottom action down on tall screens
                Spacer(modifier = Modifier.weight(1f))

                // 6. Bottom action: Allow unrestricted battery
                BatteryOptimizationButton(
                    masterOn = settings.masterEnabled,
                    isBatteryUnrestricted = uiState.isBatteryUnrestricted,
                    onRequestUnrestrictedBattery = onRequestUnrestrictedBattery,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    // Active Dialogs
    when (val dialog = uiState.activeDialog) {
        is ActiveDialog.Info -> {
            InfoDialog(
                title = stringResource(dialog.titleRes),
                message = stringResource(dialog.messageRes),
                onDismiss = onDismissDialog
            )
        }
        null -> Unit
    }
}
