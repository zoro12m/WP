package com.sowarapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.sowarapp.android.data.ImageExporter
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.theme.Cairo

/** How the caption is shown on top of the photo. */
enum class CaptionStyle {
    /** Big text in the middle of the photo, exactly like the downloaded file. For the image viewer. */
    Overlay,

    /** Small text at the bottom on a dark fade, readable on any photo. For grids and cards. */
    Footer,

    /** No caption (e.g. when the card shows its own title). */
    None,
}

/**
 * Shows the photo, with its Arabic caption on top when [SowarImage.showCaption] is true.
 *
 * Grids pass [useThumbnail] = true to download the small version of the photo.
 * The image is decorative for screen readers; the clickable card around it provides the description.
 */
@Composable
fun CaptionedImage(
    image: SowarImage,
    modifier: Modifier = Modifier,
    useThumbnail: Boolean = false,
    captionStyle: CaptionStyle = CaptionStyle.Overlay,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val model = if (useThumbnail) image.thumbnailUrl else image.imageUrl
    var isLoading by remember(model) { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier.then(if (isLoading) Modifier.shimmer() else Modifier),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = contentScale,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!image.showCaption || image.caption.isBlank()) return@BoxWithConstraints

        when (captionStyle) {
            CaptionStyle.Overlay -> {
                // Same proportions as ImageExporter, so what you see is what gets downloaded.
                val fontSize = with(LocalDensity.current) { (maxWidth * ImageExporter.CAPTION_SIZE_RATIO).toSp() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        // Move the text centre from 50% to 60% of the height, like the exported file.
                        .offset(y = maxHeight * (ImageExporter.CAPTION_VERTICAL_POSITION - 0.5f))
                        // A soft dark band behind the words keeps them readable on bright photos.
                        .background(captionBandBrush())
                        .padding(vertical = maxWidth * ImageExporter.CAPTION_SIZE_RATIO * ImageExporter.CAPTION_BAND_PADDING),
                ) {
                    Text(
                        text = image.caption,
                        modifier = Modifier.fillMaxWidth(ImageExporter.CAPTION_WIDTH_RATIO),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            color = Color.White,
                            fontFamily = Cairo,
                            fontSize = fontSize,
                            lineHeight = fontSize * ImageExporter.CAPTION_LINE_HEIGHT,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), blurRadius = 10f),
                        ),
                    )
                }
            }

            CaptionStyle.Footer -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.72f),
                            )
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 10.dp),
                ) {
                    Text(
                        text = image.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), blurRadius = 4f),
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            CaptionStyle.None -> Unit
        }
    }
}

/** Transparent at the edges, darker in the middle: the same band ImageExporter draws. */
private fun captionBandBrush() = Brush.verticalGradient(
    0f to Color.Transparent,
    0.5f to Color.Black.copy(alpha = ImageExporter.CAPTION_BAND_ALPHA),
    1f to Color.Transparent,
)
