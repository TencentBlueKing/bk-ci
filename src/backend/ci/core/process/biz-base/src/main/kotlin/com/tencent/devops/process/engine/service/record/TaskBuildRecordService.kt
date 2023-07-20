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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.pojo.PipelineTaskStatusInfo
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateTaskTimeCost
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.engine.service.detail.TaskBuildDetailService
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress(
    "LongParameterList",
    "MagicNumber",
    "ReturnCount",
    "TooManyFunctions",
    "ComplexCondition",
    "ComplexMethod",
    "LongMethod",
    "NestedBlockDepth"
)
@Service
class TaskBuildRecordService(
    private val buildVariableService: BuildVariableService,
    private val dslContext: DSLContext,
    private val recordTaskDao: BuildRecordTaskDao,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val taskBuildDetailService: TaskBuildDetailService,
    recordModelService: PipelineRecordModelService,
    pipelineResDao: PipelineResDao,
    pipelineBuildDao: PipelineBuildDao,
    pipelineResVersionDao: PipelineResVersionDao,
    pipelineElementService: PipelineElementService,
    stageTagService: StageTagService,
    buildRecordModelDao: BuildRecordModelDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    redisOperation: RedisOperation
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = buildRecordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation,
    recordModelService = recordModelService,
    pipelineResDao = pipelineResDao,
    pipelineBuildDao = pipelineBuildDao,
    pipelineResVersionDao = pipelineResVersionDao,
    pipelineElementService = pipelineElementService
) {

    fun updateTaskStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        operation: String,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        taskBuildDetailService.updateTaskStatus(
            projectId = projectId,
            buildId = buildId,
            taskId = taskId,
            taskStatus = buildStatus,
            buildStatus = BuildStatus.RUNNING,
            operation = operation
        )
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = buildStatus,
            taskVar = emptyMap(),
            timestamps = timestamps,
            operation = operation
        )
    }

    // TODO #7983 暂时保留和detail一致的方法，后续简化为updateTaskStatus
    fun taskPause(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        executeCount: Int
    ) {
        taskBuildDetailService.taskPause(
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId,
            buildStatus = BuildStatus.PAUSE
        )
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.PAUSE,
            taskVar = emptyMap(),
            operation = "taskPause#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to BuildRecordTimeStamp(
                    LocalDateTime.now().timestampmilli(), null
                )
            )
        )
    }

    fun taskStart(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ) {
        taskBuildDetailService.taskStart(projectId, buildId, taskId)
        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = "taskStart#$taskId"
        ) {
            val delimiters = ","
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val recordTask = recordTaskDao.getRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount
                ) ?: run {
                    logger.warn(
                        "ENGINE|$buildId|updateTaskByMap| get task($taskId) record failed."
                    )
                    return@transaction
                }
                val taskVar = mutableMapOf<String, Any>()
                val taskStatus: BuildStatus
                if (
                    recordTask.classType == ManualReviewUserTaskElement.classType ||
                    (recordTask.classType == MatrixStatusElement.classType &&
                        recordTask.originClassType == ManualReviewUserTaskElement.classType)
                ) {
                    taskStatus = BuildStatus.REVIEWING
                    val list = mutableListOf<String>()
                    taskVar[ManualReviewUserTaskElement::reviewUsers.name]?.let {
                        try {
                            (it as List<*>).forEach { reviewUser ->
                                list.addAll(
                                    buildVariableService.replaceTemplate(projectId, buildId, reviewUser.toString())
                                        .split(delimiters)
                                )
                            }
                        } catch (ignore: Throwable) {
                            return@let
                        }
                    }
                    taskVar[ManualReviewUserTaskElement::reviewUsers.name] = list
                } else if (
                    recordTask.classType == QualityGateInElement.classType ||
                    recordTask.classType == QualityGateOutElement.classType ||
                    recordTask.originClassType == QualityGateInElement.classType ||
                    recordTask.originClassType == QualityGateOutElement.classType
                ) {
                    taskStatus = BuildStatus.REVIEWING
                } else {
                    taskStatus = BuildStatus.RUNNING
                }

                // TODO #7983 即将废除的旧数据兼容
                if (taskVar[Element::startEpoch.name] == null) { // 自动重试，startEpoch 不会为null，所以不需要查redis来确认
                    taskVar[Element::startEpoch.name] = System.currentTimeMillis()
                }
                taskVar.remove(Element::elapsed.name)
                taskVar.remove(Element::errorType.name)
                taskVar.remove(Element::errorCode.name)
                taskVar.remove(Element::errorMsg.name)

                recordTaskDao.updateRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = taskStatus,
                    startTime = recordTask.startTime ?: LocalDateTime.now(),
                    endTime = null,
                    timestamps = null
                )
            }
        }
    }

    fun taskPauseCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        executeCount: Int,
        cancelUser: String
    ) {
        taskBuildDetailService.taskCancel(
            projectId = projectId,
            buildId = buildId,
            containerId = containerId,
            taskId = taskId,
            cancelUser = cancelUser // fix me: 是否要直接更新取消人，暂时维护原有逻辑
        )
        updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.CANCELED,
            taskVar = emptyMap(),
            operation = "taskCancel#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            )
        )
    }

    fun taskPauseContinue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        executeCount: Int,
        element: Element?
    ) {
        taskBuildDetailService.taskContinue(
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId,
            element = element
        )
        // #7983 此处需要保持Container状态独立刷新，不能放进更新task的并发锁
        containerBuildRecordService.updateContainerStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount,
            buildStatus = BuildStatus.QUEUE,
            operation = "updateElementWhenPauseContinue#$taskId"
        )
        // TODO #7983 重写同container下的插件input
        updateTaskStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId,
            executeCount = executeCount,
            buildStatus = BuildStatus.QUEUE,
            operation = "updateElementWhenPauseContinue#$taskId",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            )
        )
    }

    fun taskEnd(taskBuildEndParam: TaskBuildEndParam): List<PipelineTaskStatusInfo> {

        val projectId = taskBuildEndParam.projectId
        val pipelineId = taskBuildEndParam.pipelineId
        val buildId = taskBuildEndParam.buildId
        val taskId = taskBuildEndParam.taskId
        // #7983 将RETRY中间态过滤，不体现在详情页面
        val buildStatus = taskBuildEndParam.buildStatus.let {
            if (it == BuildStatus.RETRY) null else it
        }
        val atomVersion = taskBuildEndParam.atomVersion
        val errorType = taskBuildEndParam.errorType
        val executeCount = taskBuildEndParam.executeCount

        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = "taskEnd#$taskId"
        ) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val recordTask = recordTaskDao.getRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount
                ) ?: run {
                    logger.warn(
                        "ENGINE|$buildId|taskEnd| get task($taskId) record failed."
                    )
                    return@transaction
                }
                val taskVar = mutableMapOf<String, Any>()
                if (atomVersion != null) {
                    when (recordTask.classType) {
                        MarketBuildAtomElement.classType -> {
                            taskVar[MarketBuildAtomElement::version.name] = atomVersion
                        }
                        MarketBuildLessAtomElement.classType -> {
                            taskVar[MarketBuildLessAtomElement::version.name] = atomVersion
                        }
                        else -> {
                            taskVar[MarketBuildAtomElement::version.name] = INIT_VERSION
                        }
                    }
                }
                var timestamps: MutableMap<BuildTimestampType, BuildRecordTimeStamp>? = null
                if (recordTask.status == BuildStatus.PAUSE.name) {
                    timestamps = mergeTimestamps(
                        recordTask.timestamps,
                        mapOf(
                            BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                                BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
                        )
                    )
                }
                if (errorType != null) {
                    taskVar[Element::errorType.name] = errorType.name
                    taskBuildEndParam.errorCode?.let { taskVar[Element::errorCode.name] = it }
                    taskBuildEndParam.errorMsg?.let { taskVar[Element::errorMsg.name] = it }
                }
                recordTask.generateTaskTimeCost()?.let {
                    taskVar[Element::timeCost.name] = it
                }
                recordTaskDao.updateRecord(
                    dslContext = context,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = buildStatus,
                    startTime = null,
                    endTime = LocalDateTime.now(),
                    timestamps = timestamps
                )
            }
        }

        return taskBuildDetailService.taskEnd(taskBuildEndParam)
    }

    fun updateTaskRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        taskVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        operation: String,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        update(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            executeCount = executeCount, buildStatus = BuildStatus.RUNNING,
            cancelUser = null, operation = operation
        ) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                val recordTask = recordTaskDao.getRecord(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount
                ) ?: run {
                    logger.warn(
                        "ENGINE|$buildId|updateTaskByMap| get task($taskId) record failed."
                    )
                    return@transaction
                }
                var startTime: LocalDateTime? = null
                var endTime: LocalDateTime? = null
                val now = LocalDateTime.now()
                val newTimestamps = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
                if (buildStatus?.isRunning() == true && recordTask.startTime == null) {
                    startTime = now
                }
                if (buildStatus?.isFinish() == true && recordTask.endTime == null) {
                    endTime = now
                    if (BuildStatus.parse(recordTask.status) == BuildStatus.REVIEWING) {
                        newTimestamps[BuildTimestampType.TASK_REVIEW_PAUSE_WAITING] =
                            BuildRecordTimeStamp(null, now.timestampmilli())
                    }
                }
                recordTaskDao.updateRecord(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = recordTask.taskVar.plus(taskVar),
                    buildStatus = buildStatus,
                    startTime = recordTask.startTime ?: startTime,
                    endTime = endTime,
                    timestamps = timestamps?.let { mergeTimestamps(timestamps, recordTask.timestamps) }
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskBuildRecordService::class.java)
    }
}
