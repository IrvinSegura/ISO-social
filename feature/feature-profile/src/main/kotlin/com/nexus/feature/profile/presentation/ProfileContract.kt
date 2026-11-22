package com.nexus.feature.profile.presentation

import com.nexus.core.datastore.model.ThemeMode
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.domain.model.ProfileStats
import com.nexus.feature.profile.domain.model.ProfileUser

enum class ProfileTab { POSTS, SAVED }

/** Content of one tab: loading the first time, then its items or an error. */
data class TabContent(
    val items: List<ProfilePost> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class ProfileState(
    val user: ProfileUser? = null,
    val stats: ProfileStats = ProfileStats(),
    val selectedTab: ProfileTab = ProfileTab.POSTS,
    val myPosts: TabContent = TabContent(),
    val saved: TabContent = TabContent(),
    val isRefreshing: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

sealed interface ProfileIntent {
    data class SelectTab(val tab: ProfileTab) : ProfileIntent
    data object Refresh : ProfileIntent
    data object RetryPosts : ProfileIntent
    data object RetrySaved : ProfileIntent
    data class Unsave(val postId: String) : ProfileIntent
    data class UndoUnsave(val postId: String) : ProfileIntent
    data class SetTheme(val mode: ThemeMode) : ProfileIntent
}

sealed interface ProfileEvent {
    data class ShowMessage(val text: String) : ProfileEvent
    /** A post left the saved list; the screen offers to undo it. */
    data class Unsaved(val postId: String) : ProfileEvent
}
