package com.sowarapp.android.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Lets one item inside a padded list stretch to the screen edges,
 * e.g. the home header inside a grid that has 16dp side padding.
 */
fun Modifier.fullBleed(horizontalPadding: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontalPadding.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
