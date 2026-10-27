package com.sowarapp.android.ui.home

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.Catalog
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage
import kotlinx.coroutines.launch

class HomeViewModel(imageRepository: ImageRepository) : ViewModel() {

    /** Null until the catalog is ready; replaced when a newer one is downloaded. */
    private var catalog by mutableStateOf<Catalog?>(null)

    init {
        viewModelScope.launch { imageRepository.catalog.collect { catalog = it } }
    }

    /** False for the split second before the catalog is read. */
    val isLoaded: Boolean by derivedStateOf { catalog != null }

    val categories: List<Category> by derivedStateOf { catalog?.categories.orEmpty() }
    val popularImages: List<SowarImage> by derivedStateOf { catalog?.getPopularImages().orEmpty() }

    /** Image count per category id, shown on each category card. */
    val imageCounts: Map<String, Int> by derivedStateOf {
        val current = catalog ?: return@derivedStateOf emptyMap()
        current.categories.associate { it.id to current.getImagesInCategory(it.id).size }
    }

    /** What the user typed in the search field. */
    var searchQuery by mutableStateOf("")
        private set

    /** Recalculated automatically whenever [searchQuery] or the catalog changes. */
    val searchResults: List<SowarImage> by derivedStateOf { catalog?.search(searchQuery).orEmpty() }

    val isSearching: Boolean by derivedStateOf { searchQuery.isNotBlank() }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }
}
