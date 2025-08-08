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
package com.tencent.devops.common.util

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadPoolUtil {
    fun getThreadPoolExecutor(
        corePoolSize: Int = 1,
        maximumPoolSize: Int = 1,
        keepAliveTime: Long = 0,
        unit: TimeUnit = TimeUnit.SECONDS,
        threadNamePrefix: String,
        queue: LinkedBlockingQueue<Runnable> = LinkedBlockingQueue(1),
        handler: RejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy()
    ): ThreadPoolExecutor {
        val threadFactory = BasicThreadFactory.Builder().namingPattern(threadNamePrefix).build()
        return ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            queue,
            threadFactory,
            handler
        )
    }

    /**
     * 注意: executor建议定义成成员变量，避免每次都创建,如果定义局部变量,在使用完后应在finally调用shutdown方法关闭,避免线程暴涨
     */
    fun submitAction(
        executor: ThreadPoolExecutor = defaultExecutor,
        actionTitle: String,
        action: () -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val bizId = MDC.get(BIZID)
        executor.submit {
            try {
                MDC.put(BIZID, bizId)
                logger.info("start thread action [$actionTitle]")
                action.invoke()
                logger.info(
                    "finish thread action [$actionTitle] | time cost: ${System.currentTimeMillis() - startTime}"
                )
            } finally {
                MDC.remove(BIZID)
            }
        }
    }

    private val logger = LoggerFactory.getLogger(ThreadPoolUtil::class.java)
    private val defaultExecutor = getThreadPoolExecutor(threadNamePrefix = "thread-action-%d")
    private const val BIZID = "bizId"
}
