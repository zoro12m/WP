package com.sowarapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Rose40,
    onPrimary = Neutral99,
    primaryContainer = Rose90,
    onPrimaryContainer = Rose10,
    secondary = Gold40,
    onSecondary = Neutral99,
    secondaryContainer = Gold90,
    onSecondaryContainer = Gold10,
    tertiary = Teal40,
    onTertiary = Neutral99,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainer = Neutral95,
    outline = NeutralVariant50,
)

private val DarkColors = darkColorScheme(
    primary = Rose80,
    onPrimary = Rose10,
    primaryContainer = Rose30,
    onPrimaryContainer = Rose90,
    secondary = Gold80,
    onSecondary = Gold10,
    secondaryContainer = Gold30,
    onSecondaryContainer = Gold90,
    tertiary = Teal80,
    onTertiary = Teal10,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainer = Neutral20,
    surfaceContainerHigh = Neutral24,
    outline = NeutralVariant50,
)

/** Brand colors are used on purpose (no dynamic "Material You" colors) so the app always looks the same. */
@Composable
fun SowarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SowarTypography,
        content = content,
    )
}
