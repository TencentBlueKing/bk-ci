package com.tencent.devops.worker.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TaskProcessCleanupTest {
    @Test
    fun nativeCleanupCannotBlockCallerIndefinitely() {
        val release = CountDownLatch(1)
        val start = System.nanoTime()
        try {
            val failure = TaskProcessCleanup.run(100) {
                while (release.count > 0) {
                    try { release.await() } catch (e: InterruptedException) { /* native I/O */ }
                }
            }
            assertNotNull(failure)
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000)
        } finally { release.countDown() }
    }

    @Test
    fun cleanupFailureIsReturnedAndInterruptIsPreserved() {
        val expected = IllegalStateException("kill failed")
        assertSame(expected, TaskProcessCleanup.run { throw expected })
        try {
            Thread.currentThread().interrupt()
            assertNull(TaskProcessCleanup.run {})
            assertTrue(Thread.currentThread().isInterrupted)
        } finally { Thread.interrupted() }
    }
}
