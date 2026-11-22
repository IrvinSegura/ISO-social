package com.nexus.feature.messages

import androidx.lifecycle.SavedStateHandle
import com.nexus.core.firebase.circle.UserBrief
import com.nexus.core.giphy.GiphyKind
import com.nexus.feature.messages.data.NoSharedCircleException
import com.nexus.feature.messages.domain.model.ChatMessage
import com.nexus.feature.messages.domain.model.Contact
import com.nexus.feature.messages.domain.model.Conversation
import com.nexus.feature.messages.domain.model.Conversations
import com.nexus.feature.messages.domain.repository.MessagesRepository
import com.nexus.feature.messages.presentation.badge.UnreadMessagesViewModel
import com.nexus.feature.messages.presentation.chat.ChatEvent
import com.nexus.feature.messages.presentation.chat.ChatIntent
import com.nexus.feature.messages.presentation.chat.ChatRow
import com.nexus.feature.messages.presentation.chat.ChatViewModel
import com.nexus.feature.messages.presentation.chat.buildRows
import com.nexus.feature.messages.presentation.chat.dayLabel
import com.nexus.feature.messages.presentation.list.ConversationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {

    /** Conversations and messages in memory; [send] behaves like the batch in Firestore. */
    private class FakeRepository : MessagesRepository {
        override val currentUid = "me"
        val conversations = MutableStateFlow<Map<String, Conversation>>(emptyMap())
        val messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
        val contacts = MutableStateFlow<List<Contact>>(emptyList())
        var failSend: Exception? = null
        val markedRead = mutableListOf<String>()
        private var nextId = 1

        override fun observeConversations(): Flow<List<Conversation>> =
            conversations.map { it.values.sortedByDescending { c -> c.updatedAt } }

        override fun observeContacts(): Flow<List<Contact>> = contacts

        override fun observeConversation(otherUid: String): Flow<Conversation?> =
            conversations.map { it[Conversations.idFor(currentUid, otherUid)] }

        override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
            messages.map { it[conversationId].orEmpty() }

        override suspend fun users(uids: Collection<String>) =
            uids.associateWith { UserBrief(it, it.replaceFirstChar { c -> c.uppercase() } + " Pérez", null) }

        override suspend fun send(otherUid: String, text: String, sharedCircleId: String?, gifUrl: String?) {
            failSend?.let { throw it }
            val id = Conversations.idFor(currentUid, otherUid)
            val existing = conversations.value[id]
            if (existing == null && sharedCircleId == null) throw NoSharedCircleException()
            val now = (existing?.updatedAt ?: 0L) + 1
            conversations.value += id to (existing ?: Conversation(id, otherUid, "", "", 0, 0, 0))
                .copy(lastText = text.ifEmpty { Conversations.GIF_PREVIEW }, lastSenderId = currentUid, updatedAt = now)
            messages.value += id to (messages.value[id].orEmpty() + ChatMessage("m${nextId++}", currentUid, text, now, false, gifUrl))
        }

        override suspend fun markRead(conversationId: String) {
            markedRead += conversationId
            conversations.value[conversationId]?.let { conversations.value += conversationId to it.copy(unread = 0) }
        }
    }

    private fun contact(uid: String, circle: String = "c1") = Contact(uid, uid, null, circle, listOf("Familia"))

    private fun chat(repo: FakeRepository, other: String = "bob") =
        ChatViewModel(SavedStateHandle(mapOf("otherUid" to other)), repo)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `conversation ids do not depend on who writes first`() {
        assertEquals("alice_bob", Conversations.idFor("bob", "alice"))
        assertEquals("alice_bob", Conversations.idFor("alice", "bob"))
    }

    @Test
    fun `the first message creates the conversation with a shared circle`() = runTest {
        val repo = FakeRepository().apply { contacts.value = listOf(contact("bob")) }
        val vm = chat(repo)
        advanceUntilIdle()
        assertNull(vm.state.value.conversation)
        assertFalse(vm.state.value.isLoading)

        vm.onIntent(ChatIntent.InputChanged("  Hola Bob  "))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("", state.input)
        assertEquals("bob_me", state.conversation?.id)
        assertEquals(listOf("Hola Bob"), state.messages.map { it.text })
        assertEquals("Bob Pérez", state.otherName)
    }

    @Test
    fun `without a shared circle a new conversation cannot start`() = runTest {
        val repo = FakeRepository()
        val vm = chat(repo)
        advanceUntilIdle()

        vm.onIntent(ChatIntent.InputChanged("Hola"))

        assertFalse(vm.state.value.canWrite)
        assertFalse(vm.state.value.canSend)
    }

    @Test
    fun `a failed send gives the text back`() = runTest {
        val repo = FakeRepository().apply {
            contacts.value = listOf(contact("bob"))
            failSend = IllegalStateException("offline")
        }
        val vm = chat(repo)
        val events = mutableListOf<ChatEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }
        advanceUntilIdle()

        vm.onIntent(ChatIntent.InputChanged("Hola"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        assertEquals("Hola", vm.state.value.input)
        assertFalse(vm.state.value.isSending)
        assertTrue(events.single() is ChatEvent.ShowMessage)
    }

    @Test
    fun `a GIF is sent on its own and keeps what was typed`() = runTest {
        val repo = FakeRepository().apply { contacts.value = listOf(contact("bob")) }
        val vm = chat(repo)
        advanceUntilIdle()

        vm.onIntent(ChatIntent.InputChanged("luego te cuento"))
        vm.onIntent(ChatIntent.ShowGiphyPicker(GiphyKind.EMOJI))
        assertEquals(GiphyKind.EMOJI, vm.state.value.giphyPicker)
        vm.onIntent(ChatIntent.SendGif("https://media.giphy.com/media/abc/giphy.gif"))
        advanceUntilIdle()

        val message = vm.state.value.messages.single()
        assertEquals("https://media.giphy.com/media/abc/giphy.gif", message.gifUrl)
        assertEquals("", message.text)
        assertEquals("GIF", vm.state.value.conversation?.lastText)
        assertEquals("luego te cuento", vm.state.value.input)
        assertNull(vm.state.value.giphyPicker)
    }

    @Test
    fun `unread messages are marked read only while the chat is visible`() = runTest {
        val repo = FakeRepository().apply {
            conversations.value = mapOf("bob_me" to Conversation("bob_me", "bob", "Hola", "bob", 5, unread = 2, otherLastRead = 0))
        }
        val vm = chat(repo)
        advanceUntilIdle()
        assertTrue(repo.markedRead.isEmpty())

        vm.onIntent(ChatIntent.VisibilityChanged(true))
        advanceUntilIdle()

        assertEquals(listOf("bob_me"), repo.markedRead)
        assertEquals(0, vm.state.value.conversation?.unread)
    }

    @Test
    fun `seen marks the newest own message the other person has read`() = runTest {
        val repo = FakeRepository().apply {
            conversations.value = mapOf("bob_me" to Conversation("bob_me", "bob", "c", "me", 30, 0, otherLastRead = 25))
            messages.value = mapOf(
                "bob_me" to listOf(
                    ChatMessage("a", "me", "a", 10, false),
                    ChatMessage("b", "bob", "b", 15, false),
                    ChatMessage("c", "me", "c", 20, false),
                    ChatMessage("d", "me", "d", 30, false),
                ),
            )
        }
        val vm = chat(repo)
        advanceUntilIdle()

        assertEquals("c", vm.state.value.seenMessageId)
    }

    @Test
    fun `conversation list resolves names and the badge sums unread`() = runTest {
        val repo = FakeRepository().apply {
            conversations.value = mapOf(
                "bob_me" to Conversation("bob_me", "bob", "Hola", "bob", 5, unread = 2, otherLastRead = 0),
                "ana_me" to Conversation("ana_me", "ana", "Hey", "me", 9, unread = 1, otherLastRead = 0),
            )
        }
        val list = ConversationsViewModel(repo)
        val badge = UnreadMessagesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { badge.unread.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("Ana Pérez", "Bob Pérez"), list.state.value.items.map { it.otherName })
        assertEquals(3, badge.unread.value)
    }

    @Test
    fun `rows are newest first with a day label above each day`() {
        val day = 24 * 60 * 60 * 1000L
        val now = 1_700_000_000_000L
        val rows = buildRows(
            messages = listOf(
                ChatMessage("old", "bob", "ayer", now - day, false),
                ChatMessage("new", "me", "hoy", now, false),
            ),
            currentUid = "me",
            seenMessageId = "new",
            now = now,
        )
        assertEquals(listOf("new", "day-Hoy", "old", "day-Ayer"), rows.map { it.key })
        val newest = rows.first() as ChatRow.Message
        assertTrue(newest.isMine)
        assertTrue(newest.seen)
        assertEquals("Hoy", dayLabel(now, now))
    }
}
