package com.nexus.feature.messages.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.feature.messages.domain.model.Contact
import com.nexus.feature.messages.domain.model.ConversationItem
import com.nexus.feature.messages.domain.repository.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ConversationsState(
    val items: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUid: String? = null,
    val contacts: List<Contact> = emptyList(),
    val contactsLoading: Boolean = true,
)

sealed interface ConversationsIntent {
    data object Retry : ConversationsIntent
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val repository: MessagesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationsState(currentUid = repository.currentUid))
    val state: StateFlow<ConversationsState> = _state.asStateFlow()

    private var conversationsJob: Job? = null
    private var contactsJob: Job? = null

    init {
        observe()
    }

    fun onIntent(intent: ConversationsIntent) {
        when (intent) {
            ConversationsIntent.Retry -> observe()
        }
    }

    private fun observe() {
        conversationsJob?.cancel()
        contactsJob?.cancel()
        _state.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
        conversationsJob = viewModelScope.launch {
            repository.observeConversations()
                .catch { e ->
                    Timber.w(e, "Conversations listener failed")
                    _state.update { it.copy(isLoading = false, error = "No se pudieron cargar tus mensajes") }
                }
                .collect { conversations ->
                    val users = repository.users(conversations.map { it.otherUid })
                    val items = conversations.map { conversation ->
                        val user = users[conversation.otherUid]
                        ConversationItem(
                            conversation = conversation,
                            otherName = user?.name ?: CircleDirectory.UNKNOWN_NAME,
                            otherPhotoUrl = user?.photoUrl,
                        )
                    }
                    _state.update { it.copy(items = items, isLoading = false, error = null) }
                }
        }
        contactsJob = viewModelScope.launch {
            repository.observeContacts()
                .catch { e ->
                    Timber.w(e, "Contacts could not be loaded")
                    _state.update { it.copy(contactsLoading = false) }
                }
                .collect { contacts -> _state.update { it.copy(contacts = contacts, contactsLoading = false) } }
        }
    }
}
