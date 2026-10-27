package com.sowarapp.android.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sowarapp.android.SowarApplication
import com.sowarapp.android.ui.category.CategoryViewModel
import com.sowarapp.android.ui.details.ImageDetailsViewModel
import com.sowarapp.android.ui.favorites.FavoritesViewModel
import com.sowarapp.android.ui.home.HomeViewModel

/** Tells Compose how to build each ViewModel with the repositories it needs. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(sowarApplication().container.imageRepository)
        }
        initializer {
            CategoryViewModel(
                savedStateHandle = createSavedStateHandle(),
                imageRepository = sowarApplication().container.imageRepository,
            )
        }
        initializer {
            val container = sowarApplication().container
            ImageDetailsViewModel(
                savedStateHandle = createSavedStateHandle(),
                imageRepository = container.imageRepository,
                favoritesRepository = container.favoritesRepository,
                imageExporter = container.imageExporter,
            )
        }
        initializer {
            val container = sowarApplication().container
            FavoritesViewModel(container.imageRepository, container.favoritesRepository)
        }
    }
}

private fun CreationExtras.sowarApplication(): SowarApplication =
    this[APPLICATION_KEY] as SowarApplication
