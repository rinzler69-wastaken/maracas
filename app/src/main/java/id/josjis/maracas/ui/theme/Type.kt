package id.josjis.maracas.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System-native typography: prefers Google Sans on Pixel / Android OS, with fallback to SansSerif
val GoogleSans = try {
    FontFamily(Typeface.create("google-sans", Typeface.NORMAL))
} catch (_: Exception) {
    FontFamily.SansSerif
}

val MaracasTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.01).sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.04.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
)
