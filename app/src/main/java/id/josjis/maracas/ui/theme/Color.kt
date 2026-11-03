package id.josjis.maracas.ui.theme

import androidx.compose.ui.graphics.Color

// Yellow identity for FAR/NEAR proximity indicator
val MaracasYellow = Color(0xFFFFC107)

// -------------------------------------------------------------------------
// Light theme fallback palette (Simplified industry-standard flat gray)
// -------------------------------------------------------------------------

// Accent 1 (Primary): Amethyst purple (swapped with theme button)
val MaracasPrimaryLight = Color(0xFF7B3FA2)
val MaracasOnPrimaryLight = Color(0xFFFFFFFF)

// Accent 2 (Secondary / Tertiary): Violet purple (0xFF6750A4)
val MaracasSecondaryLight = Color(0xFF6750A4)
val MaracasOnSecondaryLight = Color(0xFFFFFFFF)
val MaracasTertiaryLight = Color(0xFF6750A4)
val MaracasOnTertiaryLight = Color(0xFFFFFFFF)

// Material You button disabled state when master power is enabled (dark grey on light theme)
val MaracasM3DisabledLight = Color(0xFF4B5563)
val MaracasOnM3DisabledLight = Color(0xFFFFFFFF)

// Error
val MaracasErrorLight = Color(0xFFBA1A1A)
val MaracasOnErrorLight = Color(0xFFFFFFFF)
val MaracasErrorContainerLight = Color(0xFFFFDAD6)
val MaracasOnErrorContainerLight = Color(0xFF410002)

// Flat gray surfaces & background
val MaracasBackgroundLight = Color(0xFFF3F4F6)       // Very light gray background
val MaracasSurfaceLight = Color(0xFFF3F4F6)          // Very light gray surface
val MaracasOnSurfaceLight = Color(0xFF1F2937)        // Dark charcoal neutral text
val MaracasOnSurfaceVariantLight = Color(0xFF6B7280) // Muted neutral gray text
val MaracasOutlineLight = Color(0xFF9CA3AF)          // Neutral outline
val MaracasOutlineVariantLight = Color(0xFFD1D5DB)   // Subtle neutral outline

// Large surface island containers (Flat grays)
// Baseline: used when island is disabled / unselected
val MaracasContainerBaselineLight = Color(0xFFEFF1F5)
// Enabled: a little distinct when enabled than the baseline gray
val MaracasContainerEnabledLight = Color(0xFFE2E4E8)

val MaracasContainerLight = MaracasContainerBaselineLight
val MaracasContainerLowestLight = Color(0xFFFFFFFF)
val MaracasContainerLowLight = Color(0xFFECEEF2)
val MaracasContainerHighLight = Color(0xFFDCDFE5)
val MaracasContainerHighestLight = Color(0xFFD4D8DF)

// -------------------------------------------------------------------------
// Dark theme fallback palette (Simplified industry-standard flat gray)
// -------------------------------------------------------------------------

// Accent 1 (Primary): Royal Blue
val MaracasPrimaryDark = Color(0xFF4169E1)
val MaracasOnPrimaryDark = Color(0xFFFFFFFF)

// Accent 2 (Secondary / Tertiary): Navy Blue (brightened by 20%)
val MaracasSecondaryDark = Color(0xFF30609E)
val MaracasOnSecondaryDark = Color(0xFFFFFFFF)
val MaracasTertiaryDark = Color(0xFF30609E)
val MaracasOnTertiaryDark = Color(0xFFFFFFFF)

// Material You button disabled state when master power is enabled (25% darker grey on dark theme)
val MaracasM3DisabledDark = Color(0xFF9DA0A4)
val MaracasOnM3DisabledDark = Color(0xFF121316)

// Error
val MaracasErrorDark = Color(0xFFFFB4AB)
val MaracasOnErrorDark = Color(0xFF690005)
val MaracasErrorContainerDark = Color(0xFF93000A)
val MaracasOnErrorContainerDark = Color(0xFFFFDAD6)

// Flat gray surfaces & background
val MaracasBackgroundDark = Color(0xFF121316)        // Dark gray background
val MaracasSurfaceDark = Color(0xFF121316)           // Dark gray surface
val MaracasOnSurfaceDark = Color(0xFFE5E7EB)         // Clean light neutral text
val MaracasOnSurfaceVariantDark = Color(0xFF9CA3AF)  // Muted light neutral text
val MaracasOutlineDark = Color(0xFF6B7280)           // Neutral outline
val MaracasOutlineVariantDark = Color(0xFF374151)    // Dark neutral outline variant

// Large surface island containers (Flat grays)
// Baseline: used when island is disabled / unselected
val MaracasContainerBaselineDark = Color(0xFF1D1F24)
// Enabled: a little distinct when enabled than the baseline gray
val MaracasContainerEnabledDark = Color(0xFF282A31)

val MaracasContainerDark = MaracasContainerBaselineDark
val MaracasContainerLowestDark = Color(0xFF181A1D)
val MaracasContainerLowDark = Color(0xFF1D1F23)
val MaracasContainerHighDark = Color(0xFF2B2D33)
val MaracasContainerHighestDark = Color(0xFF34373E)
