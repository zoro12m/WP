package com.sowarapp.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sowarapp.android.R

/**
 * Cairo: a modern Arabic font (SIL Open Font License, see licenses/Cairo-OFL.txt).
 * It is bundled in res/font, so it looks the same on every phone and needs no internet.
 */
val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.cairo(weight: FontWeight? = fontWeight) =
    copy(fontFamily = Cairo, fontWeight = weight)

val SowarTypography = Typography(
    displayLarge = base.displayLarge.cairo(FontWeight.Bold),
    displayMedium = base.displayMedium.cairo(FontWeight.Bold),
    displaySmall = base.displaySmall.cairo(FontWeight.Bold),
    headlineLarge = base.headlineLarge.cairo(FontWeight.Bold),
    headlineMedium = base.headlineMedium.cairo(FontWeight.Bold),
    headlineSmall = base.headlineSmall.cairo(FontWeight.Bold),
    titleLarge = base.titleLarge.cairo(FontWeight.Bold),
    titleMedium = base.titleMedium.cairo(FontWeight.SemiBold),
    titleSmall = base.titleSmall.cairo(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.cairo(FontWeight.Normal),
    bodyMedium = base.bodyMedium.cairo(FontWeight.Normal),
    bodySmall = base.bodySmall.cairo(FontWeight.Normal),
    labelLarge = base.labelLarge.cairo(FontWeight.SemiBold),
    labelMedium = base.labelMedium.cairo(FontWeight.SemiBold),
    labelSmall = base.labelSmall.cairo(FontWeight.Normal),
)
