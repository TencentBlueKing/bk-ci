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

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode.PLUGIN_DEFAULT_ERROR
import com.tencent.devops.common.api.pojo.ErrorCode.USER_QUALITY_CHECK_FAIL
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.ConcurrencyGroupLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildNoLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRedisService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.measure.MetricsService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PIPELINE_TIME_END
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 构建控制器
 * @version 1.0
 */
@Service
@Suppress("LongParameterList", "LongMethod", "ComplexMethod", "ReturnCount")
class BuildEndControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineRedisService: PipelineRedisService,
    private val meterRegistry: MeterRegistry,
    private val metricsService: MetricsService,
    private val buildVariableService: BuildVariableService
) {

    companion object {
        private const val FAIL_PIPELINE_COUNT = "fail_pipeline_count"
        private const val SUCCESS_PIPELINE_COUNT = "success_pipeline_count"
        private const val FINISH_PIPELINE_COUNT = "finish_pipeline_count"
        private val LOG = LoggerFactory.getLogger(BuildEndControl::class.java)
    }

    @BkTimed
    fun handle(event: PipelineBuildFinishEvent) {
        val watcher = Watcher(id = "ENGINE|BuildEnd|${event.traceId}|${event.buildId}|Job#${event.status}")
        try {
            with(event) {
                val buildIdLock = BuildIdLock(redisOperation, buildId)
                val buildInfo = try {
                    watcher.start("BuildIdLock")
                    buildIdLock.lock()
                    watcher.start("finish")
                    finish().also { watcher.stop() }
                } catch (ignored: Exception) {
                    LOG.warn("ENGINE|$buildId|$source|BUILD_FINISH_ERR|build finish fail: $ignored", ignored)
                    pipelineRuntimeService.getBuildInfo(projectId, buildId)
                } finally {
                    buildIdLock.unlock()
                }

                val buildStartLock = PipelineBuildStartLock(redisOperation, pipelineId)
                try {
                    watcher.start("PipelineBuildStartLock")
                    buildStartLock.lock()
                    watcher.start("popNextBuild")
                    popNextBuild(buildInfo)
                    watcher.stop()
                } finally {
                    buildStartLock.unlock()
                }
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    private fun PipelineBuildFinishEvent.finish(): BuildInfo? {

        // 将状态设置正确
        val buildStatus = BuildStatusSwitcher.pipelineStatusMaker.finish(status)

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        // 当前构建整体的状态，可能是运行中，也可能已经失败
        // 已经结束的构建，不再受理，抛弃消息 #5090 STAGE_SUCCESS 状态的也可能是已经处理完成
        if (buildInfo == null || buildInfo.isFinish()) {
            LOG.info("ENGINE|$buildId|$source|BUILD_FINISH_REPEAT_EVENT|STATUS=${buildInfo?.status}| abandon!")
            return buildInfo
        }
        LOG.info("ENGINE|$buildId|$source|BUILD_FINISH|$pipelineId|es=$status|bs=${buildInfo.status}")

        fixBuildInfo(buildInfo)

        // 刷新详情页状态
        val (model, allStageStatus, timeCost) = pipelineBuildRecordService.buildEnd(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildStatus = buildStatus,
            errorInfoList = buildInfo.errorInfoList,
            errorMsg = errorMsg,
            executeCount = buildInfo.executeCount ?: 1
        )

        // 记录本流水线最后一次构建的状态
        val endTime = LocalDateTime.now()
        pipelineRuntimeService.finishLatestRunningBuild(
            latestRunningBuild = LatestRunningBuild(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                userId = buildInfo.startUser, status = buildStatus, taskCount = buildInfo.taskCount,
                endTime = endTime, buildNum = buildInfo.buildNum
            ),
            currentBuildStatus = buildInfo.status,
            errorInfoList = buildInfo.errorInfoList,
            timeCost = timeCost
        )

        // 更新buildNo
        val retryFlag = buildInfo.executeCount?.let { it > 1 } == true || buildInfo.retryFlag == true
        if (!retryFlag && !buildStatus.isCancel() && !buildStatus.isFailure()) {
            setBuildNoWhenBuildSuccess(projectId = projectId, pipelineId = pipelineId, buildId = buildId)
        }

        pipelineRuntimeService.updateBuildHistoryStageState(projectId, buildId, allStageStatus)

        // 上报SLA数据
        if (buildStatus.isSuccess() || buildStatus == BuildStatus.STAGE_SUCCESS) {
            metricsIncrement(SUCCESS_PIPELINE_COUNT)
        } else if (buildStatus.isFailure()) {
            metricsIncrement(FAIL_PIPELINE_COUNT)
        }
        buildInfo.endTime = endTime.timestampmilli()
        buildInfo.status = buildStatus

        buildDurationTime(buildInfo.startTime!!)
        callBackParentPipeline(buildInfo)

        // 广播结束事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishBroadCastEvent(
                source = "build_finish_$buildId", projectId = projectId, pipelineId = pipelineId,
                userId = userId, buildId = buildId, status = buildStatus.name,
                startTime = buildInfo.startTime, endTime = buildInfo.endTime, triggerType = buildInfo.trigger,
                errorInfoList = if (buildInfo.errorInfoList != null) {
                    JsonUtil.toJson(buildInfo.errorInfoList!!)
                } else null
            ),
            PipelineBuildStatusBroadCastEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = ActionType.END
            ),
            PipelineBuildWebSocketPushEvent(
                source = "pauseTask",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                refreshTypes = RefreshType.HISTORY.binary
            )
        )

        // 发送metrics统计数据消息
        metricsService.postMetricsData(buildInfo, model)
        // 记录日志
        buildLogPrinter.stopLog(buildId = buildId, executeCount = buildInfo.executeCount)
        return buildInfo
    }

    private fun setBuildNoWhenBuildSuccess(projectId: String, pipelineId: String, buildId: String) {
        val model = pipelineBuildDetailService.getBuildModel(projectId, buildId) ?: return
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val buildNoObj = triggerContainer.buildNo ?: return

        if (buildNoObj.buildNoType == BuildNoType.SUCCESS_BUILD_INCREMENT) {
            // 使用分布式锁防止并发更新
            PipelineBuildNoLock(redisOperation = redisOperation, pipelineId = pipelineId).use { buildNoLock ->
                buildNoLock.lock()
                updateBuildNoInfo(projectId, pipelineId)
            }
        }
    }

    private fun updateBuildNoInfo(projectId: String, pipelineId: String) {
        val buildSummary = pipelineRuntimeService.getBuildSummaryRecord(projectId = projectId, pipelineId = pipelineId)
        val buildNo = buildSummary?.buildNo
        if (buildNo != null) {
            pipelineRuntimeService.updateBuildNo(projectId = projectId, pipelineId = pipelineId, buildNo = buildNo + 1)
            // 更新历史表的推荐版本号 BuildNo在开始就已经存入构建历史，构建结束后+1并不会影响本次构建开始的值
        }
    }

    private fun PipelineBuildFinishEvent.fixBuildInfo(buildInfo: BuildInfo) {
        val errorInfoList = mutableListOf<ErrorInfo>()
        pipelineTaskService.getAllBuildTask(projectId, buildId).forEach { task ->
            // 将所有还在运行中的任务全部结束掉
            if (task.status.isRunning()) {
                // 构建机直接结束
                if (task.containerType == VMBuildContainer.classType) {
                    pipelineTaskService.updateTaskStatus(
                        task = task, userId = userId, buildStatus = BuildStatus.TERMINATE,
                        errorType = errorType, errorCode = errorCode, errorMsg = errorMsg
                    )
                } else {
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildAtomTaskEvent(
                            source = javaClass.simpleName,
                            projectId = projectId, pipelineId = pipelineId, userId = task.starter,
                            stageId = task.stageId, buildId = task.buildId, containerId = task.containerId,
                            containerHashId = task.containerHashId, containerType = task.containerType,
                            taskId = task.taskId, taskParam = task.taskParams, actionType = ActionType.TERMINATE,
                            executeCount = task.executeCount ?: 1
                        )
                    )
                }
            }
            // 将插件出错信息逐一加入构建错误信息
            if (task.errorType != null) {
                val (taskId, taskName) = if (task.taskId.startsWith(VMUtils.getStartVmLabel())) {
                    val container = containerBuildRecordService.getRecord(
                        transactionContext = null, projectId = task.projectId, pipelineId = task.pipelineId,
                        buildId = task.buildId, containerId = task.containerId, executeCount = task.executeCount ?: 1
                    )
                    Pair("", container?.containerVar?.get(Container::name.name)?.toString() ?: task.taskName)
                } else {
                    Pair(task.taskId, task.taskName)
                }
                errorInfoList.add(
                    ErrorInfo(
                        stageId = task.stageId,
                        containerId = task.containerId,
                        matrixFlag = VMUtils.isMatrixContainerId(task.containerId),
                        // 启动插件问题设为job级别问题，将taskId置空，用于前端定位图至job
                        taskId = taskId,
                        taskName = taskName,
                        atomCode = task.atomCode ?: task.taskParams["atomCode"] as String? ?: task.taskType,
                        errorType = task.errorType?.num ?: ErrorType.USER.num,
                        errorCode = task.errorCode ?: PLUGIN_DEFAULT_ERROR,
                        errorMsg = task.errorMsg?.coerceAtMaxLength(PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX) ?: ""
                    )
                )
                // 做入库长度保护，假设超过上限则抛弃该错误信息
                if (JsonUtil.toJson(errorInfoList).toByteArray().size > PIPELINE_MESSAGE_STRING_LENGTH_MAX) {
                    errorInfoList.removeAt(errorInfoList.lastIndex)
                }
            }
        }
        pipelineStageService.getAllBuildStage(projectId, buildId).forEach { stage ->
            if (stage.checkIn?.status == BuildStatus.QUALITY_CHECK_FAIL.name ||
                stage.checkOut?.status == BuildStatus.QUALITY_CHECK_FAIL.name
            ) {
                errorInfoList.add(
                    ErrorInfo(
                        stageId = stage.stageId,
                        containerId = "",
                        taskId = "",
                        taskName = "",
                        atomCode = "",
                        errorType = ErrorType.USER.num,
                        errorCode = USER_QUALITY_CHECK_FAIL,
                        errorMsg = "Stage quality check failed"
                    )
                )
            }
            // 做入库长度保护，假设超过上限则抛弃该错误信息
            if (JsonUtil.toJson(errorInfoList).toByteArray().size > PIPELINE_MESSAGE_STRING_LENGTH_MAX) {
                errorInfoList.removeAt(errorInfoList.lastIndex)
            }
        }
        if (errorInfoList.isNotEmpty()) buildInfo.errorInfoList = errorInfoList
    }

    private fun PipelineBuildFinishEvent.popNextBuild(buildInfo: BuildInfo?) {
        if (pipelineRedisService.getBuildRestartValue(this.buildId) != null) {
            // 删除buildId占用的refresh锁
            pipelineRedisService.deleteRestartBuild(this.buildId)
        }

        if (buildInfo?.concurrencyGroup.isNullOrBlank()) {
            // 获取同流水线的下一个队首
            startNextBuild(
                pipelineRuntimeExtService.popNextQueueBuildInfo(
                    projectId = projectId,
                    pipelineId = pipelineId
                )
            )
        } else {
            // 获取同并发组的下一个队首
            buildInfo?.concurrencyGroup?.let { group ->
                ConcurrencyGroupLock(redisOperation, projectId, group).use { groupLock ->
                    groupLock.lock()
                    startNextBuild(
                        pipelineRuntimeExtService.popNextConcurrencyGroupQueueCanPend2Start(projectId, group)
                    )
                }
            }
        }
    }

    private fun PipelineBuildFinishEvent.startNextBuild(nextBuild: BuildInfo?) {
        if (nextBuild == null) {
            LOG.info("ENGINE|$buildId|$source|FETCH_QUEUE|$pipelineId no queue build!")
            return
        }

        LOG.info("ENGINE|$buildId|$source|FETCH_QUEUE|next build: ${nextBuild.buildId} ${nextBuild.status}")
        val model = pipelineBuildDetailService.getBuildModel(nextBuild.projectId, nextBuild.buildId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(nextBuild.buildId)
            )
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "build_finish_$buildId",
                projectId = nextBuild.projectId,
                pipelineId = nextBuild.pipelineId,
                userId = nextBuild.startUser,
                buildId = nextBuild.buildId,
                taskId = nextBuild.firstTaskId,
                status = nextBuild.status,
                actionType = ActionType.START,
                buildNoType = triggerContainer.buildNo?.buildNoType
            )
        )
    }

    // 设置流水线执行耗时
    private fun PipelineBuildFinishEvent.buildDurationTime(startTime: Long) {
        val endTime = System.currentTimeMillis()
        buildVariableService.setVariable(
            projectId = this.projectId,
            pipelineId = this.pipelineId,
            buildId = this.buildId,
            varName = PIPELINE_TIME_END,
            varValue = endTime
        )
        val duration = if (startTime <= 0L) { // 未启动，直接取消的情况下，耗时不准确
            "0"
        } else {
            ((endTime - startTime) / 1000).toString()
        }
        buildVariableService.setVariable(
            projectId = this.projectId,
            pipelineId = this.pipelineId,
            buildId = this.buildId,
            varName = PIPELINE_TIME_DURATION,
            varValue = duration
        )
    }

    // 子流水线回调父流水线
    private fun callBackParentPipeline(buildInfo: BuildInfo) {
        val parentBuildId = buildInfo.parentBuildId ?: return
        val parentTaskId = buildInfo.parentTaskId ?: return
        val parentProjectId = buildVariableService.getVariable(
            projectId = buildInfo.projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = buildInfo.buildId,
            varName = PIPELINE_START_PARENT_PROJECT_ID
        ) ?: return

        val parentBuildTask = pipelineTaskService.getBuildTask(parentProjectId, parentBuildId, parentTaskId)

        if (parentBuildTask == null) {
            LOG.warn("The parent build($parentBuildId) task($parentTaskId) not exist ")
            return
        }

        if (!parentBuildTask.status.isFinish()) {
            pipelineEventDispatcher.dispatch(
                PipelineBuildAtomTaskEvent(
                    source = "from_sub_pipeline_build_${buildInfo.buildId}", // 来源
                    projectId = parentBuildTask.projectId,
                    pipelineId = parentBuildTask.pipelineId,
                    userId = parentBuildTask.starter,
                    buildId = parentBuildTask.buildId,
                    stageId = parentBuildTask.stageId,
                    containerId = parentBuildTask.containerId,
                    containerHashId = parentBuildTask.containerHashId,
                    containerType = parentBuildTask.containerType,
                    taskId = parentBuildTask.taskId,
                    taskParam = parentBuildTask.taskParams,
                    actionType = ActionType.REFRESH,
                    executeCount = parentBuildTask.executeCount ?: 1
                )
            )
        }
    }

    private fun metricsIncrement(name: String) {
        Counter.builder(name).register(meterRegistry).increment()
        Counter.builder(FINISH_PIPELINE_COUNT).register(meterRegistry).increment()
    }
}
