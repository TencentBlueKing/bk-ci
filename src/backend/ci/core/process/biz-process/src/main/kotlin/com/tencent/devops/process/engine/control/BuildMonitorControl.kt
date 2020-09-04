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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_BUILD_QUEUE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_RUNNING
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.PipelineSettingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildMonitorControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineStageService: PipelineStageService
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun handle(event: PipelineBuildMonitorEvent): Boolean {

        val buildId = event.buildId
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return false
        if (BuildStatus.isFinish(buildInfo.status)) {
            logger.info("[$buildId]|monitor| is ${buildInfo.status}")
            return true
        }

        return when {
            BuildStatus.isReadyToRun(buildInfo.status) -> monitorQueueBuild(event, buildInfo)
            else -> {
                monitorPipeline(event)
            }
        }
    }

    private fun monitorPipeline(event: PipelineBuildMonitorEvent): Boolean {

        // 由于30天对应的毫秒数值过大，以Int的上限值作为下一次monitor时间
        val stageMinInterval = min(monitorStage(event), Int.MAX_VALUE.toLong()).toInt()
        val containerMinInterval = monitorContainer(event)

        val minInterval = min(containerMinInterval, stageMinInterval)

        logger.info("[${event.buildId}]|pipeline_monitor|containerMinInterval=$containerMinInterval|stageMinInterval=$stageMinInterval")

        if (minInterval < min(Timeout.CONTAINER_MAX_MILLS.toLong(), Timeout.STAGE_MAX_MILLS)) {
            logger.info("[${event.buildId}]|pipeline_monitor_continue|minInterval=$minInterval")
            event.delayMills = minInterval
            pipelineEventDispatcher.dispatch(event)
        }
        return true
    }

    private fun monitorContainer(event: PipelineBuildMonitorEvent): Int {

        val containers = pipelineRuntimeService.listContainers(event.buildId)
            .filter {
                !BuildStatus.isFinish(it.status)
            }

        var minInterval = Timeout.CONTAINER_MAX_MILLS

        if (containers.isEmpty()) {
            logger.info("[${event.buildId}]|container_monitor| have not need monitor job!")
            return minInterval
        }

        containers.forEach { container ->
            val interval = container.checkNextContainerMonitorIntervals(event.userId)
            // 根据最小的超时时间来决定下一次监控执行的时间
            if (interval in 1 until minInterval) {
                minInterval = interval
            }
        }
        return minInterval
    }

    private fun monitorStage(event: PipelineBuildMonitorEvent): Long {

        val stages = pipelineStageService.listStages(event.buildId)
            .filter {
                !BuildStatus.isFinish(it.status)
            }

        var minInterval = Timeout.STAGE_MAX_MILLS

        if (stages.isEmpty()) {
            logger.info("[${event.buildId}]|stage_monitor| have not need monitor stage!")
            return minInterval
        }

        stages.forEach { stage ->
            val interval = stage.checkNextStageMonitorIntervals(event.userId)
            // 根据最小的超时时间来决定下一次监控执行的时间
            if (interval in 1 until minInterval) {
                minInterval = interval
            }
        }

        return minInterval
    }

