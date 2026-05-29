package com.nexus.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        icon = Icons.AutoMirrored.Outlined.Chat,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavItems.forEach { item ->
                val selected = item.isSelected(currentDestination)
                NavBarItem(
                    item = item,
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconColor",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bgColor",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor,
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
