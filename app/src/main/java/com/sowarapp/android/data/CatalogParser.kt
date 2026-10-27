package com.sowarapp.android.data

import com.sowarapp.android.data.model.Catalog
import com.sowarapp.android.data.model.CatalogFile
import kotlinx.serialization.json.Json

/** Turns the text of catalog.json into a [Catalog]. Kept separate so unit tests can use it. */
object CatalogParser {

    private val json = Json {
        // New fields added to the catalog later must not break this version of the app.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Throws if the text isn't a valid catalog, so a broken download never replaces a good one. */
    fun parse(text: String): Catalog = Catalog(json.decodeFromString(CatalogFile.serializer(), text))
}
