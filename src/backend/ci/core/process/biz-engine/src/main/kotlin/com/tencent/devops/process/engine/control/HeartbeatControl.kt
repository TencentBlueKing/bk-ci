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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
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
        private const val TIMEOUT_IN_MS = 10 * 60 * 1000 // timeout in 10 minutes
        private val LOG = LoggerFactory.getLogger(HeartbeatControl::class.java)
        private const val LOG_PER_TIMES = 5 // ?次打一次日志
    }

    fun detectHeartbeat(event: PipelineContainerAgentHeartBeatEvent) {
        val lastUpdate = redisOperation.get(HeartBeatUtils.genHeartBeatKey(event.buildId, event.containerId))
            ?: run {
                LOG.info("${event.buildId}|HEART_BEAT_MONITOR_CANCEL|j(${event.containerId})")
                return
            }

        val elapse = System.currentTimeMillis() - lastUpdate.toLong()
        if (elapse > TIMEOUT_IN_MS) {
            timeout(event, elapse)
        } else {
            if (Math.floorMod(event.retryTime++, LOG_PER_TIMES) == 1) {
                LOG.info("ENGINE|${event.buildId}|HEARTBEAT_MONITOR|" +
                    "${event.source}|j(${event.containerId})|loopTime=${event.retryTime}")
            }
            // 正常是继续循环检查当前消息
            pipelineEventDispatcher.dispatch(event)
        }
    }

    @Suppress("ReturnCount")
    private fun timeout(event: PipelineContainerAgentHeartBeatEvent, elapse: Long) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(event.buildId)
        if (buildInfo == null || buildInfo.status.isFinish()) {
            LOG.info("ENGINE|${event.buildId}|HEARTBEAT_MONITOR_FINISH|finish(${buildInfo?.status})")
            return
        }

        val container = pipelineRuntimeService.getContainer(buildId = event.buildId,
            stageId = null, containerId = event.containerId)
            ?: run {
                LOG.warn("ENGINE|${event.buildId}|HEARTBEAT_MONITOR_EXIT|can not find job j(${event.containerId})")
                return
            }

        // 心跳监测是定时的消息处理，当流水线当前结束，在此时间点内又进行重试，会导致上一次的心跳监测消息处理误判，增加次数判断
        if (container.executeCount != event.executeCount) {
            LOG.info("ENGINE|${event.buildId}|HEARTBEAT_MONITOR_EXIT|" +
                "executeCount(${event.executeCount} != ${container.executeCount})")
            return
        }

        var found = false
        // #2365 在运行中的插件中记录心跳超时信息
        val runningTask = pipelineRuntimeService.getRunningTask(container.buildId)
        runningTask.forEach { taskMap ->
            if (container.containerId == taskMap["containerId"] && taskMap["taskId"] != null) {
                found = true
                val executeCount = taskMap["executeCount"]?.toString()?.toInt() ?: 1
                buildLogPrinter.addRedLine(
                    buildId = container.buildId,
                    message =
                    "Agent心跳超时/Agent's heartbeat lost(${TimeUnit.MILLISECONDS.toSeconds(elapse)} sec)",
                    tag = taskMap["taskId"].toString(),
                    jobId = container.containerId,
                    executeCount = executeCount
                )
            }
        }

        if (!found) {
            // #2365 在Set Up Job位置记录心跳超时信息
            buildLogPrinter.addRedLine(
                buildId = container.buildId,
                message = "Agent心跳超时/Agent's heartbeat lost(${TimeUnit.MILLISECONDS.toSeconds(elapse)} sec)",
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = container.containerId,
                executeCount = container.executeCount
            )
        }

        // 终止当前容器下的任务
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "heartbeat_timeout",
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                userId = event.userId,
                buildId = container.buildId,
                stageId = container.stageId,
                containerId = container.containerId,
                containerType = container.containerType,
                actionType = ActionType.TERMINATE,
                reason = "Agent心跳超时/Agent Dead，请检查构建机状态",
                errorTypeName = ErrorType.THIRD_PARTY.name,
                errorCode = ErrorCode.THIRD_PARTY_BUILD_ENV_ERROR
            )
        )
    }
}
