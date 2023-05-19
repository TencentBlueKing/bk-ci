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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TIP_MESSAGE
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HeartbeatControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    companion object {
        private const val TIMEOUT_IN_MS = 10 * 60 * 1000 // timeout in 10 minutes
        private const val CANCEL_TIMEOUT_IN_MS = 20000 // 取消构建超时时间为20秒
        private val LOG = LoggerFactory.getLogger(HeartbeatControl::class.java)
        private const val LOG_PER_TIMES = 5 // ?次打一次日志
    }

    fun detectHeartbeat(event: PipelineContainerAgentHeartBeatEvent) {
        val buildId = event.buildId
        val containerId = event.containerId
        val executeCount = event.executeCount
        // 为了兼容旧版agent没传executeCount的情况，用带executeCount的key去redis取不到值再用不带executeCount的key去取一遍
        val lastUpdate = redisOperation.get(HeartBeatUtils.genHeartBeatKey(buildId, containerId, executeCount))
            ?: redisOperation.get(HeartBeatUtils.genHeartBeatKey(buildId, containerId))
            ?: run {
                LOG.info("$buildId|HEART_BEAT_MONITOR_CANCEL|j($containerId)")
                return
            }

        val elapse = System.currentTimeMillis() - lastUpdate.toLong()
        // 如果消息事件来源是取消构建操作，超时时间为CANCEL_TIMEOUT_IN_MS
        val timeOutLimit = if (event.source == BS_CANCEL_BUILD_SOURCE) CANCEL_TIMEOUT_IN_MS else TIMEOUT_IN_MS
        if (elapse > timeOutLimit) {
            timeout(event, elapse)
        } else {
            if (Math.floorMod(event.retryTime++, LOG_PER_TIMES) == 1) {
                LOG.info("ENGINE|$buildId|HEARTBEAT_MONITOR|" +
                    "${event.source}|j($containerId)|loopTime=${event.retryTime}")
            }
            // 正常是继续循环检查当前消息
            pipelineEventDispatcher.dispatch(event)
        }
    }

    @Suppress("ReturnCount", "LongMethod")
    private fun timeout(event: PipelineContainerAgentHeartBeatEvent, elapse: Long) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(event.projectId, event.buildId)
        if (buildInfo == null || buildInfo.status.isFinish()) {
            LOG.info("ENGINE|${event.buildId}|HEARTBEAT_MONITOR_FINISH|finish(${buildInfo?.status})")
            return
        }
        val container = pipelineContainerService.getContainer(
            projectId = event.projectId,
            buildId = event.buildId,
            stageId = null,
            containerId = event.containerId
        ) ?: run {
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

        // # 5806 完善构建进程超时提示信息
        val tipMessage = I18nUtil.getCodeLanMessage(
            messageCode = BK_TIP_MESSAGE,
            params = arrayOf("${TimeUnit.MILLISECONDS.toSeconds(elapse)}")
        )

        // #2365 在运行中的插件中记录心跳超时信息
        val runningTask = pipelineTaskService.getRunningTask(container.projectId, container.buildId)
        runningTask.forEach { taskMap ->
            if (container.containerId == taskMap["containerId"] && taskMap["taskId"] != null) {
                found = true
                val executeCount = taskMap["executeCount"]?.toString()?.toInt() ?: 1
                buildLogPrinter.addRedLine(
                    buildId = container.buildId,
                    message = tipMessage,
                    tag = taskMap["taskId"].toString(),
                    jobId = container.containerHashId,
                    executeCount = executeCount
                )
            }
        }

        if (!found) {
            // #2365 在Set Up Job位置记录心跳超时信息
            buildLogPrinter.addRedLine(
                buildId = container.buildId,
                message = tipMessage,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = container.containerHashId,
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
                containerHashId = container.containerHashId,
                containerType = container.containerType,
                actionType = ActionType.TERMINATE,
                reason = tipMessage,
                errorTypeName = ErrorType.THIRD_PARTY.name,
                errorCode = ErrorCode.THIRD_PARTY_BUILD_ENV_ERROR
            )
        )
    }
}
