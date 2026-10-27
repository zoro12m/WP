package com.sowarapp.android.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sowarapp.android.data.FavoritesRepository
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.SowarImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    imageRepository: ImageRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {

    /** Null while favorites and the catalog are still loading; then the list (possibly empty). */
    val favoriteImages: StateFlow<List<SowarImage>?> = combine(
        imageRepository.catalog.filterNotNull(),
        favoritesRepository.favoriteIds,
    ) { catalog, ids -> catalog.getImagesByIds(ids) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
}
