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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    /** Null until the catalog is ready; replaced when a newer one is downloaded. */
    private var catalog by mutableStateOf<Catalog?>(null)

    /** True while pull to refresh is downloading the newest photos. */
    var isRefreshing by mutableStateOf(false)
        private set

    /** True when the last check for new photos failed; the home screen shows a short note. */
    var lastRefreshFailed by mutableStateOf(false)
        private set

    /** Pull down on the home screen: get the newest photos now. */
    fun refresh() = imageRepository.refresh()

    // Plain getters, not derivedStateOf: this ViewModel is created during composition, and
    // derived state made there could crash the app at startup ("Reading a state that was
    // created after the snapshot was taken"). Compose still tracks the catalog read inside them.

    /** False for the split second before the catalog is read. */
    val isLoaded: Boolean get() = catalog != null

    /** Sections that have photos. New sections made on the admin page stay hidden until they get one. */
    val categories: List<Category>
        get() {
            val current = catalog ?: return emptyList()
            return current.categories.filter { current.getImagesInCategory(it.id).isNotEmpty() }
        }
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

    /** What the user typed in the search field. Updated on every key, so typing stays instant. */
    var searchQuery by mutableStateOf("")
        private set

    /** Photos matching [searchQuery], worked out [SEARCH_DELAY_MILLIS] after typing pauses. */
    var searchResults by mutableStateOf<List<SowarImage>>(emptyList())
        private set

    /** True between a key press and its results, so the screen doesn't flash "no results". */
    var isSearchPending by mutableStateOf(false)
        private set

    val isSearching: Boolean get() = searchQuery.isNotBlank()

    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearchPending = false
            return
        }
        isSearchPending = true
        // Searching thousands of photos on every letter would make cheap phones lag,
        // so wait until the user pauses typing.
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MILLIS)
            search(query)
        }
    }

    private suspend fun search(query: String) {
        val current = catalog ?: return
        val results = withContext(Dispatchers.Default) { current.search(query) }
        if (query == searchQuery) {
            searchResults = results
            isSearchPending = false
        }
    }

    // Last in the class on purpose: viewModelScope starts collecting right away, so every
    // property above (searchQuery included) must already exist when these blocks run.
    init {
        viewModelScope.launch {
            imageRepository.catalog.collect {
                catalog = it
                if (isSearching) search(searchQuery)
            }
        }
        viewModelScope.launch { imageRepository.isRefreshing.collect { isRefreshing = it } }
        viewModelScope.launch { imageRepository.lastRefreshFailed.collect { lastRefreshFailed = it } }
    }

    private companion object {
        const val SEARCH_DELAY_MILLIS = 300L
    }
}
