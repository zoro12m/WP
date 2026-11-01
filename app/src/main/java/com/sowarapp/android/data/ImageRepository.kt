package com.sowarapp.android.data

import android.os.SystemClock
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
 *
 * Android often keeps the app alive in the background, so "opening it again" doesn't run
 * [start] again. [refreshIfStale] (each time the app comes to the front) and [refresh]
 * (pull down on the home screen) make sure photos added on the admin page still show up.
 */
class ImageRepository(
    private val dataSource: CatalogDataSource,
    private val scope: CoroutineScope,
) {
    private val _catalog = MutableStateFlow<Catalog?>(null)

    /** Null while the first catalog is being read (a split second), then always a catalog. */
    val catalog: StateFlow<Catalog?> = _catalog.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    /** True while a download is running; the home screen shows its pull-to-refresh spinner. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastRefreshFailed = MutableStateFlow(false)

    /**
     * True when the last check for new photos failed (no internet, or too slow). The app keeps
     * showing the photos it has, and the home screen says why new ones may be missing.
     */
    val lastRefreshFailed: StateFlow<Boolean> = _lastRefreshFailed.asStateFlow()

    /** When the last download finished (SystemClock.elapsedRealtime), or 0 if never. */
    private var lastDownloadAt = 0L

    fun start() {
        _isRefreshing.value = true
        scope.launch {
            try {
                dataSource.loadOffline()?.let { _catalog.value = it }
                download()
            } finally {
                if (_catalog.value == null) _catalog.value = Catalog.EMPTY
                _isRefreshing.value = false
            }
        }
    }

    /** Downloads the newest catalog now (pull to refresh). Does nothing if one is already running. */
    fun refresh() {
        // Checked and set in one step, so two quick calls never start two downloads.
        if (!_isRefreshing.compareAndSet(expect = false, update = true)) return
        scope.launch {
            try {
                download()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Downloads the newest catalog unless one was downloaded in the last [STALE_AFTER_MILLIS]. */
    fun refreshIfStale() {
        if (SystemClock.elapsedRealtime() - lastDownloadAt >= STALE_AFTER_MILLIS) refresh()
    }

    private suspend fun download() {
        when (val result = dataSource.download()) {
            is CatalogDataSource.Download.New -> _catalog.value = result.catalog
            CatalogDataSource.Download.Unchanged -> Unit  // what we show is already the newest
            CatalogDataSource.Download.Failed -> {
                // Keep showing what we have, and try again next time.
                _lastRefreshFailed.value = true
                return
            }
        }
        _lastRefreshFailed.value = false
        lastDownloadAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        /** Coming back to the app within this time doesn't download again. */
        const val STALE_AFTER_MILLIS = 60_000L
    }
}
