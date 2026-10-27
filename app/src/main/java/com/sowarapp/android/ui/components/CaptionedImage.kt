package com.sowarapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sowarapp.android.data.ImageExporter
import com.sowarapp.android.data.model.SowarImage

/**
 * Shows the photo, with its Arabic caption on top when [SowarImage.showCaption] is true.
 * Uses the same proportions as [ImageExporter], so what you see is what gets downloaded.
 *
 * Grids pass [useThumbnail] = true to download the small version of the photo.
 *
 * The image is decorative for screen readers; the clickable card around it
 * provides the description.
 */
@Composable
fun CaptionedImage(
    image: SowarImage,
    modifier: Modifier = Modifier,
    useThumbnail: Boolean = false,
) {
    // The soft background shows while the photo downloads, or if it fails to load.
    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = if (useThumbnail) image.thumbnailUrl else image.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (!image.showCaption || image.caption.isBlank()) return@BoxWithConstraints
        val fontSize = (maxWidth.value * ImageExporter.CAPTION_SIZE_RATIO).sp
        Box(
            modifier = Modifier
                .fillMaxWidth(ImageExporter.CAPTION_WIDTH_RATIO)
                .wrapContentHeight()
                .align(Alignment.Center)
                // Move the text centre from 50% to 60% of the height, like the exported file.
                .offset(y = maxHeight * (ImageExporter.CAPTION_VERTICAL_POSITION - 0.5f)),
        ) {
            Text(
                text = image.caption,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.25f,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), blurRadius = 8f),
                ),
            )
        }
    }
}
