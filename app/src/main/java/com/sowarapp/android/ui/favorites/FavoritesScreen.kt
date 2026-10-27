package com.sowarapp.android.ui.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.ImageGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onImageClick: (String) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val favorites by viewModel.favoriteImages.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.favorites_title)) }) },
    ) { innerPadding ->
        val images = favorites
        when {
            images == null -> Unit // Still loading (takes a split second); show nothing to avoid flicker.
            images.isEmpty() -> EmptyState(
                icon = Icons.Filled.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                message = stringResource(R.string.favorites_empty_message),
                modifier = Modifier.padding(innerPadding),
            )
            else -> ImageGrid(
                images = images,
                onImageClick = { image -> onImageClick(image.id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
