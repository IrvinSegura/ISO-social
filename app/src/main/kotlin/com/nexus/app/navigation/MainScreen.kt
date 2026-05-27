package com.nexus.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.feature.feed.presentation.navigation.FeedGraph
import com.nexus.feature.feed.presentation.navigation.feedNavGraph
import com.nexus.feature.post.presentation.navigation.CreatePostRoute
import com.nexus.feature.post.presentation.navigation.PostDetailRoute
import com.nexus.feature.post.presentation.navigation.postNavGraph
import kotlinx.serialization.Serializable

@Serializable object CirclesRoute
@Serializable object MessagesRoute
@Serializable object ProfileRoute

private data class BottomNavItem(
    val route: Any,
    val icon: ImageVector,
    val label: String,
    val isSelected: (NavDestination?) -> Boolean,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = FeedGraph,
        icon = Icons.Outlined.Home,
        label = "Inicio",
        isSelected = { dest -> dest?.hierarchy?.any { it.hasRoute(FeedGraph::class) } == true },
    ),
    BottomNavItem(
        route = CirclesRoute,
        icon = Icons.Outlined.Group,
        label = "Círculos",
        isSelected = { dest -> dest?.hierarchy?.any { it.hasRoute(CirclesRoute::class) } == true },
    ),
    BottomNavItem(
        route = MessagesRoute,
        icon = Icons.Outlined.Chat,
        label = "Mensajes",
        isSelected = { dest -> dest?.hierarchy?.any { it.hasRoute(MessagesRoute::class) } == true },
    ),
    BottomNavItem(
        route = ProfileRoute,
        icon = Icons.Outlined.Person,
        label = "Perfil",
        isSelected = { dest -> dest?.hierarchy?.any { it.hasRoute(ProfileRoute::class) } == true },
    ),
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnMainTab = bottomNavItems.any { it.isSelected(currentDestination) }

    Scaffold(
        bottomBar = {
            if (isOnMainTab) {
                NexusBottomBar(navController = navController, currentDestination = currentDestination)
            }
        },
        floatingActionButton = {
            if (isOnMainTab) {
                FloatingActionButton(
                    onClick = { navController.navigate(CreatePostRoute) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear publicación")
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = FeedGraph,
            modifier = Modifier.padding(paddingValues),
        ) {
            feedNavGraph(
                onNavigateToPost = { postId -> navController.navigate(PostDetailRoute(postId)) },
            )
            postNavGraph(navController)

            composable<CirclesRoute> { PlaceholderScreen("Círculos — próximamente") }
            composable<MessagesRoute> { PlaceholderScreen("Mensajes — próximamente") }
            composable<ProfileRoute> { PlaceholderScreen("Perfil — próximamente") }
        }
    }
}

@Composable
private fun NexusBottomBar(
    navController: NavController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = item.isSelected(currentDestination),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
