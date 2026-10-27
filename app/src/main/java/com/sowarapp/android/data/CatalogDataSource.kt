package com.sowarapp.android.data

import android.content.Context
import android.util.Log
import com.sowarapp.android.data.model.Catalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Gets the catalog from three places, best first:
 * 1. the internet ([catalogUrl]), which has the newest images;
 * 2. the copy saved on the phone after the last successful download;
 * 3. the copy bundled inside the APK, so even the very first launch has something to show.
 */
class CatalogDataSource(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val catalogUrl: String,
) {
    private val savedFile = File(context.filesDir, SAVED_FILE_NAME)

    /** The saved copy if there is one, otherwise the bundled one. Null only if both are missing or broken. */
    suspend fun loadOffline(): Catalog? = withContext(Dispatchers.IO) {
        readSaved() ?: readBundled()
    }

    /** Downloads the newest catalog and saves it on the phone. Null if there's no internet or the file is broken. */
    suspend fun download(): Catalog? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(catalogUrl).build()
            val text = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string() ?: error("Empty response")
            }
            val catalog = CatalogParser.parse(text)
            save(text)
            catalog
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not download the catalog", error)
            null
        }
    }

    private fun readSaved(): Catalog? = tryParse("saved") {
        if (savedFile.exists()) savedFile.readText() else null
    }

    private fun readBundled(): Catalog? = tryParse("bundled") {
        context.assets.open(SAVED_FILE_NAME).bufferedReader().use { it.readText() }
    }

    private fun tryParse(label: String, readText: () -> String?): Catalog? =
        try {
            readText()?.let(CatalogParser::parse)
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the $label catalog", error)
            null
        }

    /** Writes to a temporary file first, so a crash mid-write never leaves a half-written catalog. */
    private fun save(text: String) {
        val temp = File(context.filesDir, "$SAVED_FILE_NAME.tmp")
        temp.writeText(text)
        if (!temp.renameTo(savedFile)) {
            savedFile.writeText(text)
            temp.delete()
        }
    }

    private companion object {
        const val TAG = "CatalogDataSource"
        const val SAVED_FILE_NAME = "catalog.json"
    }
}
