package com.sowarapp.android

import android.content.Context
import com.sowarapp.android.data.FavoritesRepository
import com.sowarapp.android.data.ImageExporter
import com.sowarapp.android.data.ImageRepository
import com.sowarapp.android.data.LocalImageDataSource

/**
 * Creates the app's shared objects once. This is "manual dependency injection":
 * simple enough for a small app, and easy to replace with Hilt later if needed.
 */
class AppContainer(context: Context) {
    val imageRepository = ImageRepository(LocalImageDataSource())
    val favoritesRepository = FavoritesRepository(context)
    val imageExporter = ImageExporter(context)
}
