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

package com.tencent.bkrepo.common.stream.binder.memory.queue

import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MemoryMessageQueue {

    private var startingSync = Any()
    private var isRunning: Boolean = false
    private var queue: LinkedBlockingQueue<QueueItem>? = null
    private var handlers: List<MessageWorkHandler>? = null

    fun start(queueSize: Int, workerPoolSize: Int? = null) {
        if (!isRunning) {
            synchronized(startingSync) {
                if (!isRunning) {
                    val defaultSize = workerPoolSize ?: -1
                    val concurrentLevel =
                        if (defaultSize <= 0) {
                            (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                        } else {
                            defaultSize
                        }

                    queue = LinkedBlockingQueue(queueSize)
                    this.startHandlers(concurrentLevel)
                    Runtime.getRuntime().addShutdownHook(
                        Thread {
                            this.shutdown()
                        }
                    )
                    isRunning = true
                    logger.info("Cloud stream memory queue was started ( qs: $queueSize, ps: $concurrentLevel ).")
                }
            }
        }
    }

    private fun startHandlers(poolSize: Int) {
        this.handlers = this.queue?.run {
            (0 until poolSize).map {
                val handler = MessageWorkHandler(this)
                threadFactory.newThread(handler).start()
                handler
            }
        }
    }

    fun shutdown() {
        if (isRunning) {
            synchronized(startingSync) {
                if (isRunning) {
                    this.handlers?.forEach {
                        it.stop()
                    }
                    logger.info("Cloud stream memory queue was stopped.")
                    isRunning = false
                }
            }
        }
    }

    fun produce(destination: String, message: Message<*>) {
        require(isRunning) { "MemoryMessageQueue is not running." }
        var added = false
        while (!added) {
            added = queue?.offer(QueueItem(message, destination), 2, TimeUnit.SECONDS) ?: true
            if (!added) {
                queue?.poll()
                logger.warn("Message queue was full, the earlier messages have been dropped.")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MemoryMessageQueue::class.java)
        private val index: AtomicLong = AtomicLong()
        private val threadFactory = ThreadFactory {
            Thread(it).apply {
                this.isDaemon = true
                this.name = "stream-memory-queue-poll-${index.incrementAndGet()}"
            }
        }
        val instance: MemoryMessageQueue by lazy { MemoryMessageQueue() }
    }
}
