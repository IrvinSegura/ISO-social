package com.nexus.feature.messages.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.feature.messages.presentation.chat.ChatScreen
import com.nexus.feature.messages.presentation.list.ConversationsScreen
import kotlinx.serialization.Serializable

@Serializable
object MessagesRoute

/** The conversation with [otherUid]; it is created when the first message is sent. */
@Serializable
data class ChatRoute(val otherUid: String)

fun NavGraphBuilder.messagesNavGraph(
    onOpenChat: (otherUid: String) -> Unit,
    onGoToCircles: () -> Unit,
    onBack: () -> Unit,
) {
    composable<MessagesRoute> {
        ConversationsScreen(onOpenChat = onOpenChat, onGoToCircles = onGoToCircles)
    }
    composable<ChatRoute> {
        ChatScreen(onBack = onBack)
    }
}
