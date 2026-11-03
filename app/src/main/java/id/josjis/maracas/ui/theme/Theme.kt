package id.josjis.maracas.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.DynamicColors

private val LightColorScheme = lightColorScheme(
    primary = MaracasPrimaryLight,
    onPrimary = MaracasOnPrimaryLight,
    primaryContainer = MaracasContainerEnabledLight,
    onPrimaryContainer = MaracasOnSurfaceLight,
    secondary = MaracasSecondaryLight,
    onSecondary = MaracasOnSecondaryLight,
    secondaryContainer = MaracasContainerEnabledLight,
    onSecondaryContainer = MaracasOnSurfaceLight,
    tertiary = MaracasTertiaryLight,
    onTertiary = MaracasOnTertiaryLight,
    tertiaryContainer = MaracasContainerEnabledLight,
    onTertiaryContainer = MaracasOnSurfaceLight,
    error = MaracasErrorLight,
    onError = MaracasOnErrorLight,
    errorContainer = MaracasErrorContainerLight,
    onErrorContainer = MaracasOnErrorContainerLight,
    background = MaracasBackgroundLight,
    onBackground = MaracasOnSurfaceLight,
    surface = MaracasSurfaceLight,
    onSurface = MaracasOnSurfaceLight,
    surfaceVariant = MaracasContainerBaselineLight,
    onSurfaceVariant = MaracasOnSurfaceVariantLight,
    outline = MaracasOutlineLight,
    outlineVariant = MaracasOutlineVariantLight,
    surfaceContainerLowest = MaracasContainerLowestLight,
    surfaceContainerLow = MaracasContainerLowLight,
    surfaceContainer = MaracasContainerBaselineLight,
    surfaceContainerHigh = MaracasContainerHighLight,
    surfaceContainerHighest = MaracasContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MaracasPrimaryDark,
    onPrimary = MaracasOnPrimaryDark,
    primaryContainer = MaracasContainerEnabledDark,
    onPrimaryContainer = MaracasOnSurfaceDark,
    secondary = MaracasSecondaryDark,
    onSecondary = MaracasOnSecondaryDark,
    secondaryContainer = MaracasContainerEnabledDark,
    onSecondaryContainer = MaracasOnSurfaceDark,
    tertiary = MaracasTertiaryDark,
    onTertiary = MaracasOnTertiaryDark,
    tertiaryContainer = MaracasContainerEnabledDark,
    onTertiaryContainer = MaracasOnSurfaceDark,
    error = MaracasErrorDark,
    onError = MaracasOnErrorDark,
    errorContainer = MaracasErrorContainerDark,
    onErrorContainer = MaracasOnErrorContainerDark,
    background = MaracasBackgroundDark,
    onBackground = MaracasOnSurfaceDark,
    surface = MaracasSurfaceDark,
    onSurface = MaracasOnSurfaceDark,
    surfaceVariant = MaracasContainerBaselineDark,
    onSurfaceVariant = MaracasOnSurfaceVariantDark,
    outline = MaracasOutlineDark,
    outlineVariant = MaracasOutlineVariantDark,
    surfaceContainerLowest = MaracasContainerLowestDark,
    surfaceContainerLow = MaracasContainerLowDark,
    surfaceContainer = MaracasContainerBaselineDark,
    surfaceContainerHigh = MaracasContainerHighDark,
    surfaceContainerHighest = MaracasContainerHighestDark
)

val LocalContrastLevel = staticCompositionLocalOf { 0f }
val LocalDynamicColor = staticCompositionLocalOf { false }

@Composable
fun MaracasTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val canUseDynamic = dynamicColor &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            DynamicColors.isDynamicColorAvailable()

    val contrastLevel = produceState(initialValue = 0f, context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            if (uiModeManager != null) {
                value = uiModeManager.contrast
                val listener = UiModeManager.ContrastChangeListener { newContrast ->
                    value = newContrast
                }
                uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
                awaitDispose {
                    uiModeManager.removeContrastChangeListener(listener)
                }
            }
        }
    }.value

    val colorScheme = remember(canUseDynamic, darkTheme, contrastLevel, context) {
        val base = when {
            canUseDynamic && darkTheme -> dynamicDarkColorScheme(context)
            canUseDynamic && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
        if (canUseDynamic) {
            harmonizeContainers(base, darkTheme, contrastLevel)
        } else {
            harmonizeFallbackScheme(base, darkTheme, contrastLevel)
        }
    }

    CompositionLocalProvider(
        LocalContrastLevel provides contrastLevel,
        LocalDynamicColor provides canUseDynamic
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaracasTypography,
            content = content
        )
    }
}

private fun withLightness(color: Color, targetL: Double): Color {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(color.toArgb(), lab)
    lab[0] = targetL.coerceIn(5.0, 95.0)
    return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
}

private fun ensureLightContrast(onColor: Color, minL: Double = 88.0): Color {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(onColor.toArgb(), lab)
    if (lab[0] < minL) {
        lab[0] = minL
        return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
    }
    return onColor
}

