package com.sowarapp.android.ui.details

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sowarapp.android.R
import com.sowarapp.android.data.FavoritesRepository
import com.sowarapp.android.data.ImageExporter
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.model.Catalog
import com.sowarapp.android.data.model.SowarImage
import com.sowarapp.android.ui.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-time things the screen must do, like showing a message or opening the share sheet. */
sealed interface DetailsEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : DetailsEvent
    data class OpenShareSheet(val uri: Uri, val caption: String) : DetailsEvent
}

class ImageDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    imageRepository: ImageRepository,
    private val favoritesRepository: FavoritesRepository,
    private val imageExporter: ImageExporter,
) : ViewModel() {

    /** The image that was tapped to open this screen. */
    private val openedImageId: String = checkNotNull(savedStateHandle[Routes.ARG_IMAGE_ID])

    private var catalog by mutableStateOf<Catalog?>(null)

    init {
        viewModelScope.launch { imageRepository.catalog.collect { catalog = it } }
    }

    // Plain getters, not derivedStateOf: this ViewModel is created during composition, and
    // derived state made there could crash the app at startup ("Reading a state that was
    // created after the snapshot was taken"). Compose still tracks the catalog read inside them.

    /** False for the split second before the catalog is read. */
    val isLoaded: Boolean get() = catalog != null

    /** Every image of the opened image's category, so the user can swipe to the next one. */
    val images: List<SowarImage>
        get() {
            val current = catalog ?: return emptyList()
            val opened = current.getImage(openedImageId) ?: return emptyList()
            return current.getImagesInCategory(opened.categoryId).ifEmpty { listOf(opened) }
        }

    /** Where the opened image is in [images], to start the swiping there. */
    val initialPage: Int get() = images.indexOfFirst { it.id == openedImageId }.coerceAtLeast(0)

    val categoryTitle: String?
        get() = images.firstOrNull()?.let { catalog?.getCategory(it.categoryId)?.title }

    /** The image currently on screen (changes when the user swipes). */
    private var currentImageId by mutableStateOf(openedImageId)

    /** Null if the id doesn't match any image. */
    val image: SowarImage? get() = catalog?.getImage(currentImageId)

    fun onImageShown(imageId: String) {
        currentImageId = imageId
    }

    val isFavorite: StateFlow<Boolean> = combine(favoritesRepository.favoriteIds, snapshotFlow { currentImageId }) { ids, id ->
        id in ids
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    private val _isBusy = MutableStateFlow(false)
    /** True while a download or share is being prepared, to disable the buttons. */
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = Channel<DetailsEvent>(Channel.BUFFERED)
    val events: Flow<DetailsEvent> = _events.receiveAsFlow()

    fun toggleFavorite() {
        viewModelScope.launch {
            val nowFavorite = favoritesRepository.toggleFavorite(currentImageId)
            val message = if (nowFavorite) R.string.message_added_favorite else R.string.message_removed_favorite
            _events.send(DetailsEvent.ShowMessage(message))
        }
    }

    fun download() {
        val image = image ?: return
        runExport(failureMessage = R.string.message_save_failed) {
            imageExporter.saveToGallery(image)
            _events.send(DetailsEvent.ShowMessage(R.string.message_saved))
        }
    }

    fun share() {
        val image = image ?: return
        runExport(failureMessage = R.string.message_share_failed) {
            val uri = imageExporter.createShareableUri(image)
            _events.send(DetailsEvent.OpenShareSheet(uri, image.caption))
        }
    }

    fun onStoragePermissionDenied() {
        viewModelScope.launch { _events.send(DetailsEvent.ShowMessage(R.string.message_permission_denied)) }
    }

    /** Runs a download/share job, ignoring taps while busy and turning errors into a message. */
    private fun runExport(@StringRes failureMessage: Int, block: suspend () -> Unit) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.send(DetailsEvent.ShowMessage(failureMessage))
            } finally {
                _isBusy.value = false
            }
        }
    }
}
