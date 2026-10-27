package com.sowarapp.android.ui.category

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.BackTopBar
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.ImageGrid

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: CategoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val category = viewModel.category
    Scaffold(
        topBar = {
            BackTopBar(
                title = category?.title ?: stringResource(R.string.category_not_found),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        if (viewModel.images.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.category_empty_title),
                message = stringResource(R.string.category_empty_message),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            ImageGrid(
                images = viewModel.images,
                onImageClick = { image -> onImageClick(image.id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
