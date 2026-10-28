package com.sowarapp.android.ui.home

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
import java.util.Calendar

class HomeViewModel(imageRepository: ImageRepository) : ViewModel() {

    /** Null until the catalog is ready; replaced when a newer one is downloaded. */
    private var catalog by mutableStateOf<Catalog?>(null)

    init {
        viewModelScope.launch { imageRepository.catalog.collect { catalog = it } }
    }

    // Plain getters, not derivedStateOf: this ViewModel is created during composition, and
    // derived state made there could crash the app at startup ("Reading a state that was
    // created after the snapshot was taken"). Compose still tracks the catalog read inside them.

    /** False for the split second before the catalog is read. */
    val isLoaded: Boolean get() = catalog != null

    val categories: List<Category> get() = catalog?.categories.orEmpty()
    val popularImages: List<SowarImage> get() = catalog?.getPopularImages().orEmpty()

    /** "Picture of the day": a popular image that changes every day. */
    val featuredImage: SowarImage?
        get() {
            val candidates = popularImages.ifEmpty { catalog?.items.orEmpty() }
            if (candidates.isEmpty()) return null
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            return candidates[dayOfYear % candidates.size]
        }

    /** Everything else, for the "discover more" grid under the popular images. */
    val moreImages: List<SowarImage>
        get() {
            val shown = popularImages.map { it.id }.toSet() + listOfNotNull(featuredImage?.id)
            return catalog?.items.orEmpty().filter { it.id !in shown }
        }

    /** Image count per category id, shown on each category card. */
    val imageCounts: Map<String, Int>
        get() {
            val current = catalog ?: return emptyMap()
            return current.categories.associate { it.id to current.getImagesInCategory(it.id).size }
        }

    /** What the user typed in the search field. */
    var searchQuery by mutableStateOf("")
        private set

    /** Recalculated automatically whenever [searchQuery] or the catalog changes. */
    val searchResults: List<SowarImage> get() = catalog?.search(searchQuery).orEmpty()

    val isSearching: Boolean get() = searchQuery.isNotBlank()

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }
}
