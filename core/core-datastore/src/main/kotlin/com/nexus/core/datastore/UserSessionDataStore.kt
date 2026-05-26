package com.nexus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.core.datastore.model.SessionKeys
import com.nexus.core.datastore.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_session")

@Singleton
class UserSessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    private val keyUid = stringPreferencesKey(SessionKeys.KEY_UID)
    private val keyEmail = stringPreferencesKey(SessionKeys.KEY_EMAIL)
    private val keyDisplayName = stringPreferencesKey(SessionKeys.KEY_DISPLAY_NAME)
    private val keyPhotoUrl = stringPreferencesKey(SessionKeys.KEY_PHOTO_URL)
    private val keyIsLoggedIn = booleanPreferencesKey(SessionKeys.KEY_IS_LOGGED_IN)

    val sessionFlow: Flow<UserSession?> = store.data.map { prefs ->
        val isLoggedIn = prefs[keyIsLoggedIn] ?: false
        if (!isLoggedIn) return@map null
        UserSession(
            uid = prefs[keyUid].orEmpty(),
            email = prefs[keyEmail].orEmpty(),
            displayName = prefs[keyDisplayName].orEmpty(),
            photoUrl = prefs[keyPhotoUrl].orEmpty(),
            isLoggedIn = true,
        )
    }

    suspend fun saveSession(session: UserSession) {
        store.edit { prefs ->
            prefs[keyUid] = session.uid
            prefs[keyEmail] = session.email
            prefs[keyDisplayName] = session.displayName
            prefs[keyPhotoUrl] = session.photoUrl
            prefs[keyIsLoggedIn] = true
        }
    }

    suspend fun clearSession() {
        store.edit { it.clear() }
    }
}
