package com.nexus.feature.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.nexus.feature.auth.presentation.login.LoginScreen
import com.nexus.feature.auth.presentation.register.RegisterScreen
import kotlinx.serialization.Serializable

@Serializable
object AuthGraph

@Serializable
private object LoginRoute

@Serializable
private object RegisterRoute

fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    onNavigateToHome: () -> Unit,
) {
    navigation<AuthGraph>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
                onNavigateToHome = onNavigateToHome,
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = onNavigateToHome,
            )
        }
    }
}
