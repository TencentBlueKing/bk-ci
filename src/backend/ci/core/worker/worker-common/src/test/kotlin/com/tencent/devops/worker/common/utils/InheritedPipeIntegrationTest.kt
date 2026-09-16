package com.tencent.devops.worker.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.PumpStreamHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Real OS pipes: the direct child exits while a grandchild still owns stdout/stderr. */
class InheritedPipeIntegrationTest {
    @Test
    fun testReturnsBeforeGrandchildClosesPipesForSuccessAndFailure() {
        for (exitCode in listOf(0, 23)) {
            val directory = Files.createTempDirectory("inherited-pipe-").toFile()
            val ready = File(directory, "ready")
            val release = File(directory, "release")
            val pool = Executors.newSingleThreadExecutor { r -> Thread(r).apply { isDaemon = true } }
            val executor = CommandLineExecutor(300)
            executor.streamHandler = PumpStreamHandler(ByteArrayOutputStream(), ByteArrayOutputStream())
            val java = File(System.getProperty("java.home"), "bin/java").absolutePath
            val classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"))
            val command = CommandLine(java).addArguments(
                arrayOf("-cp", classpath, InheritedPipeChild::class.java.name, "parent", directory.path, "$exitCode"),
                false
            )
            var leaf: ProcessHandle? = null
            try {
                val future = pool.submit<Int> { executor.execute(command) }
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
                while (!ready.isFile && System.nanoTime() < deadline) Thread.sleep(20)
                assertTrue(ready.isFile, "Grandchild did not start")
                leaf = ProcessHandle.of(ready.readText().trim().toLong()).orElseThrow()
                assertEquals(exitCode, future.get(4, TimeUnit.SECONDS).toInt())
                assertTrue(leaf.isAlive, "Executor must finish while inherited pipe is still open")
                assertNotNull(executor.streamCleanupFailure)
            } finally {
                release.writeText("release")
                leaf?.let {
                    try { it.onExit().get(5, TimeUnit.SECONDS) } catch (e: Exception) { it.destroyForcibly() }
                }
                pool.shutdownNow()
                directory.deleteRecursively()
            }
        }
    }
}

object InheritedPipeChild {
    @JvmStatic
    fun main(args: Array<String>) {
        val directory = File(args[1])
        val ready = File(directory, "ready")
        val release = File(directory, "release")
        if (args[0] == "parent") {
            ProcessBuilder(
                File(System.getProperty("java.home"), "bin/java").absolutePath,
                "-cp", System.getProperty("java.class.path"), InheritedPipeChild::class.java.name,
                "leaf", directory.path
            ).inheritIO().start()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (!ready.isFile && System.nanoTime() < deadline) Thread.sleep(20)
            exitProcess(if (ready.isFile) args[2].toInt() else 99)
        }
        println("inherited stdout")
        System.err.println("inherited stderr")
        val pending = File(directory, "ready.tmp")
        pending.writeText(ProcessHandle.current().pid().toString())
        Files.move(pending.toPath(), ready.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!release.isFile && System.nanoTime() < deadline) Thread.sleep(20)
    }
}
