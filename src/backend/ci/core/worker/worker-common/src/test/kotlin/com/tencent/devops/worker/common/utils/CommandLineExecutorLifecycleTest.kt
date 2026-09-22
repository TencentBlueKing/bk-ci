package com.tencent.devops.worker.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.PumpStreamHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class CommandLineExecutorLifecycleTest {
    @Test
    fun testInheritedPipeKeepsExitCodeAndNeverClosesPendingRead() {
        val pipe = HeldPipe()
        val process = FakeProcess(pipe)
        val executor = fakeExecutor(process)
        executor.streamHandler = PumpStreamHandler(ByteArrayOutputStream())
        val pool = Executors.newSingleThreadExecutor { r -> Thread(r).apply { isDaemon = true } }
        try {
            val started = System.nanoTime()
            assertEquals(23, pool.submit<Int> { executor.execute(CommandLine("unused")) }.get(3, TimeUnit.SECONDS))
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 3000)
            assertNotNull(executor.streamCleanupFailure)
            assertFalse(pipe.closed)
        } finally {
            pipe.release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun testStopFailureDoesNotReplaceScriptExitCode() {
        val executor = fakeExecutor(FakeProcess())
        executor.streamHandler = object : PumpStreamHandler(ByteArrayOutputStream()) {
            override fun stop() { throw IOException("stop failed") }
        }
        assertEquals(23, executor.execute(CommandLine("unused")))
        assertEquals("stop failed", executor.streamCleanupFailure?.message)
    }

    @Test
    fun testStartFailureDestroysLaunchedProcess() {
        val process = FakeProcess()
        val executor = fakeExecutor(process)
        executor.streamHandler = object : PumpStreamHandler(ByteArrayOutputStream()) {
            override fun start() { throw IOException("start failed") }
        }
        try {
            executor.execute(CommandLine("unused"))
            fail<Unit>("Expected start failure")
        } catch (e: IOException) {
            assertEquals("start failed", e.message)
            assertTrue(process.destroyed)
        }
    }

    @Test
    fun testCancellationPropagatesAndRestoresInterrupt() {
        val waiting = CountDownLatch(1)
        val process = object : FakeProcess() {
            override fun waitFor(): Int { waiting.countDown(); CountDownLatch(1).await(); return 0 }
        }
        val executor = fakeExecutor(process)
        executor.streamHandler = PumpStreamHandler(ByteArrayOutputStream())
        val result = AtomicReference<Throwable?>()
        var interrupted = false
        val thread = Thread {
            try { executor.execute(CommandLine("unused")) } catch (e: Throwable) {
                interrupted = Thread.currentThread().isInterrupted
                result.set(e)
            }
        }.apply { isDaemon = true; start() }
        assertTrue(waiting.await(3, TimeUnit.SECONDS))
        thread.interrupt()
        thread.join(3000)
        assertFalse(thread.isAlive)
        assertTrue(result.get() is InterruptedException)
        assertTrue(interrupted)
        assertTrue(process.destroyed)
    }

    private fun fakeExecutor(process: Process) = object : CommandLineExecutor(200) {
        override fun launch(command: CommandLine, env: MutableMap<String, String>?, directory: File?): Process = process
    }

    private open class FakeProcess(private val stdout: InputStream = ByteArrayInputStream(byteArrayOf())) : Process() {
        @Volatile var destroyed = false
        override fun getInputStream() = stdout
        override fun getErrorStream() = ByteArrayInputStream(byteArrayOf())
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun waitFor() = 23
        override fun exitValue() = 23
        override fun destroy() { destroyed = true }
        override fun destroyForcibly(): Process { destroy(); return this }
    }

    private class HeldPipe : InputStream() {
        val release = CountDownLatch(1)
        @Volatile var closed = false
        override fun read(): Int {
            while (release.count > 0) {
                try { release.await() } catch (e: InterruptedException) { /* emulate native read */ }
            }
            return -1
        }
        override fun close() { closed = true; read() }
    }
}
