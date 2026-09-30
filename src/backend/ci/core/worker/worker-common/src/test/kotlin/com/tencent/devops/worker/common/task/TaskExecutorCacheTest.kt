package com.tencent.devops.worker.common.task

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

class TaskExecutorCacheTest {
    @Test
    fun cancellationBeforeFutureRegistrationCannotBeLost() {
        val execution = TaskExecutorCache.Execution(Executors.newSingleThreadExecutor())
        try {
            execution.cancel()
            val future = FutureTask { "success" }
            execution.attach(future)
            assertTrue(future.isCancelled)
            assertTrue(execution.cancelled)
            assertThrows(java.util.concurrent.CancellationException::class.java) {
                execution.submit(java.util.concurrent.Callable { fail<String>("Cancelled task must not start") })
            }
        } finally { execution.executor.shutdownNow() }
    }

    @Test
    fun repeatedCancellationAndRetriesHaveSeparateScopes() {
        val first = TaskExecutorCache.Execution(Executors.newSingleThreadExecutor())
        val retry = TaskExecutorCache.Execution(Executors.newSingleThreadExecutor())
        try {
            val future = FutureTask { "success" }
            first.attach(future)
            first.cancel()
            first.cancel()
            assertTrue(future.isCancelled)
            assertNotEquals(first.id, retry.id)
            assertFalse(retry.cancelled)
        } finally { first.executor.shutdownNow(); retry.executor.shutdownNow() }
    }
}
