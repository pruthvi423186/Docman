package com.example.docmanager.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_EMAIL = stringPreferencesKey("saved_email")
        val KEY_DISPLAY_NAME = stringPreferencesKey("saved_display_name")
        val KEY_PHOTO_URI = stringPreferencesKey("saved_photo_uri")
        val KEY_THEME = stringPreferencesKey("theme_preference") // "light", "dark", "system"
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val KEY_ALWAYS_OPEN_WITH_DEFAULT = booleanPreferencesKey("always_open_with_default")
    }

    val savedEmailFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_EMAIL]
    }
    
    val savedDisplayNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_DISPLAY_NAME]
    }

    val savedPhotoUriFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_PHOTO_URI]
    }

    val themePreferenceFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "system"
    }

    val recentSearchesFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|")
    }

    val alwaysOpenWithDefaultFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ALWAYS_OPEN_WITH_DEFAULT] ?: false
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        dataStore.edit { preferences ->
            val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
            val currentList = if (raw.isBlank()) emptyList() else raw.split("|")
            val newList = (listOf(trimmed) + currentList.filter { it != trimmed }).take(5)
            preferences[KEY_RECENT_SEARCHES] = newList.joinToString("|")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        dataStore.edit { preferences ->
            val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
            val currentList = if (raw.isBlank()) emptyList() else raw.split("|")
            val newList = currentList.filter { it != query }
            preferences[KEY_RECENT_SEARCHES] = newList.joinToString("|")
        }
    }

    suspend fun saveAuthSession(email: String, displayName: String, photoUri: String?) {
        dataStore.edit { preferences ->
            preferences[KEY_EMAIL] = email
            preferences[KEY_DISPLAY_NAME] = displayName
            if (photoUri != null) {
                preferences[KEY_PHOTO_URI] = photoUri
            } else {
                preferences.remove(KEY_PHOTO_URI)
            }
        }
    }

    suspend fun savePhotoUri(photoUri: String?) {
        dataStore.edit { preferences ->
            if (photoUri != null) {
                preferences[KEY_PHOTO_URI] = photoUri
            } else {
                preferences.remove(KEY_PHOTO_URI)
            }
        }
    }

    suspend fun clearAuthSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_EMAIL)
            preferences.remove(KEY_DISPLAY_NAME)
            preferences.remove(KEY_PHOTO_URI)
        }
    }

    suspend fun setThemePreference(theme: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun setAlwaysOpenWithDefault(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ALWAYS_OPEN_WITH_DEFAULT] = enabled
        }
    }
}
