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

package com.tencent.devops.sign.service

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.jmx.SignBean
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class AsyncSignService(
    private val signService: SignService,
    private val signInfoService: SignInfoService,
    private val signBean: SignBean
) : DisposableBean {

    @Value("\${bkci.sign.taskPoolSize:#{null}}")
    private val taskPoolSize: Int? = null

    @Value("\${bkci.sign.taskQueueSize:#{null}}")
    private val taskQueueSize: Int? = null

    // 线程池队列和线程上限保持一致，并保持有一个活跃线程
    private val signExecutorService = ThreadPoolExecutor(
        taskPoolSize ?: DEFAULT_TASK_POOL_SIZE,
        taskPoolSize ?: DEFAULT_TASK_POOL_SIZE,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(taskQueueSize ?: DEFAULT_TASK_QUEUE_SIZE)
    )

    fun asyncSign(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaFile: File,
        taskExecuteCount: Int
    ) {
        try {
            signExecutorService.execute {
                val start = LocalDateTime.now()
                logger.info("[$resignId] asyncSign start")
                val success = signService.signIpaAndArchive(
                    resignId = resignId,
                    ipaSignInfo = ipaSignInfo,
                    ipaFile = ipaFile,
                    taskExecuteCount = taskExecuteCount
                )
                logger.info("[$resignId] asyncSign finished with success:$success")
                signBean.signTaskFinish(
                    elapse = LocalDateTime.now().timestampmilli() - start.timestampmilli(),
                    success = success
                )
            }
        } catch (e: RejectedExecutionException) {
            // 失败结束签名逻辑
            signInfoService.failResign(
                resignId = resignId,
                info = ipaSignInfo,
                executeCount = taskExecuteCount,
                message = "Sign service queue tasks exceed the limit: ${e.message}"
            )
            // 异步处理，所以无需抛出异常
            logger.error("[$resignId] asyncSign failed: $e")
        } catch (ignore: Throwable) {
            // 失败结束签名逻辑
            signInfoService.failResign(
                resignId = resignId,
                info = ipaSignInfo,
                executeCount = taskExecuteCount,
                message = ignore.message ?: "Start async sign task with exception"
            )
            // 异步处理，所以无需抛出异常
            logger.error("[$resignId] asyncSign failed: $ignore")
        }
    }

    override fun destroy() {
        // 当有签名任务执行时，阻塞服务的退出
        signExecutorService.shutdown()
        while (!signExecutorService.awaitTermination(EXECUTOR_DESTROY_AWAIT_SECOND, TimeUnit.SECONDS)) {
            logger.warn("SignTaskBean still has sign tasks.")
        }
    }

    @Scheduled(cron = "0/30 * *  * * ? ")
    fun flushTaskStatus() {
        logger.info("SIGN|signExecutorService|activeCount=${signExecutorService.activeCount}" +
            "|taskCount=${signExecutorService.taskCount}|queueSize=${signExecutorService.queue.size}")
        signBean.flushStatus(
            activeCount = signExecutorService.activeCount,
            taskCount = signExecutorService.taskCount,
            queueSize = signExecutorService.queue.size
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncSignService::class.java)
        const val DEFAULT_TASK_POOL_SIZE = 10
        const val DEFAULT_TASK_QUEUE_SIZE = 5
        const val EXECUTOR_DESTROY_AWAIT_SECOND = 5L
    }
}
