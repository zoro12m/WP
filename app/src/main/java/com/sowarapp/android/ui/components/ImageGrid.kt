package com.sowarapp.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sowarapp.android.data.model.SowarImage

/**
 * A responsive "masonry" grid: 2 columns on a phone, more on tablets or in landscape,
 * because each column is at least 150dp wide. Cards keep their photo's shape.
 *
 * [header] adds full-width content (like a title or a hero picture) above the photos.
 */
@Composable
fun ImageGrid(
    images: List<SowarImage>,
    onImageClick: (SowarImage) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (LazyStaggeredGridScope.() -> Unit)? = null,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = modifier,
    ) {
        header?.invoke(this)
        imageItems(images, onImageClick)
    }
}

/** Adds one card per image to any staggered grid. */
fun LazyStaggeredGridScope.imageItems(
    images: List<SowarImage>,
    onImageClick: (SowarImage) -> Unit,
) {
    items(images, key = { it.id }) { image ->
        ImageCard(image = image, onClick = { onImageClick(image) })
    }
}
