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

/** A tappable thumbnail used in grids and rows. Images are portrait (3:4). */
@Composable
fun ImageCard(
    image: SowarImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .aspectRatio(IMAGE_ASPECT_RATIO)
            .semantics { contentDescription = image.description },
    ) {
        CaptionedImage(image = image, useThumbnail = true, modifier = Modifier.fillMaxSize())
    }
}

const val IMAGE_ASPECT_RATIO = 3f / 4f
