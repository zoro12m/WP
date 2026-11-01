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
import java.util.zip.GZIPInputStream

/**
 * Gets the catalog from three places, best first:
 * 1. the internet ([catalogUrl]), which has the newest images. It downloads the gzip copy
 *    (about 10 times smaller), and asks the server to skip the download entirely when the
 *    catalog hasn't changed since last time (ETag), so most app starts download almost nothing;
 * 2. the copy saved on the phone after the last successful download;
 * 3. the copy bundled inside the APK, so even the very first launch has something to show.
 */
class CatalogDataSource(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val catalogUrl: String,
) {
    private val savedFile = File(context.filesDir, SAVED_FILE_NAME)

    /** The server's version tag (ETag) of the saved copy, sent back so it can answer "unchanged". */
    private val etagFile = File(context.filesDir, "$SAVED_FILE_NAME.etag")

    /** What a download attempt found. */
    sealed interface Download {
        data class New(val catalog: Catalog) : Download
        data object Unchanged : Download
        data object Failed : Download
    }

    /** The saved copy if there is one, otherwise the bundled one. Null only if both are missing or broken. */
    suspend fun loadOffline(): Catalog? = withContext(Dispatchers.IO) {
        readSaved() ?: readBundled()
    }

    /** Downloads the newest catalog and saves it on the phone. [Download.Failed] if there's no internet or the file is broken. */
    suspend fun download(): Download = withContext(Dispatchers.IO) {
        try {
            // Until the gzip copy exists online (it's made the first time the catalog is saved), use the plain one.
            fetch("$catalogUrl.gz", gzipped = true) ?: fetch(catalogUrl, gzipped = false) ?: error("HTTP 404")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not download the catalog", error)
            Download.Failed
        }
    }

    /** Null if [url] doesn't exist (404), so the caller can try another address. */
    private fun fetch(url: String, gzipped: Boolean): Download? {
        val request = Request.Builder().url(url)
        val etag = if (savedFile.exists() && etagFile.exists()) etagFile.readText() else null
        if (etag != null) request.header("If-None-Match", etag)
        return httpClient.newCall(request.build()).execute().use { response ->
            when {
                response.code == 304 -> Download.Unchanged
                response.code == 404 -> null
                !response.isSuccessful -> error("HTTP ${response.code}")
                else -> {
                    val body = response.body ?: error("Empty response")
                    val text = if (gzipped) {
                        GZIPInputStream(body.byteStream()).bufferedReader().use { it.readText() }
                    } else {
                        body.string()
                    }
                    val catalog = CatalogParser.parse(text)
                    save(text)
                    // Both addresses have their own ETag, so keep only the one this copy came from.
                    val newEtag = response.header("ETag")
                    if (newEtag != null) etagFile.writeText(newEtag) else etagFile.delete()
                    Download.New(catalog)
                }
            }
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
