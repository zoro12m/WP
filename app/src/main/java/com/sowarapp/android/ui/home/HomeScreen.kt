package com.sowarapp.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.sowarapp.android.ui.components.CategoryAvatar
import com.sowarapp.android.ui.components.EmptyState
import com.sowarapp.android.ui.components.FeaturedImageCard
import com.sowarapp.android.ui.components.SectionHeader
import com.sowarapp.android.ui.components.fullBleed
import com.sowarapp.android.ui.components.imageItems
import com.sowarapp.android.ui.theme.LocalBrandColors
import java.util.Calendar

private val SidePadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val focusManager = LocalFocusManager.current
    val openImage: (SowarImage) -> Unit = { image -> onImageClick(image.id) }

    // Pull down to get the newest photos (for example right after adding some on the admin page).
    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        // One scrolling "masonry" grid for the whole page. Headers, the category row and the
        // featured picture take the full width; photo cards fill 2 columns on phones, more on tablets.
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(start = SidePadding, end = SidePadding, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidthItem {
                HomeHeader(
                    query = viewModel.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier.fullBleed(SidePadding),
                )
            }

            if (!viewModel.isLoaded) {
                // The catalog is being read; this takes a split second.
            } else if (viewModel.categories.isEmpty()) {
                fullWidthItem {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.catalog_empty_title),
                        message = stringResource(R.string.catalog_empty_message),
                    )
                }
            } else if (viewModel.isSearching) {
                sectionTitle(R.string.section_search_results)
                if (viewModel.searchResults.isEmpty()) {
                    fullWidthItem {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.search_empty_title),
                            message = stringResource(R.string.search_empty_message),
                        )
                    }
                } else {
                    imageItems(viewModel.searchResults, openImage)
                }
            } else {
                sectionTitle(R.string.section_categories)
                fullWidthItem {
                    CategoriesRow(
                        categories = viewModel.categories,
                        imageCounts = viewModel.imageCounts,
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier.fullBleed(SidePadding),
                    )
                }
                viewModel.featuredImage?.let { featured ->
                    sectionTitle(R.string.section_featured)
                    fullWidthItem { FeaturedImageCard(image = featured, onClick = { openImage(featured) }) }
                }
                if (viewModel.popularImages.isNotEmpty()) {
                    sectionTitle(R.string.section_popular)
                    imageItems(viewModel.popularImages, openImage)
                }
                if (viewModel.moreImages.isNotEmpty()) {
                    sectionTitle(R.string.section_more)
                    imageItems(viewModel.moreImages, openImage)
                }
            }
        }
    }
}

private fun LazyStaggeredGridScope.fullWidthItem(content: @Composable () -> Unit) {
    item(span = StaggeredGridItemSpan.FullLine) { content() }
}

private fun LazyStaggeredGridScope.sectionTitle(titleRes: Int) {
    fullWidthItem { SectionHeader(stringResource(titleRes)) }
}

/** Rose gradient header with a greeting, the app name and the search field. */
@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Before noon say "good morning", after it "good evening".
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 4..11) R.string.greeting_morning else R.string.greeting_evening
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                LocalBrandColors.current.headerBrush,
                RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp),
    ) {
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        SearchField(query = query, onQueryChange = onQueryChange, onSearch = onSearch)
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colors.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerLowest,
            unfocusedContainerColor = colors.surfaceContainerLowest,
            // No underline: the white pill shape is enough.
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoriesRow(
    categories: List<Category>,
    imageCounts: Map<String, Int>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = SidePadding - 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryAvatar(
                category = category,
                imageCount = imageCounts[category.id] ?: 0,
                onClick = { onCategoryClick(category.id) },
            )
        }
    }
}
