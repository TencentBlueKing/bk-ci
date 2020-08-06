package com.tencent.devops.common.api.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatcherTest {

    @Test
    fun start() {
        val watcher = Watcher("hello")
        Thread.sleep(10)
        watcher.start()
        assertTrue(watcher.isRunning)
        watcher.start("cde")
        assertTrue(watcher.isRunning)
        assertTrue(watcher.elapsed() > watcher.totalTimeMillis)
        watcher.start()
        assertTrue(watcher.isRunning)
        println(watcher.toString())
        assertFalse(watcher.isRunning)
        watcher.stop() // 即使running = false 也不会有异常
    }

    @Test
    fun testStart() {
    }
}