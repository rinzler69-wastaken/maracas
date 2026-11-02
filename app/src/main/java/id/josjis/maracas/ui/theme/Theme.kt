package id.josjis.maracas.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.DynamicColors

private val LightColorScheme = lightColorScheme(
    primary = MaracasPrimaryLight,
    onPrimary = MaracasOnPrimaryLight,
    primaryContainer = MaracasPrimaryContainerLight,
    onPrimaryContainer = MaracasOnPrimaryContainerLight,
    secondary = MaracasSecondaryLight,
    onSecondary = MaracasOnSecondaryLight,
    secondaryContainer = MaracasSecondaryContainerLight,
    onSecondaryContainer = MaracasOnSecondaryContainerLight,
    tertiary = MaracasTertiaryLight,
    onTertiary = MaracasOnTertiaryLight,
    tertiaryContainer = MaracasTertiaryContainerLight,
    onTertiaryContainer = MaracasOnTertiaryContainerLight,
    error = MaracasErrorLight,
    onError = MaracasOnErrorLight,
    errorContainer = MaracasErrorContainerLight,
    onErrorContainer = MaracasOnErrorContainerLight,
    background = MaracasSurfaceLight,
    onBackground = MaracasOnSurfaceLight,
    surface = MaracasSurfaceLight,
    onSurface = MaracasOnSurfaceLight,
    surfaceVariant = MaracasSurfaceVariantLight,
    onSurfaceVariant = MaracasOnSurfaceVariantLight,
    outline = MaracasOutlineLight,
    outlineVariant = MaracasOutlineVariantLight,
    surfaceContainerLowest = MaracasContainerLowestLight,
    surfaceContainerLow = MaracasContainerLowLight,
    surfaceContainer = MaracasContainerLight,
    surfaceContainerHigh = MaracasContainerHighLight,
    surfaceContainerHighest = MaracasContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MaracasPrimaryDark,
    onPrimary = MaracasOnPrimaryDark,
    primaryContainer = MaracasPrimaryContainerDark,
    onPrimaryContainer = MaracasOnPrimaryContainerDark,
    secondary = MaracasSecondaryDark,
    onSecondary = MaracasOnSecondaryDark,
    secondaryContainer = MaracasSecondaryContainerDark,
    onSecondaryContainer = MaracasOnSecondaryContainerDark,
    tertiary = MaracasTertiaryDark,
    onTertiary = MaracasOnTertiaryDark,
    tertiaryContainer = MaracasTertiaryContainerDark,
    onTertiaryContainer = MaracasOnTertiaryContainerDark,
    error = MaracasErrorDark,
    onError = MaracasOnErrorDark,
    errorContainer = MaracasErrorContainerDark,
    onErrorContainer = MaracasOnErrorContainerDark,
    background = MaracasSurfaceDark,
    onBackground = MaracasOnSurfaceDark,
    surface = MaracasSurfaceDark,
    onSurface = MaracasOnSurfaceDark,
    surfaceVariant = MaracasSurfaceVariantDark,
    onSurfaceVariant = MaracasOnSurfaceVariantDark,
    outline = MaracasOutlineDark,
    outlineVariant = MaracasOutlineVariantDark,
    surfaceContainerLowest = MaracasContainerLowestDark,
    surfaceContainerLow = MaracasContainerLowDark,
    surfaceContainer = MaracasContainerDark,
    surfaceContainerHigh = MaracasContainerHighDark,
    surfaceContainerHighest = MaracasContainerHighestDark
)

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

    val colorScheme = when {
        canUseDynamic && darkTheme -> dynamicDarkColorScheme(context)
        canUseDynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val monetTones = androidx.compose.runtime.remember(context, darkTheme, dynamicColor, colorScheme) {
        MonetTones.resolve(
            context = context,
            isNight = darkTheme,
            dynamicColorEnabled = dynamicColor,
            colorScheme = colorScheme
        )
    }

    CompositionLocalProvider(LocalMonetTones provides monetTones) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaracasTypography,
            content = content
        )
    }
}
