package com.nexus.feature.messages.presentation.badge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.feature.messages.domain.repository.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/** Unread messages across all conversations, for the badge on the Mensajes tab. */
@HiltViewModel
class UnreadMessagesViewModel @Inject constructor(
    repository: MessagesRepository,
) : ViewModel() {

    val unread: StateFlow<Int> = repository.observeConversations()
        .map { conversations -> conversations.sumOf { it.unread } }
        .catch { e ->
            Timber.w(e, "Unread counter failed")
            emit(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
