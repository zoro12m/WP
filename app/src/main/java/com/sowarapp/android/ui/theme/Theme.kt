package com.sowarapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Rose40,
    onPrimary = Cream100,
    primaryContainer = Rose90,
    onPrimaryContainer = Rose10,
    secondary = Gold40,
    onSecondary = Cream100,
    secondaryContainer = Gold90,
    onSecondaryContainer = Gold10,
    tertiary = Plum40,
    onTertiary = Cream100,
    tertiaryContainer = Plum90,
    onTertiaryContainer = Plum10,
    background = Cream98,
    onBackground = Ink10,
    surface = Cream98,
    onSurface = Ink10,
    surfaceVariant = Cream90,
    onSurfaceVariant = Ink30,
    surfaceContainerLowest = Cream100,
    surfaceContainerLow = Cream96,
    surfaceContainer = Cream94,
    surfaceContainerHigh = Cream92,
    surfaceContainerHighest = Cream90,
    outline = Ink50,
    outlineVariant = Ink80,
)

private val DarkColors = darkColorScheme(
    primary = Rose80,
    onPrimary = Rose20,
    primaryContainer = Rose30,
    onPrimaryContainer = Rose90,
    secondary = Gold80,
    onSecondary = Gold10,
    secondaryContainer = Gold30,
    onSecondaryContainer = Gold90,
    tertiary = Plum80,
    onTertiary = Plum10,
    tertiaryContainer = Plum30,
    onTertiaryContainer = Plum90,
    background = Night6,
    onBackground = Night90,
    surface = Night6,
    onSurface = Night90,
    surfaceVariant = Ink30,
    onSurfaceVariant = Ink80,
    surfaceContainerLowest = Night6,
    surfaceContainerLow = Night10,
    surfaceContainer = Night12,
    surfaceContainerHigh = Night17,
    surfaceContainerHighest = Night22,
    outline = Ink50,
    outlineVariant = Ink30,
)

/** Brand colors that Material's color scheme has no slot for. */
@Immutable
data class SowarBrandColors(
    /** Background of the big rose header on the home screen. */
    val headerBrush: Brush,
    /** Gold used for thin decorations: section markers, rings around category photos. */
    val gold: Color,
    val goldBrush: Brush,
)

private val LightBrand = SowarBrandColors(
    headerBrush = Brush.linearGradient(listOf(Rose50, Rose30)),
    gold = GoldShine,
    goldBrush = Brush.sweepGradient(listOf(GoldShine, GoldLight, GoldShine, Gold40, GoldShine)),
)

private val DarkBrand = SowarBrandColors(
    headerBrush = Brush.linearGradient(listOf(Rose30, Rose20)),
    gold = Gold80,
    goldBrush = Brush.sweepGradient(listOf(Gold80, GoldLight, Gold80, GoldShine, Gold80)),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrand }

/** Soft, generous rounding everywhere, for a friendly and premium feel. */
private val SowarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Brand colors are used on purpose (no dynamic "Material You" colors) so the app always looks the same.
 * Dark mode follows the phone's setting.
 */
@Composable
fun SowarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBrandColors provides if (darkTheme) DarkBrand else LightBrand) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SowarTypography,
            shapes = SowarShapes,
            content = content,
        )
    }
}
