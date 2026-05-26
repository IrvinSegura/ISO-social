package com.nexus.core.datastore.model

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val isLoggedIn: Boolean,
)

internal object SessionKeys {
    const val KEY_UID = "uid"
    const val KEY_EMAIL = "email"
    const val KEY_DISPLAY_NAME = "display_name"
    const val KEY_PHOTO_URL = "photo_url"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
}
