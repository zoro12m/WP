package com.sowarapp.android.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.navigation.Routes

class CategoryViewModel(
    savedStateHandle: SavedStateHandle,
    imageRepository: ImageRepository,
) : ViewModel() {

    // The category id comes from the route "category/{categoryId}".
    private val categoryId: String = checkNotNull(savedStateHandle[Routes.ARG_CATEGORY_ID])

    /** Null if the id doesn't match any category. */
    val category: Category? = imageRepository.getCategory(categoryId)
    val images: List<SowarImage> = imageRepository.getImagesInCategory(categoryId)
}
