package com.nexus.feature.post.presentation.create

import app.cash.turbine.test
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.usecase.CreatePostUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PostUploadManagerTest {

    private val createPost = mockk<CreatePostUseCase>()
    private val bus = NewPostEventBus()

    private val draft = PostDraft(
        content = "hello",
        imageUris = emptyList(),
        audioPath = null,
        fileUri = null,
        fileName = null,
        mood = null,
        locationName = null,
    )

    private fun TestScope.manager(): PostUploadManager =
        PostUploadManager(createPost, bus, backgroundScope).also { advanceUntilIdle() }

    private fun givenUploadResults(vararg results: NexusResult<String>) {
        coEvery {
            createPost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returnsMany results.toList()
    }

    private val failure = NexusResult.Error(NexusException.UnknownException(RuntimeException("offline")))

    @Test
    fun `a successful upload is confirmed with the new post id`() = runTest {
        givenUploadResults(NexusResult.Success("post-1"))
        val manager = manager()

        bus.events.test {
            manager.enqueue("temp-1", draft)

            val payload = awaitItem()
            assertEquals("temp-1", payload.tempId)
            assertEquals("post-1", payload.confirmedId)
        }
    }

    @Test
    fun `a failed upload is reported and can be retried`() = runTest {
        givenUploadResults(failure, NexusResult.Success("post-1"))
        val manager = manager()

        bus.events.test {
            manager.enqueue("temp-1", draft)
            assertNull(awaitItem().confirmedId)

            bus.requestRetry("temp-1")
            assertEquals("post-1", awaitItem().confirmedId)
        }
        coVerify(exactly = 2) { createPost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a discarded draft is not uploaded again`() = runTest {
        givenUploadResults(failure)
        val manager = manager()

        bus.events.test {
            manager.enqueue("temp-1", draft)
            awaitItem()

            bus.requestDiscard("temp-1")
            bus.requestRetry("temp-1")
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify(exactly = 1) { createPost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `the recorded audio is deleted after a successful upload`() = runTest {
        val audio = File.createTempFile("voice", ".m4a")
        givenUploadResults(NexusResult.Success("post-1"))
        val manager = manager()

        bus.events.test {
            manager.enqueue("temp-1", draft.copy(audioPath = audio.path))
            awaitItem()
        }

        assertFalse(audio.exists())
    }

    @Test
    fun `the recorded audio is kept while the draft can be retried`() = runTest {
        val audio = File.createTempFile("voice", ".m4a")
        givenUploadResults(failure)
        val manager = manager()

        bus.events.test {
            manager.enqueue("temp-1", draft.copy(audioPath = audio.path))
            awaitItem()
        }

        assertTrue(audio.exists())
        audio.delete()
    }
}
