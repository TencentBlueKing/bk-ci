/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.storage.monitor

import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.util.toPath
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class StorageHealthMonitor(
    storageProperties: StorageProperties
) {
    var health: AtomicBoolean = AtomicBoolean(true)
    var reason: String? = null
    private val monitorConfig = storageProperties.monitor
    private val storageCredentials = storageProperties.defaultStorageCredentials()
    private val executorService = Executors.newSingleThreadExecutor()
    private val observerList = mutableListOf<Observer>()
    private var healthyThroughputCount: AtomicInteger = AtomicInteger(0)
    private var unhealthyThroughputCount: AtomicInteger = AtomicInteger(0)

    init {
        require(!monitorConfig.timeout.isNegative && !monitorConfig.timeout.isZero)
        require(!monitorConfig.interval.isNegative && !monitorConfig.interval.isZero)
        require(monitorConfig.timesToRestore > 0)
        Files.createDirectories(getPrimaryPath())
        monitorConfig.fallbackLocation?.let { Files.createDirectories(Paths.get(it)) }
        start()
        logger.info("Start up storage monitor for path[${storageCredentials.upload.location}]")
    }

    private fun start() {
        thread {
            while (true) {
                var sleep = true
                if (monitorConfig.enabled) {
                    val checker = StorageHealthChecker(getPrimaryPath(), monitorConfig.dataSize)
                    val future = executorService.submit(checker)
                    sleep = try {
                        future.get(monitorConfig.timeout.seconds, TimeUnit.SECONDS)
                        changeToHealthy()
                        true
                    } catch (timeoutException: TimeoutException) {
                        changeToUnhealthy(IO_TIMEOUT_MESSAGE)
                    } catch (exception: IOException) {
                        changeToUnhealthy(exception.message.orEmpty())
                    } finally {
                        checker.clean()
                    }
                }

                if (sleep) {
                    TimeUnit.SECONDS.sleep(monitorConfig.interval.seconds)
                }
            }
        }
    }

    fun stop() {
        monitorConfig.enabled = false
    }

    fun add(observer: Observer) {
        observerList.add(observer)
    }

    fun remove(observer: Observer?) {
        observerList.remove(observer)
    }

    fun getFallbackPath(): Path? = monitorConfig.fallbackLocation?.toPath()

    private fun getPrimaryPath(): Path = storageCredentials.upload.location.toPath()

    private fun changeToUnhealthy(message: String): Boolean {
        var sleep = true
        healthyThroughputCount.set(0)
        val count = unhealthyThroughputCount.incrementAndGet()
        if (health.get()) {
            // 如果当前是健康状态，不睡眠立即检查
            sleep = false
            logger.warn("Path[${getPrimaryPath()}] check failed [$count/${monitorConfig.timesToFallback}].")
        }

        if (count >= monitorConfig.timesToFallback) {
            if (health.compareAndSet(true, false)) {
                logger.error("Path[${getPrimaryPath()}] change to unhealthy, reason: $reason")
                reason = message
                for (observer in observerList) {
                    observer.unhealthy(getFallbackPath(), reason)
                }
                sleep = true
            }
        }
        return sleep
    }

    private fun changeToHealthy() {
        unhealthyThroughputCount.set(0)
        val count = healthyThroughputCount.incrementAndGet()
        if (!health.get()) {
            logger.warn("Try to restore [$count/${monitorConfig.timesToRestore}].")
        }

        if (count >= monitorConfig.timesToRestore) {
            if (health.compareAndSet(false, true)) {
                logger.info("Path[${getPrimaryPath()}] restore healthy.")
                for (observer in observerList) {
                    observer.restore(this)
                }
            }
        }
    }

    companion object {
        const val IO_TIMEOUT_MESSAGE = "IO Delay"
        private val logger = LoggerFactory.getLogger(StorageHealthMonitor::class.java)
    }

    interface Observer {
        fun unhealthy(fallbackPath: Path?, reason: String?)
        fun restore(monitor: StorageHealthMonitor) {}
    }
}
