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

import com.tencent.bkrepo.common.api.constant.StringPool.UNKNOWN
import com.tencent.bkrepo.common.api.util.HumanReadable.time
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.util.toPath
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 存储监控状态监控类，目前只支持监控默认存储实例
 * @param storageProperties 存储配置
 */
class StorageHealthMonitor(
    storageProperties: StorageProperties,
    val path: String,
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
) {
    /**
     * 表示存储实例是否健康
     */
    var healthy: AtomicBoolean = AtomicBoolean(true)

    /**
     * 异常原因
     */
    var fallBackReason: String? = null

    /**
     * 降级时间
     */
    var fallBackTime: Long = 0

    /**
     * 健康配置
     */
    private val monitorConfig = storageProperties.monitor

    /**
     * 观察者列表，当健康状况发生变化时会通知列表中的观察者
     */
    private val observerList = Collections.synchronizedList(mutableListOf<Observer>())

    /**
     * 记录当前连续检测成功次数
     */
    private val checkSuccessTimes: AtomicInteger = AtomicInteger(0)

    /**
     * 记录当前连续检测失败次数
     */
    private val checkFailedTimes: AtomicInteger = AtomicInteger(0)

    /**
     * 检查器
     */
    private val checker = StorageHealthChecker(getPrimaryPath(), monitorConfig.dataSize)

    init {
        require(!monitorConfig.timeout.isNegative && !monitorConfig.timeout.isZero)
        require(!monitorConfig.interval.isNegative && !monitorConfig.interval.isZero)
        require(monitorConfig.timesToRestore > 0)

        Files.createDirectories(getPrimaryPath())
        monitorConfig.fallbackLocation?.let { Files.createDirectories(Paths.get(it)) }
        start()
    }

    /**
     * 开始检测
     */
    private fun start() {
        thread {
            while (true) {
                if (monitorConfig.enabled) {
                    try {
                        val future = executorService.submit { checker.check() }
                        future.get(monitorConfig.timeout.seconds, TimeUnit.SECONDS)
                        onCheckSuccess()
                    } catch (ignored: TimeoutException) {
                        onCheckFailed(IO_TIMEOUT_MESSAGE)
                    } catch (exception: InterruptedException) {
                        onCheckFailed(INTERRUPTED_MESSAGE)
                    } catch (exception: CancellationException) {
                        onCheckFailed(CANCELLED_MESSAGE)
                    } catch (exception: ExecutionException) {
                        onCheckFailed(exception.cause?.message ?: UNKNOWN)
                    } catch (exception: Exception) {
                        onCheckFailed(exception.message ?: UNKNOWN)
                    } finally {
                        try {
                            val future = executorService.submit { checker.clean() }
                            future.get(1, TimeUnit.SECONDS)
                        } catch (exception: Exception) {
                            logger.warn("Clean checker error: $exception", exception)
                        }
                    }
                }

                // wait loop
                val interval = if (checkFailedTimes.get() > 0) 1 else monitorConfig.interval.seconds
                TimeUnit.SECONDS.sleep(interval)
            }
        }
        logger.info("Startup storage monitor for path[${getPrimaryPath()}]")
    }

    /**
     * 停止检测
     */
    fun stop() {
        monitorConfig.enabled = false
    }

    /**
     * 添加观察者
     */
    fun add(observer: Observer) {
        observerList.add(observer)
    }

    /**
     * 移除观察者
     */
    fun remove(observer: Observer?) {
        observerList.remove(observer)
    }

    /**
     * 获取降级存储路径
     */
    fun getFallbackPath(): Path? = monitorConfig.fallbackLocation?.toPath()

    /**
     * 获取主存储路径
     */
    private fun getPrimaryPath(): Path = path.toPath()

    /**
     * 检测失败处理逻辑
     * @param reason 失败原因
     */
    private fun onCheckFailed(reason: String) {
        fallBackReason = reason

        // 每次失败将成功数置零
        checkSuccessTimes.set(0)
        val times = checkFailedTimes.incrementAndGet()
        logger.warn("Path[${getPrimaryPath()}] check failed[$times/${monitorConfig.timesToFallback}]: $reason")

        // 当连续失败次数超过阈值，进行降级操作
        if (times >= monitorConfig.timesToFallback) {
            changeToUnhealthy()
        }
    }

    /**
     * 检测成功处理逻辑
     */
    private fun onCheckSuccess() {
        // 每次成功将失败次数置零
        checkFailedTimes.set(0)
        val times = checkSuccessTimes.incrementAndGet()
        if (!healthy.get()) {
            logger.info("Try to restore [$times/${monitorConfig.timesToRestore}].")
        }
        logger.debug("Path[${getPrimaryPath()}] check success")

        // 当连续失败次数超过阈值，进行降级操作
        if (times >= monitorConfig.timesToRestore) {
            restoreHealthy()
        }
    }

    private fun changeToUnhealthy() {
        // 修改状态
        if (healthy.compareAndSet(true, false)) {
            logger.error("Path[${getPrimaryPath()}] change to unhealthy, reason: $fallBackReason")
            fallBackTime = System.currentTimeMillis()
            // 通知观察者
            notifyObservers {
                try {
                    it.unhealthy(getFallbackPath(), fallBackReason)
                } catch (exception: Exception) {
                    logger.error("Failed to change observer: $exception", exception)
                }
            }
        }
    }

    fun notifyObservers(action: (Observer) -> Unit) {
        synchronized(observerList) {
            observerList.forEach { action(it) }
        }
    }

    /**
     * 存储恢复健康
     */
    private fun restoreHealthy() {
        if (healthy.compareAndSet(false, true)) {
            val duration = System.currentTimeMillis() - fallBackTime
            logger.info("Path[${getPrimaryPath()}] restore healthy, during: ${time(duration, TimeUnit.MILLISECONDS)}")
            // 通知观察者
            notifyObservers {
                try {
                    it.restore(this)
                } catch (exception: Exception) {
                    logger.error("Failed to restore observer: $exception", exception)
                }
            }
        }
    }

    companion object {
        const val IO_TIMEOUT_MESSAGE = "IO Delay"
        const val INTERRUPTED_MESSAGE = "Interrupted"
        const val CANCELLED_MESSAGE = "Cancelled"
        private val logger = LoggerFactory.getLogger(StorageHealthMonitor::class.java)
    }

    interface Observer {
        fun unhealthy(fallbackPath: Path?, reason: String?)
        fun restore(monitor: StorageHealthMonitor) {}
    }
}
