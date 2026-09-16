/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.utils

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteStreamHandler
import org.apache.commons.exec.PumpStreamHandler
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process completion must not depend on a descendant closing an inherited pipe.
 * Keep this implementation in sync with the other worker/run executor.
 */
open class CommandLineExecutor @JvmOverloads constructor(
    private val streamCleanupTimeoutMillis: Long = 10_000L
) : DefaultExecutor() {
    init {
        require(streamCleanupTimeoutMillis > 0)
    }

    @Volatile
    var streamCleanupFailure: Throwable? = null
        private set

    override fun execute(command: CommandLine, environment: MutableMap<String, String>?): Int {
        streamCleanupFailure = null
        val directory = workingDirectory
        if (directory != null && !directory.exists()) {
            throw java.io.IOException("$directory doesn't exist.")
        }
        val process = launch(command, environment, directory)
        val stdout = EofInputStream(process.inputStream)
        val stderr = EofInputStream(process.errorStream)
        val streams = streamHandler
        var exited = false
        var primaryFailure: Throwable? = null
        var registered = false
        try {
            streams.setProcessInputStream(process.outputStream)
            streams.setProcessOutputStream(stdout)
            streams.setProcessErrorStream(stderr)
            streams.start()
            processDestroyer?.let {
                it.add(process)
                registered = true
            }
            watchdog?.start(process)
            val exitCode = process.waitFor()
            exited = true
            watchdog?.stop()
            watchdog?.checkException()
            return exitCode
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            watchdog?.stop()
            if (registered) processDestroyer?.remove(process)
            // waitFor clears interruption. Restore it only after bounded cleanup.
            val interrupted = Thread.interrupted() || primaryFailure is InterruptedException
            val cleanupFailure = finish(process, streams, stdout, stderr, terminate = !exited)
            streamCleanupFailure = cleanupFailure
            if (interrupted || cleanupFailure is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            if (interrupted && primaryFailure == null) throw InterruptedException("Command cancelled")
            if (cleanupFailure is InterruptedException) {
                if (primaryFailure == null) throw cleanupFailure
                primaryFailure.addSuppressed(cleanupFailure)
            }
        }
    }

    private fun finish(
        process: Process,
        streams: ExecuteStreamHandler,
        stdout: EofInputStream,
        stderr: EofInputStream,
        terminate: Boolean
    ): Throwable? {
        var future: Future<*>? = null
        return try {
            future = submitCleanup {
                if (terminate) process.destroyForcibly()
                if (streams is PumpStreamHandler) streams.setStopTimeout(streamCleanupTimeoutMillis)
                try {
                    streams.stop()
                } finally {
                    // Closing a pipe while a native read is pending can block on Windows.
                    // EOF is stronger evidence than stop() returning (which can time out).
                    if (stdout.eof) stdout.close()
                    if (stderr.eof) stderr.close()
                    process.outputStream.close()
                }
            }
            future.get(streamCleanupTimeoutMillis, TimeUnit.MILLISECONDS)
            null
        } catch (failure: ExecutionException) {
            failure.cause ?: failure
        } catch (failure: Exception) {
            failure
        } finally {
            // Cancels Java waits, not native I/O. Bounded daemon threads provide the backstop.
            if (future != null && !future.isDone) future.cancel(true)
        }
    }

    private class EofInputStream(input: InputStream) : FilterInputStream(input) {
        @Volatile
        var eof = false
            private set

        override fun read(): Int = super.read().also { if (it == -1) eof = true }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            `in`.read(buffer, offset, length).also { if (it == -1) eof = true }
    }

    companion object {
        private val cleanupThreadId = AtomicInteger()
        private val cleanupSlots = Semaphore(4)

        private fun submitCleanup(action: () -> Unit): Future<*> {
            if (!cleanupSlots.tryAcquire()) throw RejectedExecutionException("Stream cleanup capacity exhausted")
            val future = FutureTask(action, Unit)
            // Fresh threads inherit this command's logging context; a reused pool retains an older task's context.
            val thread = Thread({
                try { future.run() } finally { cleanupSlots.release() }
            }, "command-stream-cleanup-${cleanupThreadId.incrementAndGet()}").apply { isDaemon = true }
            try { thread.start() } catch (e: Throwable) { cleanupSlots.release(); throw e }
            return future
        }
    }
}