package id.josjis.maracas

import id.josjis.maracas.data.SettingsData
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDataTest {

    @Test
    fun defaultSettings_hasSens50() {
        val settings = SettingsData()
        assertEquals(50, settings.currentSens)
        assertEquals("default", settings.shakeAction)
    }

    @Test
    fun flashlightAction_clampsSensTo45() {
        val settings = SettingsData(
            shakeAction = "flashlight",
            sensFlashlight = 80
        )
        assertEquals(45, settings.currentSens)
    }

    @Test
    fun gCalculationMatchesOriginalMapping() {
        // g = 4.0f - 2.2f * (effectiveV / 100f)
        // at sens = 0 -> 4.0g
        val v0 = 0
        val g0 = 4.0f - 2.2f * (v0 / 100f)
        assertEquals(4.0f, g0, 0.001f)

        // at sens = 50 -> 2.9g
        val v50 = 50
        val g50 = 4.0f - 2.2f * (v50 / 100f)
        assertEquals(2.9f, g50, 0.001f)

        // at sens = 100 -> 1.8g
        val v100 = 100
        val g100 = 4.0f - 2.2f * (v100 / 100f)
        assertEquals(1.8f, g100, 0.001f)
    }
}
