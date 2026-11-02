package id.josjis.maracas.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.DynamicColors
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class MonetTones(
    val wakeEnabledBackground: Color,
    val wakeEnabledForeground: Color,
    val sensitivityPanelBackground: Color,
    val protectionRowBackground: Color,
    val protectionRowForeground: Color,
    val proximityCardBackground: Color,
    val proximityCardForeground: Color,
    val proximityDotPrimary: Color
) {
    fun mutedSubtitle(fg: Color, bg: Color): Color {
        val blended = ColorUtils.blendARGB(fg.toArgb(), bg.toArgb(), 0.20f)
        return Color(blended)
    }

    companion object {
        const val DISABLED_ALPHA = 0.38f
        private val resIdCache = ConcurrentHashMap<String, Int>()

        fun resolve(context: Context, isNight: Boolean, dynamicColorEnabled: Boolean, colorScheme: ColorScheme): MonetTones {
            val useMonet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    dynamicColorEnabled &&
                    DynamicColors.isDynamicColorAvailable()

            fun monetResourceIndex(tone: Int): Int? = when (tone) {
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

            fun sample(family: String, index: Int): Int? {
                val resName = "system_${family}_$index"
                val resId = resIdCache.computeIfAbsent(resName) {
                    context.resources.getIdentifier(it, "color", "android")
                }
                if (resId == 0) return null
                return try {
                    context.resources.getColor(resId, context.theme)
                } catch (_: Exception) {
                    null
                }
            }

            fun interpolatedSample(family: String, lowerIdx: Int, upperIdx: Int): Int? {
                val lower = sample(family, lowerIdx) ?: return null
                val upper = sample(family, upperIdx) ?: return null
                return ColorUtils.blendARGB(lower, upper, 0.5f)
            }

            fun resolveTone(family: String, tone: Int, fallback: Color): Color {
                if (useMonet) {
                    val dynamicInt = when (tone) {
                        25 -> interpolatedSample(family, 800, 700)
                        75 -> interpolatedSample(family, 300, 200)
                        else -> monetResourceIndex(tone)?.let { sample(family, it) }
                    }
                    if (dynamicInt != null) return Color(dynamicInt)
                }

                // Baseline fallback samples matching original MainActivity.kt fallbackSample logic:
                return when ("$family:$tone") {
                    "accent1:20" -> MaracasPrimaryTone20
                    "accent1:25" -> MaracasPrimaryTone25
                    "accent1:30" -> MaracasPrimaryTone30
                    "accent1:75" -> MaracasPrimaryTone75
                    "accent1:90" -> MaracasPrimaryTone90
                    "accent1:95" -> MaracasPrimaryTone95
                    "accent2:30" -> MaracasSecondaryTone30
                    "accent2:95" -> MaracasSecondaryTone95
                    "accent3:20" -> MaracasTertiaryTone20
                    "accent3:30" -> MaracasTertiaryTone30
                    "accent3:90" -> MaracasTertiaryTone90
                    "accent3:95" -> MaracasTertiaryTone95
                    else -> fallback
                }
            }

            // Exactly matching MainActivity.kt:
            // wakeEnabledBackground: accent1 tone 30 (night) / 85 (light), fallback colorPrimaryContainer
            val wakeBg = resolveTone("accent1", if (isNight) 30 else 85, colorScheme.primaryContainer)
            // wakeEnabledForeground: accent1 tone 95 (night) / 10 (light), fallback colorOnPrimaryContainer
            val wakeFg = resolveTone("accent1", if (isNight) 95 else 10, colorScheme.onPrimaryContainer)
            // sensitivityPanelBackground: accent1 tone 20 (night) / 90 (light), fallback colorPrimaryContainer
            val sensBg = resolveTone("accent1", if (isNight) 20 else 90, colorScheme.primaryContainer)
            // protectionRowBackground: accent2 tone 20 (night) / 85 (light), fallback colorSecondaryContainer
            val protBg = resolveTone("accent2", if (isNight) 20 else 85, colorScheme.secondaryContainer)
            // protectionRowForeground: accent2 tone 95 (night) / 10 (light), fallback colorOnSecondaryContainer
            val protFg = resolveTone("accent2", if (isNight) 95 else 10, colorScheme.onSecondaryContainer)
            // proximityCardBackground: accent3 tone 20 (night) / 85 (light), fallback colorTertiaryContainer
            val proxBg = resolveTone("accent3", if (isNight) 20 else 85, colorScheme.tertiaryContainer)
            // proximityCardForeground: accent3 tone 80 (night) / 20 (light), fallback colorOnTertiaryContainer
            val proxFg = resolveTone("accent3", if (isNight) 80 else 20, colorScheme.onTertiaryContainer)
            // proximityDotPrimary: accent1 tone 30, fallback colorPrimary
            val proxDot = resolveTone("accent1", 30, colorScheme.primary)

            return MonetTones(
                wakeEnabledBackground = wakeBg,
                wakeEnabledForeground = wakeFg,
                sensitivityPanelBackground = sensBg,
                protectionRowBackground = protBg,
                protectionRowForeground = protFg,
                proximityCardBackground = proxBg,
                proximityCardForeground = proxFg,
                proximityDotPrimary = proxDot
            )
        }
    }
}

val LocalMonetTones = staticCompositionLocalOf<MonetTones> {
    error("No MonetTones provided")
}

val monetTones: MonetTones
    @Composable
    @ReadOnlyComposable
    get() = LocalMonetTones.current
