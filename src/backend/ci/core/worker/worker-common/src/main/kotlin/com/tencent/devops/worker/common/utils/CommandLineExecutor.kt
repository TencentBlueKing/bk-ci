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
 * 将命令退出与管道收尾分开：waitFor 返回只说明直接子进程已退出，后代仍可能持有 stdout/stderr。
 * Windows 上，此时同步 close 可能等待另一个线程的 native read，导致已经结束的命令永久无法返回。
 *
 * 收尾使用有并发上限的守护线程，并限制调用方等待时间；后代进程的清理仍由 Runner 负责。
 * 此算法与 run 插件的同名执行器保持一致，修改时应同步检查两端的生命周期与继承管道回归测试；
 * 两端各自包含实现，不构成必须同步发布的版本依赖。
 *
 * @param streamCleanupTimeoutMillis 命令结束后的收尾等待上限，不是命令运行超时。
 */
open class CommandLineExecutor @JvmOverloads constructor(
    private val streamCleanupTimeoutMillis: Long = 10_000L
) : DefaultExecutor() {
    init {
        require(streamCleanupTimeoutMillis > 0)
    }

    /** 收尾超时、异常或容量不足单独交给调用方诊断，不替换命令退出码和原始执行异常。 */
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
            // waitFor 抛出 InterruptedException 时会清除中断标记；其他阶段则可能仍保留标记。
            // 暂时清除后再等待清理，避免 Future.get 立即中断而跳过等待；最后恢复，继续向上传递取消。
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
                // destroyForcibly 也可能触发底层管道关闭，因此不能放回调用线程；流启动失败同样走这里。
                if (terminate) process.destroyForcibly()
                if (streams is PumpStreamHandler) streams.setStopTimeout(streamCleanupTimeoutMillis)
                try {
                    streams.stop()
                } finally {
                    // stop 可能因超时/中断返回，不代表读取线程已结束；只有读到 EOF 才能安全关闭输入管道。
                    // 未到 EOF 时留给后代退出或进程清理释放句柄，不能在这里补一个无条件 close。
                    if (stdout.eof) stdout.close()
                    if (stderr.eof) stderr.close()
                    process.outputStream.close()
                }
            }
            // 外层期限覆盖销毁进程、stop 和 close 的整个动作，不能仅依赖 PumpStreamHandler 的超时。
            future.get(streamCleanupTimeoutMillis, TimeUnit.MILLISECONDS)
            null
        } catch (failure: ExecutionException) {
            failure.cause ?: failure
        } catch (failure: Exception) {
            failure
        } finally {
            // cancel 只能中断可响应中断的 Java 等待，不能保证终止 native I/O；守护线程避免拖住 JVM 退出。
            if (future != null && !future.isDone) future.cancel(true)
        }
    }

    /** EOF 由 Commons Exec 的读取线程写入、清理线程读取；必须保证跨线程可见。 */
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
        // native I/O 可能长期不返回：限制实际存活的清理线程，不排队积压清理请求。
        private val cleanupSlots = Semaphore(4)

        private fun submitCleanup(action: () -> Unit): Future<*> {
            if (!cleanupSlots.tryAcquire()) throw RejectedExecutionException("Stream cleanup capacity exhausted")
            val future = FutureTask(action, Unit)
            // 新线程继承本次命令的日志/执行上下文；复用线程池线程可能携带上一任务的 InheritableThreadLocal。
            // 槽位必须在线程真正退出时释放，不能在 Future 取消或调用方超时返回时提前释放。
            val thread = Thread({
                try { future.run() } finally { cleanupSlots.release() }
            }, "command-stream-cleanup-${cleanupThreadId.incrementAndGet()}").apply { isDaemon = true }
            try { thread.start() } catch (e: Throwable) { cleanupSlots.release(); throw e }
            return future
        }
    }
}