private fun ensureDarkContrast(onColor: Color, maxL: Double = 18.0): Color {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(onColor.toArgb(), lab)
    if (lab[0] > maxL) {
        lab[0] = maxL
        return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
    }
    return onColor
}

/**
 * Resolves the subtitle alpha progression for dark mode:
 * - Regular contrast: 0.72f (exact regular contrast)
 * - Medium contrast: 0.82f (ever so slightly lighter)
 * - High contrast: 0.92f (lighter)
 */
fun resolveSubtitleAlpha(contrastLevel: Float): Float {
    return when {
        contrastLevel >= 0.75f -> 0.92f
        contrastLevel >= 0.25f -> 0.82f
        else -> 0.72f
    }
}

/**
 * Harmonizes wallpaper-derived Material You dynamic colors.
 */
private fun harmonizeContainers(scheme: ColorScheme, darkTheme: Boolean, contrastLevel: Float): ColorScheme {
    val isHighContrast = contrastLevel >= 0.75f
    val isMediumContrast = contrastLevel in 0.25f..<0.75f

    if (darkTheme) {
        // In dark mode:
        // Enabled card backgrounds NEVER budge between low, medium, and high contrast.
        // Base container lightness is strictly anchored at Tone 20.
        val targetL = 20.0
        val priContainer = withLightness(scheme.primaryContainer, targetL)
        val secContainer = withLightness(scheme.secondaryContainer, targetL)
        val terContainer = withLightness(scheme.tertiaryContainer, targetL)

        // Main text colors:
        // Default: 88.0, High: 98.0, Medium (exact middle ground): 93.0
        val minTextL = when {
            isHighContrast -> 98.0
            isMediumContrast -> 93.0
            else -> 88.0
        }

        // Subtitle / smaller text (onSurfaceVariant):
        // In dark mode: exactly same base colors as regular contrast, BUT:
        // Regular: Tone 70.0
        // Medium: Tone 76.0 (ever so slightly lighter)
        // High: Tone 84.0 (lighter)
        val onSurfaceVariantL = when {
            isHighContrast -> 84.0
            isMediumContrast -> 76.0
            else -> 70.0
        }

        // Small actionable buttons and active buttonlets (primary, secondary, tertiary):
        // Default: 80.0, High: 96.0, Medium (exact middle ground): 88.0
        val targetBtnL = when {
            isHighContrast -> 96.0
            isMediumContrast -> 88.0
            else -> 80.0
        }
        val pri = withLightness(scheme.primary, targetBtnL)
        val sec = withLightness(scheme.secondary, targetBtnL)
        val ter = withLightness(scheme.tertiary, targetBtnL)

        // On-button foreground:
        // Default: Tone 20.0, High: Tone 0.0 (Black), Medium (exact middle ground): Tone 10.0
        val onPri = if (isHighContrast) Color.Black else withLightness(scheme.onPrimary, if (isMediumContrast) 10.0 else 20.0)
        val onSec = if (isHighContrast) Color.Black else withLightness(scheme.onSecondary, if (isMediumContrast) 10.0 else 20.0)
        val onTer = if (isHighContrast) Color.Black else withLightness(scheme.onTertiary, if (isMediumContrast) 10.0 else 20.0)

        return scheme.copy(
            primary = pri,
            onPrimary = onPri,
            secondary = sec,
            onSecondary = onSec,
            tertiary = ter,
            onTertiary = onTer,
            primaryContainer = priContainer,
            secondaryContainer = secContainer,
            tertiaryContainer = terContainer,
            onPrimaryContainer = ensureLightContrast(scheme.onPrimaryContainer, minTextL),
            onSecondaryContainer = ensureLightContrast(scheme.onSecondaryContainer, minTextL),
            onTertiaryContainer = ensureLightContrast(scheme.onTertiaryContainer, minTextL),
            onSurface = ensureLightContrast(scheme.onSurface, minTextL),
            onSurfaceVariant = withLightness(scheme.onSurfaceVariant, onSurfaceVariantL)
        )
    } else {
        // In light mode:
        // Enabled card backgrounds NEVER budge between low, medium, and high contrast.
        // Base container lightness is strictly anchored at Tone 90.
        val targetL = 90.0
        val priContainer = withLightness(scheme.primaryContainer, targetL)
        val secContainer = withLightness(scheme.secondaryContainer, targetL)
        val terContainer = withLightness(scheme.tertiaryContainer, targetL)

        // Main text colors:
        // Default: 18.0, High: 2.0, Medium (exact middle ground): 10.0
        val maxTextL = when {
            isHighContrast -> 2.0
            isMediumContrast -> 10.0
            else -> 18.0
        }

        // Subtitle / smaller text in light mode:
        val onSurfaceVariantL = when {
            isHighContrast -> 18.0
            isMediumContrast -> 30.0
            else -> 42.0
        }

        // Small actionable buttons and active buttonlets (primary, secondary, tertiary):
        // Default: 40.0, High: 16.0, Medium (exact middle ground): 28.0
        val targetBtnL = when {
            isHighContrast -> 16.0
            isMediumContrast -> 28.0
            else -> 40.0
        }
        val pri = withLightness(scheme.primary, targetBtnL)
        val sec = withLightness(scheme.secondary, targetBtnL)
        val ter = withLightness(scheme.tertiary, targetBtnL)

        val onPri = Color.White
        val onSec = Color.White
        val onTer = Color.White

        return scheme.copy(
            primary = pri,
            onPrimary = onPri,
            secondary = sec,
            onSecondary = onSec,
            tertiary = ter,
            onTertiary = onTer,
            primaryContainer = priContainer,
            secondaryContainer = secContainer,
            tertiaryContainer = terContainer,
            onPrimaryContainer = ensureDarkContrast(scheme.onPrimaryContainer, maxTextL),
            onSecondaryContainer = ensureDarkContrast(scheme.onSecondaryContainer, maxTextL),
            onTertiaryContainer = ensureDarkContrast(scheme.onTertiaryContainer, maxTextL),
            onSurface = ensureDarkContrast(scheme.onSurface, maxTextL),
            onSurfaceVariant = withLightness(scheme.onSurfaceVariant, onSurfaceVariantL)
        )
    }
}

