/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.storage.monitor

import com.tencent.bkrepo.common.storage.config.UploadProperties
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random

internal class StorageHealthMonitorTest {

    private val uploadProperties: UploadProperties = UploadProperties(location = "temp")
    private val storageCredentials = FileSystemCredentials(upload = uploadProperties)
    private val monitorConfig: MonitorProperties = MonitorProperties(
        enabled = true,
        fallbackLocation = "temp-fallback",
        interval = Duration.ofSeconds(5),
        timeout = Duration.ofSeconds(5),
        timesToRestore = 5,
        timesToFallback = 2
    )
    private val storageProperties = StorageProperties(filesystem = storageCredentials, monitor = monitorConfig)
    private val path = storageCredentials.upload.location

    @Test
    fun testCheck() {
        val monitor = StorageHealthMonitor(storageProperties, path)
        TimeUnit.SECONDS.sleep(10)
        monitor.stop()
    }

    @Test
    fun testRefresh() {
        val config = MonitorProperties(
            enabled = true,
            fallbackLocation = "temp-fallback",
            interval = Duration.ofSeconds(5),
            timeout = Duration.ofSeconds(10),
            timesToRestore = 5,
            timesToFallback = 2
        )
        val storageProperties = StorageProperties(filesystem = storageCredentials, monitor = config)
        val monitor = StorageHealthMonitor(storageProperties, path)
        repeat(2) {
            monitor.add(object : StorageHealthMonitor.Observer {
                override fun unhealthy(fallbackPath: Path?, reason: String?) {
                    println("unhealthy, fallbackPath: $fallbackPath, reason: $reason")
                }
                override fun restore(monitor: StorageHealthMonitor) {
                    println("restore")
                }
            })
        }
        // 4s, check 1 time
        TimeUnit.SECONDS.sleep(4)
        Assertions.assertTrue(monitor.healthy.get())

        println("Change to 1 nano")
        config.timeout = Duration.ofNanos(1)

        // 5s, check n time
        TimeUnit.SECONDS.sleep(3)
        // should change to unhealthy
        Assertions.assertFalse(monitor.healthy.get())

        println("Change to 10 second")
        config.timeout = Duration.ofSeconds(10)

        // 24s, check 4 time
        TimeUnit.SECONDS.sleep(20)
        // should keep unhealthy
        Assertions.assertFalse(monitor.healthy.get())

        // 24 + 2s, check 5 time
        TimeUnit.SECONDS.sleep(2)
        // should restore healthy
        Assertions.assertTrue(monitor.healthy.get())

        monitor.stop()
    }

    @Test
    fun testThrowExceptionInListener() {
        val config = MonitorProperties(
            enabled = true,
            fallbackLocation = "temp-fallback",
            interval = Duration.ofSeconds(5),
            timeout = Duration.ofNanos(10),
            timesToRestore = 2,
            timesToFallback = 1
        )
        val storageProperties = StorageProperties(filesystem = storageCredentials, monitor = config)
        val monitor = StorageHealthMonitor(storageProperties, path)
        monitor.add(object : StorageHealthMonitor.Observer {
            override fun unhealthy(fallbackPath: Path?, reason: String?) {
                println("unhealthy, fallbackPath: $fallbackPath, reason: $reason")
                throw IOException("simulate exception")
            }

            override fun restore(monitor: StorageHealthMonitor) {
                println("restore")
            }
        })
        config.timeout = Duration.ofSeconds(10)
        TimeUnit.SECONDS.sleep(10)
        monitor.stop()
    }

    @DisplayName("测试并发情况下的观察者的变更与通知")
    @Test
    fun testConcurrentOperateObservers() {
        val monitor = StorageHealthMonitor(storageProperties, path)
        val observer = object : StorageHealthMonitor.Observer {
            override fun unhealthy(fallbackPath: Path?, reason: String?) {
                println("change to unhealthy")
            }

            override fun restore(monitor: StorageHealthMonitor) {
                println("restore")
            }
        }
        val operateThreadNum = 10
        val latch = CountDownLatch(operateThreadNum + 1)
        // 模拟监控线程
        thread {
            assertDoesNotThrow {
                repeat(10) { idx ->
                    println("notify $idx")
                    monitor.notifyObservers {
                        it.unhealthy(null, null)
                    }
                    TimeUnit.NANOSECONDS.sleep(Random.nextLong(100, 500))
                    monitor.notifyObservers {
                        it.restore(monitor)
                    }
                }
            }
            latch.countDown()
        }
        // 模拟业务操作线程
        repeat(operateThreadNum) {
            thread {
                assertDoesNotThrow {
                    println("add observer $it")
                    monitor.add(observer)
                    TimeUnit.NANOSECONDS.sleep(Random.nextLong(500, 1000))
                    println("remove observer $it")
                    monitor.remove(observer)
                }
                latch.countDown()
            }
        }
        latch.await()
    }
}
