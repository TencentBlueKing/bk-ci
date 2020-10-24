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

package com.tencent.bkrepo.common.stream.binder.memory.queue

import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class MessageWorkHandler(private val queue: BlockingQueue<QueueItem>) : Runnable {

    private var isRunning = false

    fun stop() {
        isRunning = false
    }

    @Suppress("TooGenericExceptionCaught")
    override fun run() {
        isRunning = true
        while (isRunning) {
            val item = queue.poll(5, TimeUnit.SECONDS)
            if (item != null) {
                try {
                    val consumers = MemoryListenerContainer.findListener(item.destination)
                    consumers.forEach {
                        it.accept(item.message)
                    }
                } catch (e: Throwable) {
                    logger.error("Memory cloud stream handle fault.", e)
                    if (e is VirtualMachineError) {
                        this.stop()
                    }
                }
            } else {
                Thread.sleep(100)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MemoryMessageQueue::class.java)
    }
}
