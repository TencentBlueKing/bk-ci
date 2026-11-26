package com.tencent.devops.worker.common.logger

import com.tencent.devops.common.log.pojo.message.LogMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundedLogQueueTest {

    @Test
    fun enforcesCountCapAndWakesProducerAfterDrain() {
        val queue = BoundedLogQueue(maxCount = 2, maxBytes = 1024 * 1024)
        assertTrue(queue.offer(log("a"), 50))
        assertTrue(queue.offer(log("b"), 50))
        assertFalse(queue.offer(log("c"), 30))

        val producerStarted = CountDownLatch(1)
        val producerDone = CountDownLatch(1)
        val offered = AtomicBoolean(false)
        val producer = Thread {
            producerStarted.countDown()
            offered.set(queue.offer(log("c"), 1000))
            producerDone.countDown()
        }
        producer.start()
        assertTrue(producerStarted.await(1, TimeUnit.SECONDS))

        val drained = mutableListOf<LogMessage>()
        assertEquals(1, queue.drainTo(drained, maxCount = 1, maxBytes = 1024 * 1024))
        assertEquals("a", drained.single().message)

        assertTrue(producerDone.await(1, TimeUnit.SECONDS))
        assertTrue(offered.get())

        val rest = mutableListOf<LogMessage>()
        assertEquals(2, queue.drainTo(rest, maxCount = 10, maxBytes = 1024 * 1024))
        assertEquals(listOf("b", "c"), rest.map { it.message })
        assertTrue(queue.isEmpty())
        assertEquals(0L, queue.pendingBytes())
    }

    @Test
    fun enforcesByteCapAndAllowsSingleOversizedMessageWhenEmpty() {
        val payload = "x".repeat(200)
        val oneBytes = LoggerUploadBatch.estimateBytes(payload).toLong()
        val queue = BoundedLogQueue(maxCount = 100, maxBytes = oneBytes * 2)

        assertTrue(queue.offer(log(payload), 50))
        assertTrue(queue.offer(log(payload), 50))
        assertFalse(queue.offer(log(payload), 30))
        assertEquals(oneBytes * 2, queue.pendingBytes())

        // 清空后，单条超过 maxBytes 的消息依然可以入队避免死锁
        queue.clear()
        assertEquals(0L, queue.pendingBytes())
        val huge = "y".repeat((oneBytes * 3).toInt())
        assertTrue(queue.offer(log(huge), 50))
        assertEquals(1, queue.size())

        val drained = mutableListOf<LogMessage>()
        assertEquals(1, queue.drainTo(drained, maxCount = 10, maxBytes = oneBytes))
        assertEquals(huge, drained.single().message)
        assertEquals(0L, queue.pendingBytes())
    }

    @Test
    fun drainToRespectsByteBudget() {
        val payload = "a".repeat(100)
        val oneBytes = LoggerUploadBatch.estimateBytes(payload).toLong()
        val queue = BoundedLogQueue(maxCount = 100, maxBytes = oneBytes * 10)
        repeat(5) { idx ->
            assertTrue(queue.offer(log("$payload$idx"), 50))
        }

        val batch = mutableListOf<LogMessage>()
        val moved = queue.drainTo(batch, maxCount = 10, maxBytes = oneBytes * 2 + 10)
        assertEquals(2, moved)
        assertEquals(3, queue.size())
    }

    @Test
    fun clearUnblocksWaitingProducerImmediately() {
        val queue = BoundedLogQueue(maxCount = 1, maxBytes = 1024 * 1024)
        assertTrue(queue.offer(log("only"), 50))

        val started = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val offered = AtomicBoolean(false)
        val producer = Thread {
            started.countDown()
            offered.set(queue.offer(log("next"), 2000))
            finished.countDown()
        }
        producer.start()
        assertTrue(started.await(1, TimeUnit.SECONDS))
        Thread.sleep(50)
        queue.clear()

        assertTrue(finished.await(1, TimeUnit.SECONDS))
        assertTrue(offered.get())
    }

    private fun log(message: String) = LogMessage(message = message, timestamp = 1L)
}
