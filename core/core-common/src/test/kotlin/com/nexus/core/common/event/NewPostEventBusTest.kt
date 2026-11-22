package com.nexus.core.common.event

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NewPostEventBusTest {

    private val bus = NewPostEventBus()

    @Test
    fun `confirmed upload carries the real post id`() = runTest {
        bus.events.test {
            bus.emitConfirmed(tempId = "temp-1", confirmedId = "post-9")

            val payload = awaitItem()
            assertEquals("temp-1", payload.tempId)
            assertEquals("post-9", payload.confirmedId)
            assertFalse(payload.isPending)
        }
    }

    @Test
    fun `failed upload has no confirmed id`() = runTest {
        bus.events.test {
            bus.emitFailed("temp-1")

            val payload = awaitItem()
            assertEquals("temp-1", payload.tempId)
            assertNull(payload.confirmedId)
            assertFalse(payload.isPending)
        }
    }

    @Test
    fun `retry and discard requests reach the uploader`() = runTest {
        bus.requests.test {
            bus.requestRetry("temp-1")
            bus.requestDiscard("temp-2")

            assertEquals(PendingPostRequest.Retry("temp-1"), awaitItem())
            assertEquals(PendingPostRequest.Discard("temp-2"), awaitItem())
        }
    }
}
