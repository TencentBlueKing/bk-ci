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

import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateBuildTimeCost
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "ComplexMethod", "ReturnCount", "NestedBlockDepth")
@Service
class PipelineBuildRecordService @Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    private val dslContext: DSLContext,
    private val recordModelDao: BuildRecordModelDao,
    private val recordStageDao: BuildRecordStageDao,
    private val recordContainerDao: BuildRecordContainerDao,
    private val recordTaskDao: BuildRecordTaskDao,
    recordModelService: PipelineRecordModelService,
    pipelineResDao: PipelineResDao,
    pipelineResVersionDao: PipelineResVersionDao,
    pipelineElementService: PipelineElementService,
    redisOperation: RedisOperation,
    stageTagService: StageTagService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = recordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation,
    recordModelService = recordModelService,
    pipelineResDao = pipelineResDao,
    pipelineResVersionDao = pipelineResVersionDao,
    pipelineElementService = pipelineElementService
) {

    @Value("\${pipeline.build.retry.limit_days:21}")
    private var retryLimitDays: Int = 0

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildRecordService::class.java)!!
    }

    fun batchSave(
        transactionContext: DSLContext?,
        model: BuildRecordModel,
        stageList: List<BuildRecordStage>,
        containerList: List<BuildRecordContainer>,
        taskList: List<BuildRecordTask>
    ) {
        recordModelDao.createRecord(transactionContext ?: dslContext, model)
        recordStageDao.batchSave(transactionContext ?: dslContext, stageList)
        recordTaskDao.batchSave(transactionContext ?: dslContext, taskList)
        recordContainerDao.batchSave(transactionContext ?: dslContext, containerList)
    }

    private fun checkPassDays(startTime: Long?): Boolean {
        if (retryLimitDays < 0 || startTime == null) {
            return true
        }
        return (System.currentTimeMillis() - startTime) < TimeUnit.DAYS.toMillis(retryLimitDays.toLong())
    }

    /**
     * 查询ModelRecord
     * @param buildInfo: 构建信息
     * @param executeCount: 查询的执行次数
     * @param refreshStatus: 是否刷新状态
     */
    fun get(
        buildInfo: BuildInfo,
        executeCount: Int?,
        refreshStatus: Boolean = true
    ): ModelRecord? {
        // 直接取构建记录数据，防止接口传错
        val projectId = buildInfo.projectId
        val pipelineId = buildInfo.pipelineId
        val buildId = buildInfo.buildId
        logger.info("[$$buildId|$projectId|QUERY_BUILD_RECORD|$refreshStatus|executeCount=$executeCount")

        // 如果请求的executeCount异常则直接返回错误，防止数据错乱
        if (
            executeCount?.let {
                request -> request < 1 || buildInfo.executeCount?.let { request > it } == true
            } == true
        ) {
            return null
        }

        // 如果请求的次数为空则填补为最新的次数，旧数据直接按第一次查询
        var fixedExecuteCount = executeCount ?: buildInfo.executeCount ?: 1
        val buildRecordModel = recordModelDao.getRecord(
            dslContext, projectId, pipelineId, buildId, fixedExecuteCount
        )

        val version = buildInfo.version
        val model = if (buildRecordModel != null && buildInfo.executeCount != null) {
            val record = getRecordModel(
                projectId = projectId, pipelineId = pipelineId,
                version = version, buildId = buildId,
                fixedExecuteCount = fixedExecuteCount,
                buildRecordModel = buildRecordModel,
                executeCount = executeCount
            )
            if (record == null) fixedExecuteCount = buildInfo.executeCount!!
            record
        } else {
            null
        } ?: run {
            logger.info(
                "RECORD|turn to detail($buildId)|executeCount=$executeCount|" +
                    "fixedExecuteCount=$fixedExecuteCount"
            )
            val detail = pipelineBuildDetailService.getBuildModel(projectId, buildId) ?: return null
            fixDetailTimeCost(buildInfo, detail)
            detail
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId, buildInfo.pipelineId
        ) ?: return null

        val buildSummaryRecord = pipelineBuildSummaryDao.get(dslContext, projectId, buildInfo.pipelineId)
        logger.info(
            "RECORD|turn to detail($buildId)|buildInfoExecuteCount=${buildInfo.executeCount}" +
                "executeCount=$executeCount|fixedExecuteCount=$fixedExecuteCount"
        )

        // 判断需要刷新状态，目前只会改变canRetry & canSkip 状态
        // #7983 仅当查看最新一次执行记录时可以选择重试
        if (refreshStatus && fixedExecuteCount == buildInfo.executeCount) {
            // #4245 仅当在有限时间内并已经失败或者取消(终态)的构建上可尝试重试或跳过
            // #6400 无需流水线是终态就可以进行task重试
            if (checkPassDays(buildInfo.startTime)) {
                ModelUtils.refreshCanRetry(model)
            }
        }

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val buildNo = triggerContainer.buildNo
        if (buildNo != null) {
            buildNo.buildNo = buildSummaryRecord?.buildNo ?: buildNo.buildNo
        }
        val params = triggerContainer.params
        val newParams = ArrayList<BuildFormProperty>(params.size)
        params.forEach {
            // 变量名从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.id)
            if (!newVarName.isNullOrBlank()) newParams.add(it.copy(id = newVarName)) else newParams.add(it)
        }
        triggerContainer.params = newParams

        // #4531 兼容历史构建的页面显示
        model.stages.forEach { stage ->
            stage.resetBuildOption()
            // #4518 兼容历史构建的containerId作为日志JobId，发布后新产生的groupContainers无需校准
            stage.containers.forEach { container ->
                container.containerHashId = container.containerHashId ?: container.containerId
                container.containerId = container.id
                var elementElapsed = 0L
                container.elements.forEach { element ->
                    element.timeCost?.executeCost?.let {
                        element.elapsed = it
                        elementElapsed += it
                    }
                }
                container.elementElapsed = container.elementElapsed ?: elementElapsed
                container.systemElapsed = container.systemElapsed ?: container.timeCost?.systemCost
            }
            stage.elapsed = stage.elapsed ?: stage.timeCost?.totalCost
        }
        val triggerReviewers = pipelineTriggerReviewDao.getTriggerReviewers(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineInfo.pipelineId,
            buildId = buildId
        )

        val startUserList = recordModelDao.getRecordStartUserList(
            dslContext = dslContext,
            pipelineId = pipelineInfo.pipelineId,
            projectId = projectId,
            buildId = buildId
        )

        return ModelRecord(
            id = buildInfo.buildId,
            pipelineId = buildInfo.pipelineId,
            pipelineName = model.name,
            userId = buildInfo.startUser,
            triggerUser = buildInfo.triggerUser,
            trigger = StartType.toReadableString(buildInfo.trigger, buildInfo.channelCode),
            queueTime = buildRecordModel?.queueTime ?: buildInfo.queueTime,
            startTime = buildRecordModel?.startTime?.timestampmilli()
                ?: buildInfo.startTime ?: LocalDateTime.now().timestampmilli(),
            endTime = buildRecordModel?.endTime?.timestampmilli() ?: buildInfo.endTime,
            status = buildInfo.status.name,
            model = model,
            currentTimestamp = System.currentTimeMillis(),
            buildNum = buildInfo.buildNum,
            cancelUserId = buildRecordModel?.cancelUser,
            curVersion = buildInfo.version,
            latestVersion = pipelineInfo.version,
            latestBuildNum = buildSummaryRecord?.buildNum ?: -1,
            lastModifyUser = pipelineInfo.lastModifyUser,
            executeTime = buildInfo.executeTime,
            errorInfoList = buildRecordModel?.errorInfoList,
            triggerReviewers = triggerReviewers,
            executeCount = fixedExecuteCount,
            startUserList = startUserList,
            buildMsg = buildInfo.buildMsg,
            material = buildInfo.material,
            remark = buildInfo.remark,
            webhookInfo = buildInfo.webhookInfo
        )
    }

    private fun fixDetailTimeCost(buildInfo: BuildInfo, detail: Model) {
        if (buildInfo.status.isFinish()) {
            val queueCost = buildInfo.startTime?.let { startTime ->
                (startTime - buildInfo.queueTime).let {
                    if (it >= 0) it else null
                }
            } ?: 0
            detail.timeCost = BuildRecordTimeCost(
                systemCost = 0,
                executeCost = buildInfo.executeTime,
                waitCost = 0,
                queueCost = queueCost,
                totalCost = buildInfo.executeTime + queueCost
            )
        }
        detail.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.timeCost = BuildRecordTimeCost(
                    systemCost = container.systemElapsed ?: 0,
                    executeCost = container.elementElapsed ?: 0,
                    totalCost = (container.systemElapsed ?: 0) + (container.elementElapsed ?: 0)
                )
                container.elements.forEach { element ->
                    element.timeCost = BuildRecordTimeCost(
                        executeCost = element.elapsed ?: 0,
                        totalCost = element.elapsed ?: 0
                    )
                }
            }
        }
    }

    fun buildCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: BuildStatus,
        cancelUser: String,
        executeCount: Int
    ) {
        pipelineBuildDetailService.buildCancel(
            projectId = projectId,
            buildId = buildId,
            buildStatus = buildStatus,
            cancelUser = cancelUser
        )
        logger.info("[$buildId]|BUILD_CANCEL|cancelUser=$cancelUser|buildStatus=$buildStatus")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(
                context, projectId, pipelineId, buildId, executeCount
            ) ?: run {
                logger.warn(
                    "ENGINE|$buildId|buildEnd| get model($buildId) record failed."
                )
                return@transaction
            }
            val recordStages = recordStageDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount
            )
            // 第1层循环：刷新运行中stage状态
            recordStages.forEach nextStage@{ stage ->
                if (BuildStatus.parse(stage.status).isRunning()) {
                    return@nextStage
                }
                stage.status = buildStatus.name
                // 第2层循环：刷新stage下运行中的container状态
                val recordContainers = recordContainerDao.getRecords(
                    dslContext = context, projectId = projectId,
                    pipelineId = pipelineId, buildId = buildId,
                    executeCount = executeCount, stageId = stage.stageId,
                    matrixGroupId = null, buildStatus = BuildStatus.RUNNING
                )
                recordContainers.forEach nextContainer@{ container ->
                    val status = BuildStatus.parse(container.status)
                    val recordTasks = recordTaskDao.getRecords(
                        context, projectId, pipelineId, buildId, executeCount, container.containerId
                    )
                    // #3138 状态实时刷新
                    val refreshFlag = status.isRunning() && recordTasks[0].status.isNullOrBlank() &&
                        container.containPostTaskFlag != true
                    if (status == BuildStatus.PREPARE_ENV || refreshFlag) {
                        val containerName = container.containerVar[Container::name.name] as String?
                        if (!containerName.isNullOrBlank()) {
                            container.containerVar[Container::name.name] =
                                ContainerUtils.getClearedQueueContainerName(containerName)
                        }
                        container.status = buildStatus.name
                    }
                    container.status = buildStatus.name
                    // 第3层循环：刷新stage下运行中的container状态
                    recordTasks.forEach nextTask@{ task ->
                        if (BuildStatus.parse(task.status).isRunning()) {
                            return@nextTask
                        }
                        task.status = buildStatus.name
                    }
                    recordTaskDao.batchSave(context, recordTasks)
                }
                recordContainerDao.batchSave(context, recordContainers)
            }
            recordStageDao.batchSave(context, recordStages)

            val modelVar = mutableMapOf<String, Any>()
            modelVar[Model::timeCost.name] = recordModel.generateBuildTimeCost(recordStages)
            recordModelDao.updateRecord(
                context, projectId, pipelineId, buildId, executeCount, buildStatus,
                recordModel.modelVar.plus(modelVar), null, LocalDateTime.now(),
                null, null, null
            )
        }
    }

    fun buildEnd(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        errorInfoList: List<ErrorInfo>?,
        errorMsg: String?
    ): Pair<Model, List<BuildStageStatus>> {
        logger.info("[$buildId]|BUILD_END|buildStatus=$buildStatus")
//        var allStageStatus: List<BuildStageStatus> = emptyList()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(
                context, projectId, pipelineId, buildId, executeCount
            ) ?: run {
                logger.warn(
                    "ENGINE|$buildId|buildEnd| get model($buildId) record failed."
                )
                return@transaction
            }
            val recordStages = recordStageDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount
            )
            // 第1层循环：刷新运行中stage状态
            recordStages.forEach nextStage@{ stage ->
                if (!BuildStatus.parse(stage.status).isRunning()) {
                    return@nextStage
                }
                stage.status = buildStatus.name
                // 第2层循环：刷新stage下运行中的container状态
                val recordContainers = recordContainerDao.getRecords(
                    dslContext = context, projectId = projectId,
                    pipelineId = pipelineId, buildId = buildId,
                    executeCount = executeCount, stageId = stage.stageId,
                    matrixGroupId = null, buildStatus = BuildStatus.RUNNING
                )
                recordContainers.forEach nextContainer@{ container ->
                    if (!BuildStatus.parse(container.status).isRunning()) {
                        return@nextContainer
                    }
                    container.status = buildStatus.name
                    // 第3层循环：刷新stage下运行中的container状态
                    val recordTasks = recordTaskDao.getRecords(
                        context, projectId, pipelineId, buildId, executeCount,
                        container.containerId, BuildStatus.RUNNING
                    )
                    recordTasks.forEach nextTask@{ task ->
                        if (!BuildStatus.parse(task.status).isRunning()) {
                            return@nextTask
                        }
                        task.status = buildStatus.name
                    }
                    recordTaskDao.batchSave(context, recordTasks)
                }
                recordContainerDao.batchSave(context, recordContainers)
            }
            recordStageDao.batchSave(context, recordStages)

