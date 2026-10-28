package com.sowarapp.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** A soft light sweeping across the placeholder while a photo is downloading. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    drawBehind {
        // Reading progress here (not above) only redraws; it doesn't recompose every frame.
        val x = size.width * progress.value
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x - size.width, 0f),
                end = Offset(x, size.height),
            )
        )
    }
}
