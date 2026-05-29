package com.nexus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.app.NexusAppViewModel
import com.nexus.feature.auth.presentation.navigation.AuthGraph
import com.nexus.feature.auth.presentation.navigation.authNavGraph
import kotlinx.serialization.Serializable

@Serializable
object HomeGraph

@Composable
fun NexusNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: NexusAppViewModel = hiltViewModel(),
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val sessionLoggedIn by viewModel.sessionFlow.collectAsStateWithLifecycle()

    // Destino inicial: DataStore responde antes que Firebase Auth para evitar parpadeo
    val startDestination = if (sessionLoggedIn) HomeGraph else AuthGraph

    // Redirige a auth en cuanto Firebase confirma que no hay sesión activa
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
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
            MainScreen()
        }
    }
}
