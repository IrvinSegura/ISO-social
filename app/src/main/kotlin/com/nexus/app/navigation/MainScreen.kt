package com.nexus.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.core.ui.theme.NexusAccent
import com.nexus.feature.circles.presentation.navigation.CircleDetailRoute
import com.nexus.feature.circles.presentation.navigation.CirclesRoute
import com.nexus.feature.circles.presentation.navigation.circlesNavGraph
import com.nexus.feature.feed.presentation.navigation.FeedGraph
import com.nexus.feature.feed.presentation.navigation.feedNavGraph
import com.nexus.feature.messages.presentation.badge.UnreadMessagesViewModel
import com.nexus.feature.messages.presentation.navigation.ChatRoute
import com.nexus.feature.messages.presentation.navigation.MessagesRoute
import com.nexus.feature.messages.presentation.navigation.messagesNavGraph
import com.nexus.feature.post.presentation.navigation.CreatePostRoute
import com.nexus.feature.post.presentation.navigation.PostDetailRoute
import com.nexus.feature.post.presentation.navigation.postNavGraph
import com.nexus.feature.profile.presentation.navigation.ProfileRoute
import com.nexus.feature.profile.presentation.navigation.profileScreen
import kotlin.reflect.KClass

private data class BottomNavItem(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    /** The tab has its own "publish" action, so the floating button shows there. */
    val showsFab: Boolean,
) {
    fun isSelected(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.hasRoute(routeClass) } == true
}

private val bottomNavItems = listOf(
    BottomNavItem(FeedGraph, FeedGraph::class, Icons.Outlined.Home, Icons.Filled.Home, "Inicio", showsFab = true),
    BottomNavItem(CirclesRoute, CirclesRoute::class, Icons.Outlined.Group, Icons.Filled.Group, "Círculos", showsFab = false),
    BottomNavItem(
        MessagesRoute, MessagesRoute::class, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat,
        "Mensajes", showsFab = false,
    ),
    BottomNavItem(ProfileRoute, ProfileRoute::class, Icons.Outlined.Person, Icons.Filled.Person, "Perfil", showsFab = true),
)

@Composable
fun MainScreen(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTab = bottomNavItems.firstOrNull { it.isSelected(currentDestination) }
    val unreadViewModel: UnreadMessagesViewModel = hiltViewModel()
    val unread by unreadViewModel.unread.collectAsStateWithLifecycle()

    fun openTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NexusBottomBar(
                    currentDestination = currentDestination,
                    unreadMessages = unread,
                    onSelect = { openTab(it.route) },
                )
            }
        },
        floatingActionButton = {
            if (currentTab?.showsFab == true) {
                FloatingActionButton(
                    onClick = { navController.navigate(CreatePostRoute()) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Crear publicación")
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = FeedGraph,
            // Consumed, so screens with their own top bar or insets do not pad them twice
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {
            feedNavGraph(
                onNavigateToPost = { postId -> navController.navigate(PostDetailRoute(postId)) },
            )
            postNavGraph(navController)

            circlesNavGraph(
                onOpenCircle = { circleId -> navController.navigate(CircleDetailRoute(circleId)) },
                onBack = { navController.popBackStack() },
                onPostClick = { postId -> navController.navigate(PostDetailRoute(postId)) },
                onCreatePostInCircle = { circleId -> navController.navigate(CreatePostRoute(circleId)) },
                onMessageMember = { uid -> navController.navigate(ChatRoute(uid)) },
            )
            messagesNavGraph(
                onOpenChat = { uid -> navController.navigate(ChatRoute(uid)) },
                onGoToCircles = { openTab(CirclesRoute) },
                onBack = { navController.popBackStack() },
            )
            profileScreen(
                onPostClick = { postId -> navController.navigate(PostDetailRoute(postId)) },
                onCreatePost = { navController.navigate(CreatePostRoute()) },
                onSignOut = onSignOut,
            )
        }
    }
}

/** Flat bar with a hairline on top, as the blog's header: dark ink when selected, an amber mark. */
@Composable
private fun NexusBottomBar(
    currentDestination: NavDestination?,
    unreadMessages: Int,
    onSelect: (BottomNavItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavItems.forEach { item ->
                NavBarItem(
                    item = item,
                    selected = item.isSelected(currentDestination),
                    badge = if (item.route == MessagesRoute) unreadMessages else 0,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navColor",
    )
    val markWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navMark",
    )
    val description = if (badge > 0) "${item.label}, $badge sin leer" else item.label

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(50))
                        .background(NexusAccent)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.surface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(markWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) NexusAccent else Color.Transparent),
        )
    }
}
