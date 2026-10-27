package com.sowarapp.android.ui.home

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage

class HomeViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    val categories: List<Category> = imageRepository.getCategories()
    val popularImages: List<SowarImage> = imageRepository.getPopularImages()

    /** Image count per category id, shown on each category card. */
    val imageCounts: Map<String, Int> =
        categories.associate { it.id to imageRepository.getImagesInCategory(it.id).size }

    /** What the user typed in the search field. */
    var searchQuery by mutableStateOf("")
        private set

    /** Recalculated automatically whenever [searchQuery] changes. */
    val searchResults: List<SowarImage> by derivedStateOf { imageRepository.search(searchQuery) }

    val isSearching: Boolean by derivedStateOf { searchQuery.isNotBlank() }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }
}
