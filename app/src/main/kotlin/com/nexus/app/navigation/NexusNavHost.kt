package com.nexus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nexus.feature.auth.presentation.navigation.AuthGraph
import com.nexus.feature.auth.presentation.navigation.authNavGraph
import kotlinx.serialization.Serializable

@Serializable
object HomeGraph

@Composable
fun NexusNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AuthGraph,
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
        // homeNavGraph(navController) — se agrega en la próxima sesión
    }
}
