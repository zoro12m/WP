package com.sowarapp.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

/**
 * Stores the ids of favorite images on the phone (Jetpack DataStore),
 * so favorites survive closing the app.
 */
class FavoritesRepository(context: Context) {

    private val dataStore = context.favoritesDataStore

    /** Emits the current favorite ids, and again every time they change. */
    val favoriteIds: Flow<Set<String>> = dataStore.data
        .catch { error ->
            // A corrupted or unreadable file should not crash the app.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[FAVORITE_IDS] ?: emptySet() }

    /** Adds the image if it isn't a favorite, removes it if it is. Returns the new state. */
    suspend fun toggleFavorite(imageId: String): Boolean {
        var isNowFavorite = false
        dataStore.edit { preferences ->
            val current = preferences[FAVORITE_IDS] ?: emptySet()
            isNowFavorite = imageId !in current
            preferences[FAVORITE_IDS] = if (isNowFavorite) current + imageId else current - imageId
        }
        return isNowFavorite
    }

    private companion object {
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_image_ids")
    }
}
