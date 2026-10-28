package com.sowarapp.android.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sowarapp.android.data.model.SowarImage

/**
 * A tappable photo card. In grids it keeps roughly the photo's own shape
 * (very tall or very wide photos are limited), which gives the Pinterest-style layout.
 */
@Composable
fun ImageCard(
    image: SowarImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = image.aspectRatio.coerceIn(MIN_CARD_RATIO, MAX_CARD_RATIO),
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .aspectRatio(aspectRatio)
            .semantics { contentDescription = image.description },
    ) {
        CaptionedImage(
            image = image,
            useThumbnail = true,
            captionStyle = CaptionStyle.Footer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val MIN_CARD_RATIO = 0.7f
private const val MAX_CARD_RATIO = 1.4f
