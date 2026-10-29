package id.josjis.maracas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class MaracasTileService : TileService() {

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        getSharedPreferences("p", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefListener)
        updateTileState()
    }

    override fun onStopListening() {
        getSharedPreferences("p", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val p = getSharedPreferences("p", Context.MODE_PRIVATE)
        val current = p.getBoolean("enabled", false)
        val next = !current
        p.edit().putBoolean("enabled", next).apply()

        // Synchronize background WakeService
        val serviceIntent = Intent(this, WakeService::class.java)
        val shake = p.getBoolean("shake", true)
        val lift = p.getBoolean("lift", true)
        val reachFlip = p.getBoolean("reach_flip", p.getBoolean("pickup", true))
        val runService = next && (shake || lift || reachFlip)
        if (runService) {
            startForegroundService(serviceIntent)
        } else {
            stopService(serviceIntent)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val p = getSharedPreferences("p", Context.MODE_PRIVATE)
        val enabled = p.getBoolean("enabled", false)

        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = if (enabled) getString(R.string.state_on) else getString(R.string.state_off)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_maracas)
        tile.contentDescription = getString(R.string.master_cd)
        tile.updateTile()
    }
}
