package com.nexus.feature.messages.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.core.giphy.GiphyKind
import com.nexus.feature.messages.data.NoSharedCircleException
import com.nexus.feature.messages.domain.model.ChatMessage
import com.nexus.feature.messages.domain.model.Conversation
import com.nexus.feature.messages.domain.model.Conversations
import com.nexus.feature.messages.domain.repository.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChatState(
    val otherUid: String = "",
    val otherName: String = "",
    val otherPhotoUrl: String? = null,
    val currentUid: String? = null,
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    /** A circle both share: needed only to start a conversation that does not exist yet. */
    val sharedCircleId: String? = null,
    val contactsLoaded: Boolean = false,
    val input: String = "",
    val isSending: Boolean = false,
    /** The Giphy picker is open on this tab; null while closed. */
    val giphyPicker: GiphyKind? = null,
) {
    /** The newest message of the user that the other person has already opened. */
    val seenMessageId: String?
        get() {
            val seenUntil = conversation?.otherLastRead ?: return null
            return messages.lastOrNull { it.senderId == currentUid && !it.isPending && it.createdAt <= seenUntil }?.id
        }

    /** False when there is no conversation yet and no circle in common to start one. */
    val canWrite: Boolean get() = conversation != null || sharedCircleId != null || !contactsLoaded
    val canSend: Boolean get() = input.isNotBlank() && !isSending && canWrite
    val canSendGif: Boolean get() = !isSending && canWrite
}

sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data class ShowGiphyPicker(val kind: GiphyKind) : ChatIntent
    data object HideGiphyPicker : ChatIntent
    /** Sends a GIF or animated emoji on its own; whatever is typed stays in the field. */
    data class SendGif(val url: String) : ChatIntent
    /** The screen is in the foreground: incoming messages count as read. */
    data class VisibilityChanged(val visible: Boolean) : ChatIntent
}

sealed interface ChatEvent {
    data class ShowMessage(val text: String) : ChatEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MessagesRepository,
) : ViewModel() {

    private val otherUid: String = checkNotNull(savedStateHandle.get<String>("otherUid")) { "otherUid missing" }

    private val _state = MutableStateFlow(ChatState(otherUid = otherUid, currentUid = repository.currentUid))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var visible = false
    private var markingRead = false

    init {
        loadOther()
        observeSharedCircle()
        observeConversation()
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> _state.update { it.copy(input = intent.text.take(Conversations.MAX_TEXT)) }
            ChatIntent.Send -> send()
            is ChatIntent.ShowGiphyPicker -> _state.update { it.copy(giphyPicker = intent.kind) }
            ChatIntent.HideGiphyPicker -> _state.update { it.copy(giphyPicker = null) }
            is ChatIntent.SendGif -> sendGif(intent.url)
            is ChatIntent.VisibilityChanged -> {
                visible = intent.visible
                markReadIfNeeded()
            }
        }
    }

    private fun loadOther() {
        viewModelScope.launch {
            val user = runCatching { repository.users(listOf(otherUid))[otherUid] }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            _state.update {
                it.copy(otherName = user?.name ?: CircleDirectory.UNKNOWN_NAME, otherPhotoUrl = user?.photoUrl)
            }
        }
    }

    private fun observeSharedCircle() {
        viewModelScope.launch {
            repository.observeContacts()
                .catch { e ->
                    Timber.w(e, "Contacts could not be loaded")
                    _state.update { it.copy(contactsLoaded = true) }
                }
                .collect { contacts ->
                    val shared = contacts.firstOrNull { it.uid == otherUid }?.sharedCircleId
                    _state.update { it.copy(sharedCircleId = shared, contactsLoaded = true) }
                }
        }
    }

    private fun observeConversation() {
        val conversation = repository.observeConversation(otherUid)
            .catch { e ->
                Timber.w(e, "Conversation listener failed")
                _state.update { it.copy(isLoading = false) }
            }
        viewModelScope.launch {
            conversation.collect { current ->
                _state.update { it.copy(conversation = current, isLoading = it.isLoading && current != null) }
                markReadIfNeeded()
            }
        }
        // Messages can only be read once the conversation exists (the rules check it)
        viewModelScope.launch {
            conversation
                .map { it?.id }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null) emptyFlow() else repository.observeMessages(id).catch { e ->
                        Timber.w(e, "Messages listener failed")
                        _events.send(ChatEvent.ShowMessage("No se pudieron cargar los mensajes"))
                    }
                }
                .collect { messages -> _state.update { it.copy(messages = messages, isLoading = false) } }
        }
    }

    private fun markReadIfNeeded() {
        val conversation = _state.value.conversation ?: return
        if (!visible || markingRead || conversation.unread == 0) return
        markingRead = true
        viewModelScope.launch {
            try {
                repository.markRead(conversation.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not mark the conversation as read")
            } finally {
                markingRead = false
            }
        }
    }

    private fun send() {
        val current = _state.value
        if (!current.canSend) return
        val text = current.input.trim()
        // The field empties right away; the message shows from the local write. Only the
        // first message waits, since it creates the conversation the next ones update
        _state.update { it.copy(input = "", isSending = current.conversation == null) }
        deliver(current, text = text, gifUrl = null)
    }

    private fun sendGif(url: String) {
        val current = _state.value
        _state.update { it.copy(giphyPicker = null) }
        if (!current.canSendGif) return
        _state.update { it.copy(isSending = current.conversation == null) }
        deliver(current, text = "", gifUrl = url)
    }

    private fun deliver(current: ChatState, text: String, gifUrl: String?) {
        viewModelScope.launch {
            try {
                repository.send(otherUid, text, current.sharedCircleId, gifUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not send a message")
                // Give the text back so nothing typed is lost
                if (text.isNotEmpty()) _state.update { if (it.input.isEmpty()) it.copy(input = text) else it }
                val message = if (e is NoSharedCircleException) {
                    "Solo puedes escribir a personas con las que compartes un círculo"
                } else {
                    "No se pudo enviar. Revisa tu conexión."
                }
                _events.send(ChatEvent.ShowMessage(message))
            } finally {
                _state.update { it.copy(isSending = false) }
            }
        }
    }
}
