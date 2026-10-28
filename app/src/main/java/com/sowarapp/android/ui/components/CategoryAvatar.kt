package com.sowarapp.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.sowarapp.android.R
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.ui.theme.LocalBrandColors

/** A round category photo with a thin gold ring and the name under it, like "stories" in social apps. */
@Composable
fun CategoryAvatar(
    category: Category,
    imageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countText = stringResource(R.string.category_image_count, imageCount)
    var isLoading by remember(category.coverUrl) { mutableStateOf(true) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .width(84.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            // Read as one item: "صباح الخير، عدد الصور: 12".
            .clearAndSetSemantics {
                contentDescription = "${category.title}، $countText"
                role = Role.Button
            }
            .padding(vertical = 4.dp),
    ) {
        AsyncImage(
            model = category.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty
            },
            modifier = Modifier
                .size(76.dp)
                .border(2.5.dp, LocalBrandColors.current.goldBrush, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .then(if (isLoading) Modifier.shimmer() else Modifier),
        )
        Text(
            text = category.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
