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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.heartbeat

import com.tencent.devops.worker.common.service.ProcessService
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object Heartbeat {
    private val EXIT_AFTER_FAILURE = 12 // Worker will exist after 12 fail heart
    private val logger = LoggerFactory.getLogger(Heartbeat::class.java)
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var running = false

    @Synchronized
    fun start() {
        if (running) {
            logger.warn("The heartbeat task already started")
            return
        }
        var failCnt = 0
        running = true
        executor.scheduleWithFixedDelay({
            if (running) {
                try {
                    logger.info("Start to do the heartbeat")
                    ProcessService.heartbeat()
                    failCnt = 0
                } catch (e: Exception) {
                    logger.warn("Fail to do the heartbeat", e)
                    failCnt++
                    if (failCnt >= EXIT_AFTER_FAILURE) {
                        logger.error("Heartbeat has been failed for $failCnt times, worker exit")
                        exitProcess(-1)
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS)
    }

    @Synchronized
    fun stop() {
        running = false
        executor.shutdown()
    }
}