//    companion object {
//        val MAX_MINUTES = TimeUnit.DAYS.toMinutes(7L) // 7 * 24 * 60 = 10080 分钟 = 执行最多超时7天
//        val CONTAINER_MAX_MILLS = TimeUnit.MINUTES.toMillis(MAX_MINUTES).toInt() + 1 // 毫秒+1
//        val MAX_HOURS = TimeUnit.DAYS.toHours(60) // 60 * 24 = 1440 小时 = 审核最多超时60天
//        val STAGE_MAX_MILLS = TimeUnit.HOURS.toMillis(MAX_HOURS) + 1 // 毫秒+1
//    }

    private fun PipelineBuildContainer.checkNextContainerMonitorIntervals(userId: String): Int {

        var interval = 0

        if (BuildStatus.isFinish(status)) {
            logger.info("[$buildId]|container=$containerId| is $status")
            return interval
        }
        val (minute: Int, timeoutMills: Long) = Timeout.transMinuteTimeoutToMills(controlOption?.jobControlOption?.timeout)
        val usedTimeMills: Long = if (BuildStatus.isRunning(status) && startTime != null) {
            System.currentTimeMillis() - startTime!!.timestampmilli()
        } else {
            0
        }

        logger.info("[$buildId]|start_monitor_container|container=$containerId|timeoutMills=$timeoutMills|useTimeMills=$usedTimeMills")

        interval = (timeoutMills - usedTimeMills).toInt()
        if (interval <= 0) {
            val tag = if (containerType == "normal") {
                "TIME_OUT_Job#$containerId(N)"
            } else {
                "TIME_OUT_Job#$containerId"
            }
            val errorInfo = MessageCodeUtil.generateResponseDataObject<String>(
                messageCode = ERROR_TIMEOUT_IN_RUNNING,
                params = arrayOf("Job", "$minute")
            )
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = errorInfo.message ?: "Job运行达到($minute)分钟，超时结束运行!",
                tag = tag,
                jobId = containerId,
                executeCount = 1
            )
            logger.warn("[$buildId]|monitor_container_timeout|container=$containerId")
            // 终止当前容器下的任务
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(
                    source = "running_timeout",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerType = containerType,
                    actionType = ActionType.TERMINATE,
                    reason = errorInfo.message ?: "Job运行达到($minute)分钟，超时结束运行!",
                    timeout = true
                )
            )
        }

        return interval
    }

    private fun PipelineBuildStage.checkNextStageMonitorIntervals(userId: String): Long {
        var interval: Long = 0

        if (controlOption?.stageControlOption?.manualTrigger != true) {
            logger.info("[$buildId]|not_monitor_stage|stage=$stageId|manualTrigger != true")
            return interval
        }
        if (BuildStatus.isFinish(status)) {
            logger.info("[$buildId]|not_monitor_stage|stage=$stageId|status=$status")
            return interval
        }

        var hours = controlOption?.stageControlOption?.timeout ?: Timeout.DEFAULT_STAGE_TIMEOUT_HOURS
        if (hours <= 0 || hours > Timeout.MAX_HOURS) {
            hours = Timeout.MAX_HOURS.toInt()
        }
        val timeoutMills = TimeUnit.HOURS.toMillis(hours.toLong())

        val usedTimeMills: Long = if (startTime != null) {
            System.currentTimeMillis() - startTime!!.timestampmilli()
        } else {
            0
        }

        logger.info("[$buildId]|start_monitor_stage|stage=$stageId|timeoutMills=$timeoutMills|useTimeMills=$usedTimeMills")

        interval = timeoutMills - usedTimeMills
        if (interval <= 0) {
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "审核触发时间超过($hours)小时，流水线将无法继续执行",
                tag = stageId,
                jobId = "",
                executeCount = 1
            )
            logger.warn("[$buildId]|monitor_stage_timeout|stage=$stageId")
            pipelineStageService.cancelStage(userId, projectId, pipelineId, buildId, stageId)
        }

        return interval
    }

    private fun monitorQueueBuild(event: PipelineBuildMonitorEvent, buildInfo: BuildInfo): Boolean {
        // 判断是否超时
        if (pipelineSettingService.isQueueTimeout(event.pipelineId, buildInfo.startTime!!)) {
            logger.info("[${event.buildId}]|monitor| queue timeout")
            val errorInfo = MessageCodeUtil.generateResponseDataObject<String>(
                messageCode = ERROR_TIMEOUT_IN_BUILD_QUEUE,
                params = arrayOf(event.buildId)
            )
            buildLogPrinter.addRedLine(
                buildId = event.buildId,
                message = errorInfo.message ?: "排队超时，取消运行! [${event.buildId}]",
                tag = "QUEUE_TIME_OUT",
                jobId = "",
                executeCount = 1
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = "queue_timeout",
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    status = BuildStatus.QUEUE_TIMEOUT,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_JOB_OUTTIME_LIMIT,
                    errorMsg = "Job排队超时，请检查并发配置"
                )
            )
        } else {
            // 判断当前监控的排队构建是否可以尝试启动(仅当前是在队列中排第1位的构建可以)
            if (pipelineRuntimeExtService.queueCanPend2Start(projectId = event.projectId, pipelineId = event.pipelineId, buildId = buildInfo.buildId)) {
                logger.info("[${event.buildId}]|monitor| still queue, to start it!")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStartEvent(
                        source = "start_monitor",
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        userId = buildInfo.startUser,
                        buildId = buildInfo.buildId,
                        taskId = buildInfo.firstTaskId,
                        status = BuildStatus.RUNNING,
                        actionType = ActionType.START
                    )
                )
            }
            // next time to loop monitor
            pipelineEventDispatcher.dispatch(event)
        }

        return true
    }
}