/**
 * Harmonizes the static flat gray fallback scheme when Material You is disabled.
 * Preserves the distinct baseline and enabled flat gray surfaces across all containers
 * and the vivid royal blue / navy blue / purple accents.
 */
private fun harmonizeFallbackScheme(
    scheme: ColorScheme,
    darkTheme: Boolean,
    contrastLevel: Float
): ColorScheme {
    val isHighContrast = contrastLevel >= 0.75f
    val isMediumContrast = contrastLevel in 0.25f..<0.75f

    if (!isHighContrast && !isMediumContrast) {
        return scheme
    }

    return if (darkTheme) {
        val onColor = if (isHighContrast) Color.White else Color(0xFFF3F4F6)
        val onVariantColor = if (isHighContrast) Color(0xFFCFD2DC) else Color(0xFFB5B8C2)
        scheme.copy(
            onSurface = onColor,
            onSurfaceVariant = onVariantColor,
            onPrimaryContainer = onColor,
            onSecondaryContainer = onColor,
            onTertiaryContainer = onColor
        )
    } else {
        val onColor = if (isHighContrast) Color.Black else Color(0xFF111827)
        val onVariantColor = if (isHighContrast) Color(0xFF374151) else Color(0xFF4B5563)
        scheme.copy(
            onSurface = onColor,
            onSurfaceVariant = onVariantColor,
            onPrimaryContainer = onColor,
            onSecondaryContainer = onColor,
            onTertiaryContainer = onColor
        )
    }
}

/**
 * Visual hierarchy for primary container elements:
 * - Dynamic color path:
 *   1. Maracas row (Header): "less darker" in dark (Tone 25), "less lighter" in light (Tone 85)
 *   2. Shake / Lift cards: baseline primaryContainer (Tone 20 / Tone 90)
 *   3. Active sensitivity box: "darker" in dark (Tone 15), "lighter" in light (Tone 95)
 * - Fallback path (Material You off):
 *   Flat gray, identical shade across all large surface islands when enabled (distinct from baseline).
 */
val ColorScheme.primaryContainerHeader: Color
    @Composable
    get() {
        if (!LocalDynamicColor.current) {
            return primaryContainer
        }
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(primaryContainer.toArgb(), lab)
        lab[0] = if (lab[0] < 55.0) 25.0 else 85.0
        return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
    }

val ColorScheme.primaryContainerSensitivity: Color
    @Composable
    get() {
        if (!LocalDynamicColor.current) {
            return primaryContainer
        }
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(primaryContainer.toArgb(), lab)
        lab[0] = if (lab[0] < 55.0) 15.0 else 95.0
        return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
    }

/**
 * Proximity sensor row:
 * - Dynamic color path: matches Maracas row lightness (Tone 25 in dark, Tone 85 in light)
 * - Fallback path: flat gray identical across all islands when enabled
 */
val ColorScheme.tertiaryContainerProximity: Color
    @Composable
    get() {
        if (!LocalDynamicColor.current) {
            return primaryContainer
        }
        val terLab = DoubleArray(3)
        ColorUtils.colorToLAB(tertiaryContainer.toArgb(), terLab)
        val priLab = DoubleArray(3)
        ColorUtils.colorToLAB(primaryContainer.toArgb(), priLab)
        terLab[0] = if (priLab[0] < 55.0) 25.0 else 85.0
        return Color(ColorUtils.LABToColor(terLab[0], terLab[1], terLab[2]))
    }

/**
 * Proximity sensor dot backing ring:
 * Always uses the deep light-mode primary tone in dynamic color mode.
 */
val ColorScheme.proximityDotPrimary: Color
    get() {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(primary.toArgb(), lab)
        if (lab[0] >= 50.0) {
            lab[0] = when {
                lab[0] >= 94.0 -> 16.0
                lab[0] >= 85.0 -> 28.0
                else -> 40.0
            }
            return Color(ColorUtils.LABToColor(lab[0], lab[1], lab[2]))
        }
        return primary
    }
