package com.nexus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.app.AuthStatus
import com.nexus.app.NexusAppViewModel
import com.nexus.feature.auth.presentation.navigation.AuthGraph
import com.nexus.feature.auth.presentation.navigation.authNavGraph
import kotlinx.serialization.Serializable

@Serializable
object HomeGraph

@Composable
fun NexusNavHost(
    viewModel: NexusAppViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()

    // The splash screen covers this state, nothing to draw yet
    if (authStatus == AuthStatus.Loading) return

    // Resolved only once: later auth changes are handled by navigation, not by
    // swapping the graph's start destination
    val startDestination: Any = remember {
        if (authStatus == AuthStatus.LoggedIn) HomeGraph else AuthGraph
    }

    // Session ended (sign-out, revoked token…) → back to auth, clearing the back stack
    LaunchedEffect(authStatus) {
        val inAuthGraph = navController.currentDestination
            ?.hierarchy
            ?.any { it.hasRoute(AuthGraph::class) } == true
        if (authStatus == AuthStatus.LoggedOut && !inAuthGraph) {
            navController.navigate(AuthGraph) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authNavGraph(
            navController = navController,
            onNavigateToHome = {
                navController.navigate(HomeGraph) {
                    popUpTo(AuthGraph) { inclusive = true }
                }
            },
        )
        composable<HomeGraph> {
            MainScreen(onSignOut = viewModel::signOut)
        }
    }
}
