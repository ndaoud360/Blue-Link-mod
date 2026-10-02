package com.bluedeck.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bluedeck.R

/** Inter (SIL OFL 1.1) bundled in res/font. */
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

/**
 * Scale tuned for glanceable automotive data: large light numerals for values,
 * medium-weight small text for labels.
 */
val BlueDeckTypography = Typography(
    displayLarge = style(64, 68, FontWeight.Light, -1.5),
    displayMedium = style(48, 52, FontWeight.Light, -1.0),
    displaySmall = style(36, 40, FontWeight.Light, -0.6),
    headlineLarge = style(30, 36, FontWeight.Normal, -0.4),
    headlineMedium = style(26, 32, FontWeight.Normal, -0.3),
    headlineSmall = style(22, 28, FontWeight.Medium, -0.2),
    titleLarge = style(20, 26, FontWeight.Medium, -0.1),
    titleMedium = style(16, 22, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal, 0.1),
    labelLarge = style(14, 20, FontWeight.Medium, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.2),
    labelSmall = style(11, 14, FontWeight.Medium, 0.3)
)
