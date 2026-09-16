package com.tencent.devops.worker.common.utils

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/** Bounds native process-tree operations without creating an unbounded number of cleanup threads. */
internal object TaskProcessCleanup {
    private val slots = Semaphore(2)

    private fun submit(action: () -> Unit): Future<*> {
        if (!slots.tryAcquire()) throw RejectedExecutionException("Process cleanup capacity exhausted")
        val future = FutureTask(action, Unit)
        val thread = Thread({
            try { future.run() } finally { slots.release() }
        }, "task-process-cleanup").apply { isDaemon = true }
        try { thread.start() } catch (e: Throwable) { slots.release(); throw e }
        return future
    }
    fun run(timeoutMillis: Long = 30_000, action: () -> Unit): Throwable? {
        var interrupted = Thread.interrupted()
        var future: Future<*>? = null
        return try {
            future = submit(action)
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
            null
        } catch (e: InterruptedException) {
            interrupted = true
            e
        } catch (e: ExecutionException) {
            e.cause ?: e
        } catch (e: Exception) {
            e
        } finally {
            if (future != null && !future.isDone) future.cancel(true)
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
