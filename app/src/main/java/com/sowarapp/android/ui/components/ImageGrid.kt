package com.sowarapp.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sowarapp.android.data.model.SowarImage

/**
 * A responsive grid: 2 columns on a phone, more on tablets or in landscape,
 * because each column is at least 150dp wide.
 */
@Composable
fun ImageGrid(
    images: List<SowarImage>,
    onImageClick: (SowarImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(images, key = { it.id }) { image ->
            ImageCard(image = image, onClick = { onImageClick(image) })
        }
    }
}
