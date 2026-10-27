package com.sowarapp.android

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.sowarapp.android.data.CatalogDataSource
import com.sowarapp.android.data.FavoritesRepository
import com.sowarapp.android.data.ImageExporter
import com.sowarapp.android.data.ImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * Creates the app's shared objects once. This is "manual dependency injection":
 * simple enough for a small app, and easy to replace with Hilt later if needed.
 */
class AppContainer(context: Context) {

    /** Lives as long as the app, for work that must not stop when a screen closes. */
    private val appScope = CoroutineScope(SupervisorJob())

    /** One HTTP client for everything. Image hosts like Wikimedia ask apps to identify themselves. */
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    /** Downloads, caches (memory + disk) and decodes every picture in the app. */
    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { httpClient })) }
        .crossfade(true)
        .build()

    val imageRepository = ImageRepository(
        dataSource = CatalogDataSource(context, httpClient, BuildConfig.CATALOG_URL),
        scope = appScope,
    )
    val favoritesRepository = FavoritesRepository(context)
    val imageExporter = ImageExporter(context, imageLoader)

    private companion object {
        const val USER_AGENT =
            "SowarApp/${BuildConfig.VERSION_NAME} (Android; https://github.com/zoro12m/WP)"
    }
}
