package com.sowarapp.android.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sowarapp.android.R
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.ImageGrid
import com.sowarapp.android.ui.components.RoundIconButton
import com.sowarapp.android.ui.components.fullBleed
import com.sowarapp.android.ui.theme.LocalBrandColors

private val SidePadding = 16.dp

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: CategoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val category = viewModel.category
    Box(modifier = Modifier.fillMaxSize()) {
        if (!viewModel.isLoaded) {
            // The catalog is being read; this takes a split second, so show nothing to avoid flicker.
        } else if (category == null || viewModel.images.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Info,
                title = stringResource(
                    if (category == null) R.string.category_not_found else R.string.category_empty_title
                ),
                message = if (category == null) "" else stringResource(R.string.category_empty_message),
                modifier = Modifier
                    .align(Alignment.Center)
                    .statusBarsPadding(),
            )
        } else {
            ImageGrid(
                images = viewModel.images,
                onImageClick = { image -> onImageClick(image.id) },
                contentPadding = PaddingValues(start = SidePadding, end = SidePadding, bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
                header = {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        CategoryHero(
                            category = category,
                            imageCount = viewModel.images.size,
                            modifier = Modifier
                                .fullBleed(SidePadding)
                                .padding(bottom = 4.dp),
                        )
                    }
                },
            )
        }

        // Stays on top while the photos scroll under it.
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp),
        )
    }
}

/** Big cover photo with the category name, at the top of the list. */
@Composable
private fun CategoryHero(
    category: Category,
    imageCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(LocalBrandColors.current.headerBrush),
    ) {
        AsyncImage(
            model = category.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Dark at the top (for the status bar and back button) and bottom (for the title).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.35f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.category_image_count, imageCount),
                style = MaterialTheme.typography.titleSmall,
                color = LocalBrandColors.current.gold,
            )
        }
    }
}
