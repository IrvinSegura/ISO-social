package com.nexus.feature.profile.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.feature.profile.presentation.ui.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute

/**
 * The profile tab. Sign-out comes from the app module, so this feature does not depend on
 * the auth feature.
 */
fun NavGraphBuilder.profileScreen(
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onSignOut: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onPostClick = onPostClick,
            onCreatePost = onCreatePost,
            onSignOut = onSignOut,
        )
    }
}
