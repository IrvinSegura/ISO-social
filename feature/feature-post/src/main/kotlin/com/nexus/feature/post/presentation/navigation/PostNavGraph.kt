package com.nexus.feature.post.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.feature.post.presentation.create.ui.CreatePostScreen
import com.nexus.feature.post.presentation.detail.ui.PostDetailScreen
import kotlinx.serialization.Serializable

/** [circleId] preselects that circle as the audience; null publishes to everyone. */
@Serializable
data class CreatePostRoute(val circleId: String? = null)

@Serializable
data class PostDetailRoute(val postId: String)

fun NavGraphBuilder.postNavGraph(navController: NavController) {
    composable<CreatePostRoute> {
        CreatePostScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<PostDetailRoute> {
        PostDetailScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