//            allStageStatus = fetchHistoryStageStatus(
//                recordStages = recordStages,
//                buildStatus = buildStatus,
//                errorMsg = errorMsg
//            )

            val modelVar = mutableMapOf<String, Any>()
            modelVar[Model::timeCost.name] = recordModel.generateBuildTimeCost(recordStages)
            recordModelDao.updateRecord(
                context, projectId, pipelineId, buildId, executeCount, buildStatus,
                recordModel.modelVar.plus(modelVar), null, LocalDateTime.now(),
                errorInfoList, null, null
            )
        }

        return pipelineBuildDetailService.buildEnd(
            projectId = projectId,
            buildId = buildId,
            buildStatus = buildStatus,
            errorMsg = errorMsg
        )
    }

    fun updateBuildCancelUser(projectId: String, buildId: String, cancelUserId: String) {
        pipelineBuildDetailService.updateBuildCancelUser(
            projectId = projectId,
            buildId = buildId,
            cancelUserId = cancelUserId
        )
        recordModelDao.updateBuildCancelUser(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            cancelUser = cancelUserId
        )
    }

    fun updateModelRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        modelVar: Map<String, Any>,
        buildStatus: BuildStatus?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>? = null
    ) {
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount
            ) ?: run {
                logger.warn(
                    "ENGINE|$buildId|updateModelByMap| get record failed."
                )
                return@transaction
            }

            recordModelDao.updateRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount, cancelUser = null,
                modelVar = recordModel.modelVar.plus(modelVar), buildStatus = buildStatus,
                startTime = startTime, endTime = endTime, errorInfoList = null,
                timestamps = timestamps?.let { mergeTimestamps(timestamps, recordModel.timestamps) }
            )
        }
    }
}
