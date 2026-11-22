package com.nexus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.core.datastore.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Separate from the session file: signing out clears the session, not the device preferences
private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_preferences")

/** Preferences of this device that outlive the session, such as the theme. */
@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.preferencesStore
    private val keyThemeMode = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[keyThemeMode]
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[keyThemeMode] = mode.name }
    }
}
