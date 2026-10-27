package com.sowarapp.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sowarapp.android.R
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.AppViewModelProvider
import com.sowarapp.android.ui.components.CategoryCard
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.ImageCard

@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val focusManager = LocalFocusManager.current

    // One scrolling grid for the whole page. Headers and the popular row take the full width
    // (span = maxLineSpan); category cards fill 2 columns on phones, more on wider screens.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        fullWidthItem {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        fullWidthItem {
            SearchField(
                query = viewModel.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = { focusManager.clearFocus() },
            )
        }

        if (viewModel.isSearching) {
            searchResults(viewModel.searchResults, onImageClick)
        } else {
            categoriesSection(viewModel.categories, viewModel.imageCounts, onCategoryClick)
            popularSection(viewModel.popularImages, onImageClick)
        }
    }
}

private fun LazyGridScope.fullWidthItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.sectionTitle(titleRes: Int) {
    fullWidthItem {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )
    }
}

private fun LazyGridScope.categoriesSection(
    categories: List<Category>,
    imageCounts: Map<String, Int>,
    onCategoryClick: (String) -> Unit,
) {
    sectionTitle(R.string.section_categories)
    items(categories, key = { it.id }) { category ->
        CategoryCard(
            category = category,
            imageCount = imageCounts[category.id] ?: 0,
            onClick = { onCategoryClick(category.id) },
        )
    }
}

private fun LazyGridScope.popularSection(
    images: List<SowarImage>,
    onImageClick: (String) -> Unit,
) {
    sectionTitle(R.string.section_popular)
    fullWidthItem {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(images, key = { it.id }) { image ->
                ImageCard(
                    image = image,
                    onClick = { onImageClick(image.id) },
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

private fun LazyGridScope.searchResults(
    results: List<SowarImage>,
    onImageClick: (String) -> Unit,
) {
    sectionTitle(R.string.section_search_results)
    if (results.isEmpty()) {
        fullWidthItem {
            EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.search_empty_title),
                message = stringResource(R.string.search_empty_message),
            )
        }
    } else {
        items(results, key = { it.id }) { image ->
            ImageCard(image = image, onClick = { onImageClick(image.id) })
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier.fillMaxWidth(),
    )
}
