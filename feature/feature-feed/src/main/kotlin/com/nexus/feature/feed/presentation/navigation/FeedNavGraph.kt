package com.nexus.feature.feed.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.nexus.feature.feed.presentation.ui.FeedScreen
import kotlinx.serialization.Serializable

@Serializable
object FeedGraph

@Serializable
object FeedRoute

@Serializable
data class PostDetailRoute(val postId: String)

fun NavGraphBuilder.feedNavGraph(navController: NavController) {
    navigation<FeedGraph>(startDestination = FeedRoute) {
        composable<FeedRoute> {
            FeedScreen(
                onNavigateToPost = { postId ->
                    navController.navigate(PostDetailRoute(postId))
                },
            )
        }
        // PostDetailScreen — Session 3 (feature-post)
    }
}
