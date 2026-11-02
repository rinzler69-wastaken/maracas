package id.josjis.maracas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.josjis.maracas.ui.MainViewModel
import id.josjis.maracas.ui.MaracasScreen
import id.josjis.maracas.ui.theme.MaracasTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: MainViewModel by viewModels()
    private var proximitySensor: Sensor? = null
    private var sensorManager: SensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        viewModel.updateProximity(
            covered = false,
            hasSensor = proximitySensor != null
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.settings.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            MaracasTheme(
                themeMode = uiState.settings.theme,
                dynamicColor = uiState.settings.materialYou
            ) {
                MaracasScreen(
                    uiState = uiState,
                    onToggleMaster = viewModel::toggleMaster,
                    onToggleMaterialYou = viewModel::toggleMaterialYou,
                    onCycleThemeQuick = viewModel::cycleThemeQuick,
                    onSetThemeSystem = {
                        viewModel.setThemeSystem()
                        toast(R.string.theme_set_system)
                    },
                    onToggleShake = viewModel::toggleShake,
                    onToggleLift = viewModel::toggleLift,
                    onToggleSensMode = viewModel::toggleSensMode,
                    onSensChanged = viewModel::setSensValue,
                    onSetShakeAction = viewModel::setShakeAction,
                    onSetLiftSens = viewModel::setLiftSens,
                    onToggleReachFlip = viewModel::setReachFlip,
                    onTogglePocket = viewModel::setPocket,
                    onRequestUnrestrictedBattery = ::requestUnrestrictedBattery,
                    onShowDialog = viewModel::showDialog,
                    onDismissDialog = viewModel::dismissDialog
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryStatus()
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val near = event.values[0] < minOf(5f, event.sensor.maximumRange)
            viewModel.updateProximity(covered = near, hasSensor = true)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun requestUnrestrictedBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast(R.string.battery_already)
        } else {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Exception) {
                    toast(R.string.battery_manual, long = true)
                }
            }
        }
        viewModel.refreshBatteryStatus()
    }

    private fun toast(@StringRes msg: Int, long: Boolean = false) {
        Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}
