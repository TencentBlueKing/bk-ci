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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_JOB_QUEUE_TIMEOUT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_QUEUE_TIMEOUT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_BUILD_QUEUE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TIMEOUT_IN_RUNNING
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineSettingService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.min

/**
 * 构建控制器
 * @version 1.0
 */
@Service
@Suppress("LongParameterList")
class BuildMonitorControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    companion object {
        private const val TAG = "startVM-0"
        private const val JOB_ID = "0"
        private val LOG = LoggerFactory.getLogger(BuildMonitorControl::class.java)
        private val TEN_MIN_MILLS = TimeUnit.MINUTES.toMillis(10)
        private fun coerceAtMost10Min(interval: Long) = interval.coerceAtMost(TEN_MIN_MILLS)
    }

    fun handle(event: PipelineBuildMonitorEvent): Boolean {

        val buildId = event.buildId
        val buildInfo = pipelineRuntimeService.getBuildInfo(event.projectId, buildId)
        if (buildInfo == null || buildInfo.isFinish()) {
            LOG.info("ENGINE|$buildId|BUILD_MONITOR|status=${buildInfo?.status}|ec=${event.executeCount}")
            return true
        }

        LOG.info("ENGINE|${event.buildId}|BUILD_MONITOR_START|ec=${event.executeCount}")
        return when {
            buildInfo.status.isReadyToRun() -> monitorQueueBuild(event, buildInfo)
            else -> {
                monitorPipeline(event, buildInfo)
            }
        }
    }

    private fun monitorPipeline(event: PipelineBuildMonitorEvent, buildInfo: BuildInfo): Boolean {

        // 由于30天对应的毫秒数值过大，以Int的上限值作为下一次monitor时间
        val stageMinInt = monitorStage(event, buildInfo)
        val jobMinInt = monitorContainer(event)

        val minInterval = min(jobMinInt, stageMinInt)

        if (minInterval < min(Timeout.CONTAINER_MAX_MILLS, Timeout.STAGE_MAX_MILLS)) {
            LOG.info("ENGINE|${event.buildId}|BUILD_MONITOR_CONTINUE|jobMinInt=$jobMinInt|" +
                "stageMinInt=$stageMinInt|Interval=$minInterval")
            // 每次Check间隔不能大于10分钟，防止长时间延迟消息被大量堆积
            event.delayMills = coerceAtMost10Min(minInterval).toInt()
            pipelineEventDispatcher.dispatch(event)
        } else {
            LOG.info("ENGINE|${event.buildId}|BUILD_MONITOR_QUIT|jobMinInt=$jobMinInt|" +
                "stageMinInt=$stageMinInt|Interval=$minInterval")
        }
        return true
    }

    private fun monitorContainer(event: PipelineBuildMonitorEvent): Long {

        var minInterval = Timeout.CONTAINER_MAX_MILLS
        // #5090 ==0 是为了兼容旧的监控事件
        var containers = pipelineContainerService.listContainers(event.projectId, event.buildId)
        if (containers.isEmpty()) { // 因数据被过期清理，必须超时失败
            buildLogPrinter.addRedLine(event.buildId, "empty_container!", TAG, JOB_ID, event.executeCount)
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = "empty_container",
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    status = BuildStatus.TERMINATE,
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCode.SYSTEM_OUTTIME_ERROR,
                    errorMsg = "empty_container"
                )
            )
            return minInterval
        }
        // #5090 ==0 是为了兼容旧的监控事件
        containers = containers.filter {
            !it.status.isFinish() && (it.executeCount == event.executeCount || event.executeCount == 0)
        }
        if (containers.isEmpty()) {
            LOG.info("ENGINE|${event.buildId}|BUILD_CONTAINER_MONITOR|no match")
            return minInterval
        }

        for (container in containers) {
            val interval = container.checkNextContainerMonitorIntervals(event.userId)
            // 根据最小的超时时间来决定下一次监控执行的时间
            if (interval in 1 until minInterval) {
                minInterval = interval
            }
        }
        return minInterval
    }

    private fun monitorStage(event: PipelineBuildMonitorEvent, buildInfo: BuildInfo): Long {

        var minInterval = Timeout.STAGE_MAX_MILLS

        var stages = pipelineStageService.listStages(event.projectId, event.buildId)
        if (stages.isEmpty()) { // 因数据被过期清理，必须超时失败
            buildLogPrinter.addRedLine(event.buildId, "empty_stage!", TAG, JOB_ID, event.executeCount)
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = "empty_stage",
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId,
                    buildId = event.buildId,
                    status = BuildStatus.TERMINATE,
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCode.SYSTEM_OUTTIME_ERROR,
                    errorMsg = "empty_stage"
                )
            )
            return minInterval
        }

        stages = stages.filter {
            // #5873 即使stage已完成，如果有准出卡审核也需要做超时监控
            (!it.status.isFinish() || it.checkOut?.status == BuildStatus.QUALITY_CHECK_WAIT.name) &&
                it.status != BuildStatus.STAGE_SUCCESS &&
                (it.executeCount == event.executeCount || event.executeCount == 0) // #5090 ==0 是为了兼容旧的监控事件
        }
        if (stages.isEmpty()) {
            LOG.info("ENGINE|${event.buildId}|BUILD_STAGE_MONITOR|no match")
            return minInterval
        }

        for (stage in stages) {
            val interval = stage.checkNextStageMonitorIntervals(
                userId = event.userId,
                startTime = stage.startTime,
                endTime = stage.endTime,
                buildInfo = buildInfo
            )
            // 根据最小的超时时间来决定下一次监控执行的时间
            if (interval in 1 until minInterval) {
                minInterval = interval
            }
        }

        return minInterval
    }

    private fun PipelineBuildContainer.checkNextContainerMonitorIntervals(userId: String): Long {

        val usedTimeMills: Long = if (status.isRunning() && startTime != null) {
            System.currentTimeMillis() - startTime!!.timestampmilli()
        } else {
            0
        }

        val minute = controlOption.jobControlOption.timeout
        val timeoutMills = Timeout.transMinuteTimeoutToMills(minute)

        buildLogPrinter.addDebugLine(
            buildId = buildId,
            message = "[SystemLog]Check job timeout($minute minutes), " +
                "running: ${TimeUnit.MILLISECONDS.toMinutes(usedTimeMills)} minutes!",
            tag = VMUtils.genStartVMTaskId(containerId),
            jobId = containerHashId,
            executeCount = executeCount
        )

        val interval = timeoutMills - usedTimeMills
        if (interval <= 0) {
            val errorInfo = I18nUtil.generateResponseDataObject<String>(
                messageCode = ERROR_TIMEOUT_IN_RUNNING,
                params = arrayOf("Job", "$minute"),
                language = I18nUtil.getLanguage(userId)
            )
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = errorInfo.message ?: "[SystemLog]Job timeout: $minute minutes!",
                tag = VMUtils.genStartVMTaskId(containerId),
                jobId = containerHashId,
                executeCount = executeCount
            )
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
                    containerHashId = containerHashId,
                    containerType = containerType,
                    actionType = ActionType.TERMINATE,
                    reason = errorInfo.message ?: "[SystemLog]Job timeout: $minute minutes!",
                    errorCode = ErrorCode.USER_JOB_OUTTIME_LIMIT,
                    errorTypeName = ErrorType.USER.name
                )
            )
        }

        return interval
    }

    private fun PipelineBuildStage.checkNextStageMonitorIntervals(
        userId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        buildInfo: BuildInfo
    ): Long {
        val checkInIntervals = checkInOutMonitorIntervals(
            userId = userId,
            inOrOut = true,
            checkTime = startTime,
            buildInfo = buildInfo
        )
        val checkOutIntervals = checkInOutMonitorIntervals(
            userId = userId,
            inOrOut = false,
            checkTime = endTime,
            buildInfo = buildInfo
        )
        return min(checkInIntervals, checkOutIntervals)
    }

    private fun PipelineBuildStage.checkInOutMonitorIntervals(
        userId: String,
        inOrOut: Boolean,
        checkTime: LocalDateTime?,
        buildInfo: BuildInfo
    ): Long {

        val pauseCheck = if (inOrOut) checkIn else checkOut
        if (pauseCheck?.manualTrigger != true && pauseCheck?.ruleIds.isNullOrEmpty()) {
            return Timeout.STAGE_MAX_MILLS
        }

        var hours = pauseCheck?.timeout ?: Timeout.DEFAULT_STAGE_TIMEOUT_HOURS
        if (hours <= 0 || hours > Timeout.MAX_HOURS) {
            hours = Timeout.MAX_HOURS.toInt()
        }

        val timeoutMills = TimeUnit.HOURS.toMillis(hours.toLong())
        val usedTimeMills: Long = if (checkTime != null) {
            System.currentTimeMillis() - checkTime.timestampmilli()
        } else {
            0
        }

        buildLogPrinter.addDebugLine(
            buildId = buildId,
            message = "Monitor| check stage review($inOrOut) timeout($hours hours), " +
                "running: ${TimeUnit.MILLISECONDS.toMinutes(usedTimeMills)}) minutes!",
            tag = stageId,
            jobId = "",
            executeCount = executeCount
        )

        val interval = timeoutMills - usedTimeMills
        if (interval <= 0) {
            buildLogPrinter.addRedLine(
                buildId = buildId,
                message = "Stage Review timeout $hours hours. Shutdown build!",
                tag = stageId,
                jobId = "",
                executeCount = executeCount
            )

            // #5654 如果是红线待审核状态则取消红线审核
            if (pauseCheck?.status == BuildStatus.QUALITY_CHECK_WAIT.name) {
                pipelineStageService.qualityTriggerStage(
                    userId = userId,
                    buildStage = this,
                    qualityRequest = StageQualityRequest(
                        position = ControlPointPosition.BEFORE_POSITION,
                        pass = false,
                        checkTimes = executeCount
                    ),
                    inOrOut = inOrOut,
                    check = pauseCheck,
                    timeout = true
                )
            }
            // #5654 如果是待人工审核则取消人工审核
            else if (pauseCheck?.groupToReview() != null) {
                val pipelineInfo =
                    pipelineRepositoryService.getPipelineInfo(buildInfo.projectId, buildInfo.pipelineId)
                pipelineStageService.cancelStage(
                    userId = userId,
                    triggerUserId = buildInfo.triggerUser,
                    pipelineName = pipelineInfo?.pipelineName,
                    buildNum = buildInfo.buildNum,
                    buildStage = this,
                    reviewRequest = StageReviewRequest(
                        reviewParams = listOf(),
                        id = pauseCheck.groupToReview()?.id,
                        suggest = "TIMEOUT"
                    ),
                    timeout = true
                )
            }
        }

        return interval
    }

    @Suppress("LongMethod")
    private fun monitorQueueBuild(event: PipelineBuildMonitorEvent, buildInfo: BuildInfo): Boolean {
        // 判断是否超时
        if (pipelineSettingService.isQueueTimeout(event.projectId, event.pipelineId, buildInfo.queueTime)) {
            val exitQueue = pipelineRuntimeExtService.existQueue(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                buildStatus = buildInfo.status
            )
            LOG.info("ENGINE|${event.buildId}|BUILD_QUEUE_MONITOR_TIMEOUT|queue timeout|exitQueue=$exitQueue")
            val errorInfo = I18nUtil.generateResponseDataObject<String>(
                messageCode = ERROR_TIMEOUT_IN_BUILD_QUEUE,
                params = arrayOf(event.buildId),
                language = I18nUtil.getLanguage()
            )
            val jobId = "0"
            buildLogPrinter.addRedLine(
                buildId = event.buildId,
                message = errorInfo.message ?: I18nUtil.getCodeLanMessage(
                    messageCode = BK_QUEUE_TIMEOUT,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + ". Cancel build!",
                tag = VMUtils.genStartVMTaskId(jobId),
                jobId = jobId,
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
                    errorMsg = I18nUtil.getCodeLanMessage(
                        messageCode = BK_JOB_QUEUE_TIMEOUT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            )
        } else {
            // 判断当前监控的排队构建是否可以尝试启动(仅当前是在队列中排第1位的构建可以)
            val canStart = pipelineRuntimeExtService.queueCanPend2Start(
                projectId = event.projectId, pipelineId = event.pipelineId, buildId = buildInfo.buildId
            )
            if (canStart) {
                val buildId = event.buildId
                LOG.info("ENGINE|$buildId|BUILD_QUEUE_TRY_START")
                val model = pipelineBuildDetailService.getBuildModel(event.projectId, buildInfo.buildId)
                    ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                        params = arrayOf(buildInfo.buildId)
                    )
                val triggerContainer = model.stages[0].containers[0] as TriggerContainer
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStartEvent(
                        source = "start_monitor",
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        userId = buildInfo.startUser,
                        buildId = buildInfo.buildId,
                        taskId = buildInfo.firstTaskId,
                        status = BuildStatus.RUNNING,
                        actionType = ActionType.START,
                        buildNoType = triggerContainer.buildNo?.buildNoType
                    )
                )
            }
            // next time to loop monitor
            pipelineEventDispatcher.dispatch(event)
        }

        return true
    }
}
