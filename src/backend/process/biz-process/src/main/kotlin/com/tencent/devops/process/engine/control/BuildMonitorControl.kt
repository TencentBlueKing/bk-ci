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

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_BUILD_QUEUE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_RUNNING
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.PipelineSettingService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildMonitorControl @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun handle(event: PipelineBuildMonitorEvent) {

        val buildId = event.buildId
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return

        return when {
            BuildStatus.isFinish(buildInfo.status) -> {
                logger.info("[$buildId]|monitor| is ${buildInfo.status}")
            }
            BuildStatus.isReadyToRun(buildInfo.status) -> monitorQueueBuild(event, buildInfo)
            else -> monitorContainer(event)
        }
    }

    private fun monitorContainer(event: PipelineBuildMonitorEvent) {

        val containers = pipelineRuntimeService.listContainers(event.buildId)
            .filter {
                !BuildStatus.isFinish(it.status)
            }

        if (containers.isEmpty()) {
            logger.info("[${event.buildId}]|monitor| have not need monitor job!")
            return
        }

        var minInterval = Timeout.MAX_MILLS

        containers.forEach { container ->
            val interval = container.checkNextMonitorIntervals()
            // 根据最小的超时时间来决定下一次监控执行的时间
            if (interval in 1 until minInterval) {
                minInterval = interval
            }
        }

        if (minInterval < Timeout.MAX_MILLS) {
            logger.info("[${event.buildId}]|monitor_continue|pipelineId=${event.pipelineId}")
            event.delayMills = minInterval
            pipelineEventDispatcher.dispatch(event)
        }

        return
    }

    private fun PipelineBuildContainer.checkNextMonitorIntervals(): Int {

        var interval = 0

        if (BuildStatus.isFinish(status)) {
            logger.info("[$buildId]|container=$containerId| is $status")
            return interval
        }

        if (BuildStatus.isRunning(status) && startTime != null) {
            var minute = controlOption?.jobControlOption?.timeout ?: Timeout.DEFAULT_TIMEOUT_MIN
            if (minute <= 0 || minute > Timeout.MAX_MINUTES) {
                minute = Timeout.MAX_MINUTES
            }
            val timeoutMills = TimeUnit.MINUTES.toMillis(minute.toLong())

            // 超时就下发终止当前容器下的任务，然后监控结束
            val usedTimeMills = System.currentTimeMillis() - startTime!!.timestampmilli()

            logger.info("[$buildId]|container=$containerId|timeoutMills=$timeoutMills|useTimeMills=$usedTimeMills")

            interval = (timeoutMills - usedTimeMills).toInt()
            if (interval <= 0) {
                val tag = if (containerType == "normal") {
                    "TIME_OUT_Job#$containerId(N)"
                } else {
                    "TIME_OUT_Job#$containerId"
                }
                val errorInfo =
                    MessageCodeUtil.generateResponseDataObject<String>(
                        ERROR_TIMEOUT_IN_RUNNING.toString(),
                        arrayOf("Job", "$minute")
                    )
                logFail(
                    buildId = buildId, tag = tag,
                    message = errorInfo.message ?: "Job运行达到($minute)分钟，超时结束运行!"
                )
                logger.warn("[$buildId]|monitor_container_timeout|container=$containerId")
                // 终止当前容器下的任务
                pipelineEventDispatcher.dispatch(
                    PipelineBuildContainerEvent(
                        source = "running_timeout",
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = "System",
                        buildId = buildId,
                        stageId = stageId,
                        containerId = containerId,
                        containerType = containerType,
                        actionType = ActionType.TERMINATE
                    )
                )
            }
        }

        return interval
    }

    private fun monitorQueueBuild(event: PipelineBuildMonitorEvent, buildInfo: BuildInfo) {
        // 判断是否超时
        if (pipelineSettingService.isQueueTimeout(event.pipelineId, buildInfo.startTime!!)) {
            logger.info("[${event.buildId}]|monitor| queue timeout")
            val errorInfo =
                MessageCodeUtil.generateResponseDataObject<String>(
                    ERROR_TIMEOUT_IN_BUILD_QUEUE.toString(),
                    arrayOf(event.buildId)
                )
            logFail(
                buildId = event.buildId, tag = "QUEUE_TIME_OUT",
                message = errorInfo.message ?: "排队超时，取消运行! [${event.buildId}]"
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = "queue_timeout",
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    status = BuildStatus.QUEUE_TIMEOUT
                )
            )
        } else {
            val nextQueueBuildInfo = pipelineRuntimeService.getNextQueueBuildInfo(event.pipelineId)
            if (nextQueueBuildInfo?.buildId == event.buildId) { // 一直未启动的构建，则进行一次重发
                logger.info("[${event.buildId}]|monitor| still queue, to start it!")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStartEvent(
                        source = "start_monitor",
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        userId = event.userId,
                        buildId = event.buildId,
                        taskId = buildInfo.firstTaskId,
                        status = BuildStatus.RUNNING,
                        actionType = ActionType.START
                    ), event
                )
            }
        }

        return
    }

    private fun logFail(buildId: String, tag: String, message: String) {
        LogUtils.addFoldStartLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId, tagName = tag, tag = tag, executeCount = 1
        )
        LogUtils.addRedLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId, message = message, tag = tag, executeCount = 1
        )
        LogUtils.addFoldEndLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId, tagName = tag, tag = tag, executeCount = 1
        )
    }
}
