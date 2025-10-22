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

package com.tencent.devops.worker.common.heartbeat

import com.tencent.devops.common.api.constant.HTTP_500
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.EngineService
import com.tencent.devops.worker.common.task.TaskExecutorCache
import com.tencent.devops.worker.common.utils.KillBuildProcessTree
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object Heartbeat {
    private const val EXIT_AFTER_FAILURE = 12 // Worker will exit after 12 fail heart
    private val logger = LoggerFactory.getLogger(Heartbeat::class.java)
    private val executor = Executors.newScheduledThreadPool(2)
    private var running = false
    private val task2ProgressRate = mutableMapOf<String, Double>()

    @Synchronized
    fun start(jobTimeoutMills: Long = TimeUnit.MINUTES.toMillis(900), executeCount: Int = 1) {
        if (running) {
            logger.warn("The heartbeat task already started")
            return
        }
        var failCnt = 0
        executor.scheduleWithFixedDelay({
            if (running) {
                try {
                    logger.info("Start to do the heartbeat")
                    val heartBeatInfo = EngineService.heartbeat(
                        executeCount = executeCount,
                        jobHeartbeatRequest = JobHeartbeatRequest(
                            task2ProgressRate = task2ProgressRate
                        )
                    )
                    val cancelTaskIds = heartBeatInfo.cancelTaskIds
                    if (!cancelTaskIds.isNullOrEmpty()) {
                        // 启动线程杀掉取消任务对应的进程
                        Thread(KillCancelTaskProcessRunnable(heartBeatInfo)).start()
                    }
                    failCnt = 0
                } catch (e: Exception) {
                    logger.warn("Fail to do the heartbeat", e)
                    if (e is RemoteServiceException) {
                        handleRemoteServiceException(e)
                    }
                    failCnt++
                    if (failCnt >= EXIT_AFTER_FAILURE) {
                        logger.error("Heartbeat has been failed for $failCnt times, worker exit")
                        exitProcess(-1)
                    }
                }
            }
        }, 10, 2, TimeUnit.SECONDS)

        /*
            #2043 由worker-agent.jar 运行时进行自监控，当达到Job超时时，自行上报错误信息并结束构建
         */
        executor.scheduleWithFixedDelay({
            if (running) {
                LoggerService.addErrorLine("Job timout: ${TimeUnit.MILLISECONDS.toMinutes(jobTimeoutMills)}min")
                EngineService.timeout()
                exitProcess(99)
            }
        }, jobTimeoutMills, jobTimeoutMills, TimeUnit.MILLISECONDS)
        running = true
    }

    private fun handleRemoteServiceException(ignored: RemoteServiceException) {

        if (ignored.httpStatus != HTTP_500 && ignored.responseContent.isNullOrBlank()) {
            return
        }
        // 流水线构建结束则正常结束进程，不再重试
        if (ignored.errorCode == 2101182) {
            logger.error("build end, worker exit")
            exitProcess(0)
        }
    }

    fun recordTaskProgressRate(
        taskId: String,
        progressRate: Double
    ) {
        task2ProgressRate[taskId] = progressRate
    }

    private class KillCancelTaskProcessRunnable(
        private val heartBeatInfo: HeartBeatInfo
    ) : Runnable {
        override fun run() {
            val buildId = heartBeatInfo.buildId
            logger.info("Heartbeat cancel build:$buildId,heartBeatInfo:$heartBeatInfo")
            val cancelTaskIds = heartBeatInfo.cancelTaskIds
            KillBuildProcessTree.killProcessTree(
                projectId = heartBeatInfo.projectId,
                buildId = buildId,
                vmSeqId = heartBeatInfo.vmSeqId,
                taskIds = cancelTaskIds,
                forceFlag = true
            )
            if (!cancelTaskIds.isNullOrEmpty()) {
                val taskExecutorMap = TaskExecutorCache.getAllPresent(cancelTaskIds)
                taskExecutorMap?.forEach { taskId, executor ->
                    logger.info("Heartbeat taskId[$taskId] executor shutdownNow")
                    executor.shutdownNow()
                    TaskExecutorCache.invalidate(taskId)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        running = false
        executor.shutdown()
    }
}
