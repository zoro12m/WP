package com.sowarapp.android.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.ImageGrid

@Composable
fun FavoritesScreen(
    onImageClick: (String) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val favorites by viewModel.favoriteImages.collectAsStateWithLifecycle()
    val images = favorites

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        when {
            images == null -> Unit // Still loading (takes a split second); show nothing to avoid flicker.
            images.isEmpty() -> {
                FavoritesTitle(count = 0)
                EmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    title = stringResource(R.string.favorites_empty_title),
                    message = stringResource(R.string.favorites_empty_message),
                )
            }
            else -> ImageGrid(
                images = images,
                onImageClick = { image -> onImageClick(image.id) },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
                header = {
                    item(span = StaggeredGridItemSpan.FullLine) { FavoritesTitle(count = images.size) }
                },
            )
        }
    }
}

@Composable
private fun FavoritesTitle(count: Int) {
    Column(modifier = Modifier.padding(horizontal = if (count == 0) 16.dp else 0.dp, vertical = 16.dp)) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (count > 0) {
            Text(
                text = stringResource(R.string.category_image_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
