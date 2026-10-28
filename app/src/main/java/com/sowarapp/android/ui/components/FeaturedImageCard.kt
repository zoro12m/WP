package com.sowarapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sowarapp.android.R
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.theme.Gold10
import com.sowarapp.android.ui.theme.LocalBrandColors

/** The big "picture of the day" banner on the home screen. */
@Composable
fun FeaturedImageCard(
    image: SowarImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.section_featured)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .semantics { contentDescription = "$label: ${image.description}" },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CaptionedImage(image = image, captionStyle = CaptionStyle.None, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f),
                        )
                    ),
            )
            // Gold "picture of the day" badge.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .background(LocalBrandColors.current.goldBrush, MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Gold10, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = Gold10)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
            ) {
                Text(
                    text = image.description,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.featured_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
