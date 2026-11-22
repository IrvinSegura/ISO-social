package com.nexus.feature.circles.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.feature.circles.presentation.detail.CircleDetailScreen
import com.nexus.feature.circles.presentation.list.CirclesScreen
import kotlinx.serialization.Serializable

@Serializable
object CirclesRoute

@Serializable
data class CircleDetailRoute(val circleId: String)

/**
 * The circles tab and a circle's screen. Publishing and messaging belong to other features,
 * so the app module passes those actions in.
 */
fun NavGraphBuilder.circlesNavGraph(
    onOpenCircle: (String) -> Unit,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePostInCircle: (circleId: String) -> Unit,
    onMessageMember: (uid: String) -> Unit,
) {
    composable<CirclesRoute> {
        CirclesScreen(onOpenCircle = onOpenCircle)
    }
    composable<CircleDetailRoute> {
        CircleDetailScreen(
            onBack = onBack,
            onPostClick = onPostClick,
            onCreatePost = onCreatePostInCircle,
            onMessageMember = onMessageMember,
        )
    }
}
