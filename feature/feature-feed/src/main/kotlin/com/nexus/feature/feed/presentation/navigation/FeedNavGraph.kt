package com.nexus.feature.feed.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.nexus.feature.feed.presentation.ui.FeedScreen
import kotlinx.serialization.Serializable

@Serializable
object FeedGraph

@Serializable
object FeedRoute

fun NavGraphBuilder.feedNavGraph(onNavigateToPost: (String) -> Unit) {
    navigation<FeedGraph>(startDestination = FeedRoute) {
        composable<FeedRoute> {
            FeedScreen(onNavigateToPost = onNavigateToPost)
        }
    }
}
