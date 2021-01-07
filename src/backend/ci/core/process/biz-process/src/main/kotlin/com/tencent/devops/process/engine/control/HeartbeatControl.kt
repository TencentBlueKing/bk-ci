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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class HeartbeatControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    companion object {
        private const val REDIS_EXPIRED_MIN = 30L // redis expired time in 30 minutes
        private const val TIMEOUT_IN_MS = 2 * 60 * 1000 // timeout in 2 minutes
        private val logger = LoggerFactory.getLogger(HeartbeatControl::class.java)
    }

    fun detectHeartbeat(event: PipelineContainerAgentHeartBeatEvent) {
        val lastUpdate = redisOperation.get(HeartBeatUtils.genHeartBeatKey(event.buildId, event.containerId))
            ?: run {
                logger.info("[${event.buildId}]|HEART_BEAT_MONITOR_CANCEL|Job#${event.containerId}")
                return
            }

        val buildInfo = pipelineRuntimeService.getBuildInfo(event.buildId)
        if (buildInfo == null || buildInfo.status.isFinish()) {
            logger.info("[${event.buildId}]|HEART_BEAT_MONITOR_FINISH|The build has been finish(${buildInfo?.status})")
            return
        }

        val elapse = System.currentTimeMillis() - lastUpdate.toLong()
        if (elapse > TIMEOUT_IN_MS) {
            logger.warn("[${event.buildId}]|HEART_BEAT_MONITOR_OT|The build is timeout for ${elapse}ms, terminate it")

            val container = pipelineRuntimeService.getContainer(
                buildId = event.buildId,
                stageId = null,
                containerId = event.containerId
            ) ?: run {
                logger.warn("[${event.buildId}]|HEART_BEAT_MONITOR_EXIT|can not find Job#${event.containerId}")
                return
            }

            var found = false
            // #2365 在运行中的插件中记录心跳超时信息
            val runningTask =
                pipelineRuntimeService.getRunningTask(projectId = event.projectId, buildId = event.buildId)
            runningTask.forEach { taskMap ->
                if (event.containerId == taskMap["containerId"] && taskMap["taskId"] != null) {
                    found = true
                    val executeCount = taskMap["executeCount"]?.toString()?.toInt() ?: 1
                    buildLogPrinter.addRedLine(
                        buildId = event.buildId,
                        message =
                        "Agent心跳超时/Agent's heartbeat has been lost(${TimeUnit.MILLISECONDS.toSeconds(elapse)} sec)",
                        tag = taskMap["taskId"].toString(),
                        jobId = event.containerId,
                        executeCount = executeCount
                    )

                    // #2952 心跳超时场景：因用户在使用插件时，可能因进行测试，编译占用过量资源导致Agent进程被系统级联杀死
                    // 归类于插件执行错误。同时插件需要进行优化限制，防止被过量使用。
                    pipelineRuntimeService.setTaskErrorInfo(
                        buildId = event.buildId,
                        taskId = taskMap["taskId"].toString(),
                        errorType = ErrorType.THIRD_PARTY,
                        errorCode = ErrorCode.THIRD_PARTY_BUILD_ENV_ERROR,
                        errorMsg = "Agent心跳超时/Agent Dead，请检查构建机状态"
                    )
                }
            }

            if (!found) {
                // #2365 在Set Up Job位置记录心跳超时信息
                buildLogPrinter.addRedLine(
                    buildId = event.buildId,
                    message =
                    "Agent心跳超时/Agent's heartbeat has been lost(${TimeUnit.MILLISECONDS.toSeconds(elapse)} sec)",
                    tag = com.tencent.devops.process.engine.common.VMUtils.genStartVMTaskId(event.containerId),
                    jobId = event.containerId,
                    executeCount = container.executeCount
                )
            }

            // 终止当前容器下的任务
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(
                    source = "heartbeat_timeout",
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    stageId = container.stageId,
                    containerId = event.containerId,
                    containerType = container.containerType,
                    actionType = ActionType.TERMINATE,
                    reason = "构建任务对应的Agent的心跳超时，请检查Agent的状态"
                )
            )
        } else {
            logger.info("[${event.buildId}]|HEART_BEAT_MONITOR_LOOP|${event.source}|Job#${event.containerId}")
            // 正常是继续循环检查当前消息
            pipelineEventDispatcher.dispatch(event)
        }
    }

    fun addHeartBeat(buildId: String, vmSeqId: String, time: Long, retry: Int = 3) {
        try {
            redisOperation.set(
                key = HeartBeatUtils.genHeartBeatKey(buildId = buildId, vmSeqId = vmSeqId),
                value = time.toString(),
                expiredInSecond = TimeUnit.MINUTES.toSeconds(REDIS_EXPIRED_MIN)
            )
        } catch (ignored: Throwable) {
            if (retry > 0) {
                logger.warn("[$buildId]|Fail to set heart beat variable(Job#$vmSeqId -> $time)", ignored)
                addHeartBeat(buildId = buildId, vmSeqId = vmSeqId, time = time, retry = retry - 1)
            } else {
                throw ignored
            }
        }
    }

    fun dispatchHeartbeatEvent(buildInfo: BuildInfo, containerId: String) {
        pipelineEventDispatcher.dispatch(
            PipelineContainerAgentHeartBeatEvent(
                source = "buildVMStarted",
                projectId = buildInfo.projectId,
                pipelineId = buildInfo.pipelineId,
                userId = buildInfo.startUser,
                buildId = buildInfo.buildId,
                containerId = containerId
            )
        )
    }

    fun dropHeartbeat(buildId: String, vmSeqId: String) {
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId))
    }
}
