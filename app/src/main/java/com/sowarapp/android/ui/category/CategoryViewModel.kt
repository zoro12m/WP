package com.sowarapp.android.ui.category

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.Catalog
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.navigation.Routes
import kotlinx.coroutines.launch

class CategoryViewModel(
    savedStateHandle: SavedStateHandle,
    imageRepository: ImageRepository,
) : ViewModel() {

    // The category id comes from the route "category/{categoryId}".
    private val categoryId: String = checkNotNull(savedStateHandle[Routes.ARG_CATEGORY_ID])

    private var catalog by mutableStateOf<Catalog?>(null)

    init {
        viewModelScope.launch { imageRepository.catalog.collect { catalog = it } }
    }

    /** False for the split second before the catalog is read. */
    val isLoaded: Boolean by derivedStateOf { catalog != null }

    /** Null if the id doesn't match any category. */
    val category: Category? by derivedStateOf { catalog?.getCategory(categoryId) }
    val images: List<SowarImage> by derivedStateOf { catalog?.getImagesInCategory(categoryId).orEmpty() }
}
