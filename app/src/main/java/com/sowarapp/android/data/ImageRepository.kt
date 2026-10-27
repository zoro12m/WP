package com.sowarapp.android.data

import com.sowarapp.android.data.model.Catalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The single place screens get categories and images from.
 *
 * On start it shows the catalog saved on the phone right away, then downloads the newest one
 * in the background and swaps it in. Screens observe [catalog], so they update by themselves.
 */
class ImageRepository(
    private val dataSource: CatalogDataSource,
    private val scope: CoroutineScope,
) {
    private val _catalog = MutableStateFlow<Catalog?>(null)

    /** Null while the first catalog is being read (a split second), then always a catalog. */
    val catalog: StateFlow<Catalog?> = _catalog.asStateFlow()

    fun start() {
        scope.launch {
            dataSource.loadOffline()?.let { _catalog.value = it }
            val downloaded = dataSource.download()
            _catalog.value = downloaded ?: _catalog.value ?: Catalog.EMPTY
        }
    }
}
