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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.atom.vm.DispatchBuildLessDockerShutdownTaskAtom
import com.tencent.devops.process.engine.atom.vm.DispatchBuildLessDockerStartupTaskAtom
import com.tencent.devops.process.engine.atom.vm.DispatchVMShutdownTaskAtom
import com.tencent.devops.process.engine.atom.vm.DispatchVMStartupTaskAtom
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_DESC
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.service.BuildStartupParamService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_BUILD_REMARK
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_URL
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * 流水线运行时相关的服务
 * @version 1.0
 */
@Service
class PipelineRuntimeService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val pipelineWebsocketService: PipelineWebsocketService,
    private val pipelineStageService: PipelineStageService,
    private val buildIdGenerator: BuildIdGenerator,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineBuildContainerDao: PipelineBuildContainerDao,
    private val pipelineBuildStageDao: PipelineBuildStageDao,
    private val buildDetailDao: BuildDetailDao,
    private val buildStartupParamService: BuildStartupParamService,
    private val buildVariableService: BuildVariableService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
    }

    fun deletePipelineBuilds(projectId: String, pipelineId: String, userId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildSummaryDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineBuildDao.deletePipelineBuilds(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineBuildStageDao.deletePipelineBuildStages(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineBuildContainerDao.deletePipelineBuildContainers(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineBuildTaskDao.deletePipelineBuildTasks(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
        }
        buildVariableService.deletePipelineBuildVar(projectId = projectId, pipelineId = pipelineId)
        buildStartupParamService.deletePipelineBuildParam(projectId = projectId, pipelineId = pipelineId)
    }

    fun cancelPendingTask(projectId: String, pipelineId: String, userId: String) {
        val runningBuilds = pipelineBuildDao.getBuildTasksByStatus(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
            statusSet = setOf(
                BuildStatus.RUNNING, BuildStatus.REVIEWING,
                BuildStatus.QUEUE, BuildStatus.PREPARE_ENV,
                BuildStatus.UNEXEC, BuildStatus.QUEUE_CACHE
            )
        )

        for (build in runningBuilds) {
            if (build != null) {
                pipelineEventDispatcher.dispatch(
                    PipelineBuildCancelEvent(
                        source = "deletePipelineBuilds",
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = userId,
                        buildId = build.buildId,
                        status = BuildStatus.TERMINATE
                    )
                )
            }
        }
    }

    fun getBuildInfo(buildId: String): BuildInfo? {
        val t = pipelineBuildDao.getBuildInfo(dslContext, buildId)
        return pipelineBuildDao.convert(t)
    }

    fun getBuildNoByByPair(buildIds: Set<String>): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val buildInfoList = pipelineBuildDao.listBuildInfoByBuildIds(dslContext, buildIds)
        buildInfoList.forEach {
            result[it.buildId] = it.buildNum.toString()
        }
        return result
    }

    fun getBuildSummaryRecord(pipelineId: String): TPipelineBuildSummaryRecord? {
        return pipelineBuildSummaryDao.get(dslContext, pipelineId)
    }

    fun getBuildSummaryRecords(
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: Collection<String>? = null,
        sortType: PipelineSortType? = null,
        favorPipelines: List<String> = emptyList(),
        authPipelines: List<String> = emptyList(),
        viewId: String? = null,
        pipelineFilterParamList: List<PipelineFilterParam>? = null,
        permissionFlag: Boolean? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<out Record> {
        return pipelineBuildSummaryDao.listPipelineInfoBuildSummary(
            dslContext = dslContext,
            projectId = projectId,
            channelCode = channelCode,
            sortType = sortType,
            pipelineIds = pipelineIds,
            favorPipelines = favorPipelines,
            authPipelines = authPipelines,
            viewId = viewId,
            pipelineFilterParamList = pipelineFilterParamList,
            permissionFlag = permissionFlag,
            page = page,
            pageSize = pageSize
        )
    }

    fun getBuildSummaryRecords(
        dslContext: DSLContext,
        projectIds: Set<String>,
        channelCodes: Set<ChannelCode>?,
        limit: Int?,
        offset: Int?
    ): Result<out Record> {
        return pipelineBuildSummaryDao.listPipelineInfoBuildSummary(dslContext, projectIds, channelCodes, limit, offset)
    }

    fun getBuildSummaryRecords(
        channelCodes: Set<ChannelCode>?,
        pipelineIds: Collection<String>
    ): Result<out Record> {
        return pipelineBuildSummaryDao.listPipelineInfoBuildSummary(dslContext, channelCodes, pipelineIds)
    }

    fun getLatestBuild(projectId: String, pipelineIds: List<String>): Map<String, PipelineLatestBuild> {
        val records = getBuildSummaryRecords(
            projectId = projectId,
            channelCode = ChannelCode.BS,
            pipelineIds = pipelineIds
        )
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ret = mutableMapOf<String, PipelineLatestBuild>()
        records.forEach {
            val startTime = it["LATEST_START_TIME"] as? TemporalAccessor
            val endTime = it["LATEST_END_TIME"] as? TemporalAccessor
            val status = it["LATEST_STATUS"] as? Int
            val pipelineId = it["PIPELINE_ID"] as String
            ret[pipelineId] = PipelineLatestBuild(
                buildId = it["LATEST_BUILD_ID"] as String? ?: "",
                startUser = it["LATEST_START_USER"] as String? ?: "",
                startTime = if (startTime != null) df.format(startTime) else "",
                endTime = if (endTime != null) df.format(endTime) else "",
                status = if (status != null) BuildStatus.values()[status].name else null
            )
        }

        return ret
    }

    fun getRunningTask(projectId: String, buildId: String): List<Map<String, Any>> {
        val listByStatus = pipelineBuildTaskDao.listByStatus(
            dslContext = dslContext,
            buildId = buildId,
            containerId = null,
            statusSet = listOf(BuildStatus.RUNNING, BuildStatus.REVIEWING)
        )
        val list = mutableListOf<Map<String, Any>>()
        val buildStatus = BuildStatus.values()
        listByStatus.forEach {
            list.add(
                mapOf(
                    "taskId" to it.taskId,
                    "containerId" to it.containerId,
                    "status" to buildStatus[it.status].name,
                    "executeCount" to it.executeCount
                )
            )
        }
        return list
    }

    fun getBuildTask(buildId: String, taskId: String): PipelineBuildTask? {
        val t = pipelineBuildTaskDao.get(dslContext, buildId, taskId)
        return if (t != null) {
            pipelineBuildTaskDao.convert(t)
        } else {
            null
        }
    }

    fun listContainerBuildTasks(
        buildId: String,
        containerId: String,
        buildStatus: BuildStatus? = null
    ): List<PipelineBuildTask> {
        val list = pipelineBuildTaskDao.listByStatus(
            dslContext = dslContext,
            buildId = buildId,
            containerId = containerId,
            statusSet = if (buildStatus != null) setOf(buildStatus) else null
        )
        val result = mutableListOf<PipelineBuildTask>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildTaskDao.convert(it)!!)
            }
        }
        return result
    }

    fun getAllBuildTask(buildId: String): Collection<PipelineBuildTask> {
        val list = pipelineBuildTaskDao.getByBuildId(dslContext, buildId)
        val result = mutableListOf<PipelineBuildTask>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildTaskDao.convert(it)!!)
            }
        }
        return result
    }

    fun getContainer(buildId: String, stageId: String?, containerId: String): PipelineBuildContainer? {
        val result = pipelineBuildContainerDao.get(dslContext, buildId, stageId, containerId)
        if (result != null) {
            return pipelineBuildContainerDao.convert(result)
        }
        return null
    }

    fun listContainers(buildId: String, stageId: String? = null): List<PipelineBuildContainer> {
        val list = pipelineBuildContainerDao.listByBuildId(dslContext, buildId, stageId)
        val result = mutableListOf<PipelineBuildContainer>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildContainerDao.convert(it)!!)
            }
        }
        return result
    }

    fun updateContainerStatus(
        buildId: String,
        stageId: String,
        containerId: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        buildStatus: BuildStatus
    ) {
        logger.info("[$buildId]|updateContainerStatus|status=$buildStatus|containerId=$containerId|stageId=$stageId")
        pipelineBuildContainerDao.updateStatus(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            buildStatus = buildStatus,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun listPipelineBuildHistory(projectId: String, pipelineId: String, offset: Int, limit: Int): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = if (limit < 0) 1000 else limit
        )
        val result = mutableListOf<BuildHistory>()
        val buildStatus = BuildStatus.values()
        list.forEach {
            result.add(genBuildHistory(it, buildStatus, currentTimestamp))
        }
        return result
    }

    fun listPipelineBuildHistory(
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?
    ): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            materialAlias = materialAlias,
            materialUrl = materialUrl,
            materialBranch = materialBranch,
            materialCommitId = materialCommitId,
            materialCommitMessage = materialCommitMessage,
            status = status,
            trigger = trigger,
            queueTimeStartTime = queueTimeStartTime,
            queueTimeEndTime = queueTimeEndTime,
            startTimeStartTime = startTimeStartTime,
            startTimeEndTime = startTimeEndTime,
            endTimeStartTime = endTimeStartTime,
            endTimeEndTime = endTimeEndTime,
            totalTimeMin = totalTimeMin,
            totalTimeMax = totalTimeMax,
            remark = remark,
            offset = offset,
            limit = if (limit < 0) {
                1000
            } else limit,
            buildNoStart = buildNoStart,
            buildNoEnd = buildNoEnd
        )
        val result = mutableListOf<BuildHistory>()
        val buildStatus = BuildStatus.values()
        list.forEach {
            result.add(genBuildHistory(it, buildStatus, currentTimestamp))
        }
        return result
    }

    fun updateBuildRemark(projectId: String, pipelineId: String, buildId: String, remark: String?) {
        logger.info("update build history remark, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, remark: $remark")
        pipelineBuildDao.updateBuildRemark(dslContext, buildId, remark)
    }

    fun getHistoryConditionRepo(projectId: String, pipelineId: String): List<String> {
        val history = pipelineBuildDao.getBuildHistoryMaterial(dslContext, projectId, pipelineId)
        val materialObjList = mutableListOf<PipelineBuildMaterial>()
        history.forEach {
            if (it.material != null) {
                materialObjList.addAll(JsonUtil.getObjectMapper().readValue(it.material) as List<PipelineBuildMaterial>)
            }
        }
        return materialObjList.filter { !it.aliasName.isNullOrBlank() }.map { it.aliasName!! }.distinct()
    }

    fun getHistoryConditionBranch(projectId: String, pipelineId: String, aliasList: List<String>?): List<String> {
        val history = pipelineBuildDao.getBuildHistoryMaterial(dslContext, projectId, pipelineId)
        val materialObjList = mutableListOf<PipelineBuildMaterial>()
        history.forEach {
            if (it.material != null) {
                materialObjList.addAll(JsonUtil.getObjectMapper().readValue(it.material) as List<PipelineBuildMaterial>)
            }
        }
        val aliasNames = if (null == aliasList || aliasList.isEmpty()) {
            materialObjList.map { it.aliasName }
        } else {
            aliasList
        }

        val result = mutableListOf<String>()
        aliasNames.distinct().forEach { alias ->
            val branchNames = materialObjList.filter { it.aliasName == alias && !it.branchName.isNullOrBlank() }
                .map { it.branchName!! }.distinct()
            result.addAll(branchNames)
        }
        return result.distinct()
    }

    private fun genBuildHistory(
        tPipelineBuildHistoryRecord: TPipelineBuildHistoryRecord,
        buildStatus: Array<BuildStatus>,
        currentTimestamp: Long
    ): BuildHistory {
        return with(tPipelineBuildHistoryRecord) {
            val totalTime = if (startTime == null || endTime == null) {
                0
            } else {
                Duration.between(startTime, endTime).toMillis()
            }
            BuildHistory(
                id = buildId,
                userId = triggerUser ?: startUser,
                trigger = StartType.toReadableString(trigger, ChannelCode.valueOf(channel)),
                buildNum = buildNum,
                pipelineVersion = version,
                startTime = startTime?.timestampmilli() ?: 0L,
                endTime = endTime?.timestampmilli(),
                status = buildStatus[status].name,
                stageStatus = if (stageStatus != null) {
                    JsonUtil.getObjectMapper().readValue(stageStatus) as List<BuildStageStatus>
                } else {
                    null
                },
                deleteReason = "",
                currentTimestamp = currentTimestamp,
                material = if (material != null) {
                    val materialList = JsonUtil.getObjectMapper().readValue(material) as List<PipelineBuildMaterial>
                    materialList.sortedBy { it.aliasName }
                } else {
                    null
                },
                queueTime = queueTime?.timestampmilli(),
                artifactList = if (artifactInfo != null) {
                    JsonUtil.getObjectMapper().readValue(artifactInfo) as List<FileInfo>
                } else {
                    null
                },
                remark = remark,
                totalTime = totalTime,
                executeTime = if (executeTime == null || executeTime == 0L) {
                    if (BuildStatus.isFinish(buildStatus[status])) {
                        totalTime
                    } else 0L
                } else {
                    executeTime
                },
                buildParameters = if (buildParameters != null) {
                    JsonUtil.getObjectMapper().readValue(buildParameters) as List<BuildParameters>
                } else {
                    null
                },
                webHookType = webhookType,
                webhookInfo = if (webhookInfo != null) {
                    JsonUtil.getObjectMapper().readValue(webhookInfo) as WebhookInfo
                } else {
                    null
                },
                startType = getStartType(trigger, webhookType),
                recommendVersion = recommendVersion,
                retry = isRetry ?: false,
                errorInfoList = if (errorInfo != null) {
                    try {
                        JsonUtil.getObjectMapper().readValue(errorInfo) as List<ErrorInfo>
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            )
        }
    }

    private fun getStartType(trigger: String, webhookType: String?): String {
        return when (trigger) {
            StartType.MANUAL.name -> {
                ManualTriggerElement.classType
            }
            StartType.TIME_TRIGGER.name -> {
                TimerTriggerElement.classType
            }
            StartType.WEB_HOOK.name -> {
                when (webhookType) {
                    CodeType.SVN.name -> {
                        CodeSVNWebHookTriggerElement.classType
                    }
                    CodeType.GIT.name -> {
                        CodeGitWebHookTriggerElement.classType
                    }
                    CodeType.GITLAB.name -> {
                        CodeGitlabWebHookTriggerElement.classType
                    }
                    CodeType.GITHUB.name -> {
                        CodeGithubWebHookTriggerElement.classType
                    }
                    CodeType.TGIT.name -> {
                        CodeTGitWebHookTriggerElement.classType
                    }
                    else -> RemoteTriggerElement.classType
                }
            }
            else -> { // StartType.SERVICE.name,  StartType.PIPELINE.name, StartType.REMOTE.name
                RemoteTriggerElement.classType
            }
        }
    }

    fun getBuildHistoryByBuildNum(
        projectId: String,
        pipelineId: String,
        buildNum: Int?,
        statusSet: Set<BuildStatus>?
    ): BuildHistory? {
        val record = pipelineBuildDao.getBuildInfoByBuildNum(dslContext, projectId, pipelineId, buildNum, statusSet)
        return if (record != null) {
            genBuildHistory(record, BuildStatus.values(), System.currentTimeMillis())
        } else {
            null
        }
    }

    fun getBuildBasicInfoByIds(buildIds: Set<String>): Map<String, BuildBasicInfo> {
        val records = pipelineBuildDao.listBuildInfoByBuildIds(dslContext, buildIds)
        val result = mutableMapOf<String, BuildBasicInfo>()
        if (records.isEmpty())
            return result

        buildIds.forEach { buildId ->
            result[buildId] = BuildBasicInfo(buildId, "", "", 0)
        }
        records.forEach {
            with(it) {
                result[it.buildId] = BuildBasicInfo(buildId, projectId, pipelineId, version)
            }
        }
        return result
    }

    fun getBuildHistoryByIds(buildIds: Set<String>): List<BuildHistory> {
        val records = pipelineBuildDao.listBuildInfoByBuildIds(dslContext, buildIds)
        val result = mutableListOf<BuildHistory>()
        if (records.isEmpty())
            return result
        val values = BuildStatus.values()
        val currentTimestamp = System.currentTimeMillis()
        records.forEach {
            result.add(genBuildHistory(it, values, currentTimestamp))
        }
        return result
    }

    fun cancelBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        buildStatus: BuildStatus
    ): Boolean {
        logger.info("[$buildId]|SHUTDOWN_BUILD|userId=$userId|status=$buildStatus")
        // 发送事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildCancelEvent(
                source = javaClass.simpleName,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus
            )
        )

        return true
    }

    fun startBuild(pipelineInfo: PipelineInfo, fullModel: Model, startParamsWithType: List<BuildParameters>): String {
        val params = startParamsWithType.map { it.key to it.value }.toMap()
        val startBuildStatus: BuildStatus = BuildStatus.QUEUE // 默认都是排队状态
        // 2019-12-16 产品 rerun 需求
        val buildId = params[PIPELINE_RETRY_BUILD_ID]?.toString() ?: buildIdGenerator.getNextId()

        val startType = StartType.valueOf(params[PIPELINE_START_TYPE] as String)
        val parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID]?.toString()

        val parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID]?.toString()
        val channelCode = if (params[PIPELINE_START_CHANNEL] != null) ChannelCode.valueOf(params[PIPELINE_START_CHANNEL].toString()) else ChannelCode.BS

        var taskCount = 0
        val userId = params[PIPELINE_START_USER_ID].toString()
        val triggerUser = params[PIPELINE_START_USER_NAME].toString()
        // 原子重试
        val retryStartTaskId = params[PIPELINE_RETRY_START_TASK_ID]?.toString()

        val (actionType, retryCount, isStageRetry) = if (params[PIPELINE_RETRY_COUNT] != null) {
            val i = try {
                params[PIPELINE_RETRY_COUNT].toString().trim().toInt()
            } catch (ignored: NumberFormatException) {
                0
            }
            Triple(ActionType.RETRY, i, retryStartTaskId?.startsWith("stage-") == true)
        } else {
            Triple(ActionType.START, 0, false)
        }

        var firstTaskId = if (params[PIPELINE_START_TASK_ID] != null) params[PIPELINE_START_TASK_ID].toString() else ""

        val updateExistsRecord: MutableList<TPipelineBuildTaskRecord> = mutableListOf()
        val defaultStageTagId = pipelineStageService.getDefaultStageTagId()
        val lastTimeBuildTaskRecords = pipelineBuildTaskDao.getByBuildId(dslContext, buildId)
        val lastTimeBuildContainerRecords = pipelineBuildContainerDao.listByBuildId(dslContext, buildId)
        val lastTimeBuildStageRecords = pipelineBuildStageDao.listByBuildId(dslContext, buildId)

        val buildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, buildId)
        val sModel: Model = if (buildHistoryRecord != null) {
            taskCount = buildHistoryRecord.taskCount
            val record = buildDetailDao.get(dslContext, buildId)
            if (record != null) {
                JsonUtil.getObjectMapper().readValue(record.model, Model::class.java)
            } else {
                fullModel
            }
        } else {
            fullModel
        }

        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val buildContainers = mutableListOf<PipelineBuildContainer>()
        val buildStages = mutableListOf<PipelineBuildStage>()

        val updateStageExistsRecord: MutableList<TPipelineBuildStageRecord> = mutableListOf()
        val updateContainerExistsRecord: MutableList<TPipelineBuildContainerRecord> = mutableListOf()

        var containerSeq = 0

        // --- 第1层循环：Stage遍历处理 ---
        sModel.stages.forEachIndexed nextStage@{ index, stage ->
            val stageId = stage.id!!
            var needUpdateStage = false
            // 当前 stage 是否是重试的 stage
            val retryStage = stageId == retryStartTaskId

            // 如果是stage重试不是当前stage，则直接进入下一个stage
            if (isStageRetry && !retryStage) {
                logger.info("[$buildId|RETRY|STAGE(#$stageId)(${stage.name}) is not in retry STAGE($retryStartTaskId)")
                containerSeq += stage.containers.size // Job跳过计数也需要增加
                return@nextStage
            }

            DependOnUtils.initDependOn(stage = stage, params = params)
            // --- 第2层循环：Container遍历处理 ---
            stage.containers.forEach nextContainer@{ container ->
                var startVMTaskSeq = -1 // 启动构建机位置，解决如果在执行人工审核插件时，无编译环境不需要提前无意义的启动
                var needStartVM = false // 是否需要启动构建
                var needUpdateContainer = false
                var taskSeq = 0
                // 构建机环境处理，需要先创建一个的启动构建机原子任务
                val containerId = container.id!!
                val containerHashId = container.containerId ?: ""
                val containerType = container.getClassType()

                if (container is TriggerContainer) { // 寻找触发点
                    container.elements.forEach { atomElement ->
                        if (firstTaskId.isBlank() && atomElement.isElementEnable()) {
                            firstTaskId = atomElement.findFirstTaskIdByStartType(startType)
                        }
                    }
                    containerSeq++
                    return@nextContainer
                } else if (container is NormalContainer) {
                    if (!ContainerUtils.isNormalContainerEnable(container)) {
                        containerSeq++
                        return@nextContainer
                    }
                } else if (container is VMBuildContainer) {
                    if (!ContainerUtils.isVMBuildContainerEnable(container)) {
                        containerSeq++
                        return@nextContainer
                    }
                }
                // --- 第3层循环：Element遍历处理 ---
                container.elements.forEach nextElement@{ atomElement ->
                    taskSeq++ // 跳过的也要+1，Seq不需要连续性
                    val status = atomElement.takeStatus(params = params)

                    if (BuildStatus.isFinish(status)) {
                        logger.info("[$buildId|${atomElement.id}] status=$status")
                        atomElement.status = status.name
                        return@nextElement
                    }

                    // 计算启动构建机的插件任务的序号
                    if (startVMTaskSeq < 0) {
                        startVMTaskSeq = calculateStartVMTaskSeq(taskSeq = taskSeq, container = container, atomElement = atomElement)
                        if (startVMTaskSeq > 0) {
                            taskSeq++ // 当前插件任务的执行序号往后移动一位，留给构建机启动插件任务
                        }
                    }
                    // 全新构建
                    if (lastTimeBuildTaskRecords.isEmpty()) {
                        taskCount++
                        buildTaskList.add(
                            PipelineBuildTask(
                                projectId = pipelineInfo.projectId,
                                pipelineId = pipelineInfo.pipelineId,
                                buildId = buildId,
                                stageId = stageId,
                                containerId = containerId,
                                containerHashId = containerHashId,
                                containerType = containerType,
                                taskSeq = taskSeq,
                                taskId = atomElement.id!!,
                                taskName = if (atomElement.name.length > 128) atomElement.name.substring(0, 128) else atomElement.name,
                                taskType = atomElement.getClassType(),
                                taskAtom = atomElement.getTaskAtom(),
                                status = status,
                                taskParams = atomElement.genTaskParams(),
                                additionalOptions = atomElement.additionalOptions,
                                executeCount = 1,
                                starter = userId,
                                approver = null,
                                subBuildId = null,
                                atomCode = atomElement.getAtomCode()
                            )
                        )
                    } else {
                        // 如果是失败的插件重试，并且当前插件不是要重试的插件，则检查其之前的状态，如果已经执行过，则跳过
                        if (!retryStage && !retryStartTaskId.isNullOrBlank() && retryStartTaskId != atomElement.id) {
                            val target = findTaskRecord(
                                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                                container = container,
                                retryStartTaskId = atomElement.id!!
                            )
                            // 插件任务在历史中找不到，则跳过当前插件
                            // 如果插件任务之前已经是完成状态，则跳过当前插件
                            try {
                                if (target == null || BuildStatus.isFinish(BuildStatus.values()[target.status])) {
                                    return@nextElement
                                }
                            } catch (ignored: Exception) { // 如果存在异常的ordinal
                                logger.error("[$buildId]|BAD_BUILD_STATUS| target=${target?.taskId}| status=${target?.status}", ignored)
                                return@nextElement
                            }
                        }

                        // Rebuild/Stage-Retry/Fail-Task-Retry  重跑/Stage重试/失败的插件重试
                        val taskRecord = retryDetailModelStatus(
                            lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                            stage = stage,
                            container = container,
                            retryStartTaskId = atomElement.id!!,
                            retryCount = retryCount,
                            atomElement = atomElement
                        )

                        if (taskRecord != null) {
                            updateExistsRecord.add(taskRecord)
                            needUpdateContainer = true
                        }
                    }

                    // 确认是否要启动构建机/无编译环境
                    if (!needStartVM && startVMTaskSeq > 0) {
                        needStartVM = true
                    }
                }
                // 填入: 构建机或无编译环境的环境处理，需要启动和结束构建机/环境的插件任务
                if (needStartVM) {
                    supplyVMTask(
                        stage = stage,
                        container = container,
                        containerSeq = containerSeq,
                        startVMTaskSeq = startVMTaskSeq,
                        lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                        updateExistsRecord = updateExistsRecord,
                        buildTaskList = buildTaskList,
                        pipelineInfo = pipelineInfo,
                        retryCount = retryCount,
                        buildId = buildId,
                        stageId = stageId,
                        userId = userId
                    )
                }

                if (lastTimeBuildContainerRecords.isNotEmpty()) {
                    if (needUpdateContainer) {
                        run findHistoryContainer@{
                            lastTimeBuildContainerRecords.forEach {
                                if (it.containerId == containerId && it.status != BuildStatus.SKIP.ordinal) {
                                    it.status = BuildStatus.QUEUE.ordinal
                                    it.executeCount += 1
                                    updateContainerExistsRecord.add(it)
                                    return@findHistoryContainer
                                }
                            }
                        }
                    }
                } else {
                    ModelUtils.initContainerOldData(container)
                    val controlOption = when (container) {
                        is NormalContainer -> PipelineBuildContainerControlOption(
                            container.jobControlOption!!,
                            container.mutexGroup
                        )
                        is VMBuildContainer -> PipelineBuildContainerControlOption(
                            container.jobControlOption!!,
                            container.mutexGroup
                        )
                        else -> null
                    }
                    buildContainers.add(
                        PipelineBuildContainer(
                            projectId = pipelineInfo.projectId,
                            pipelineId = pipelineInfo.pipelineId,
                            buildId = buildId,
                            stageId = stageId,
                            containerId = containerId,
                            containerType = containerType,
                            seq = containerSeq,
                            status = BuildStatus.QUEUE,
                            controlOption = controlOption
                        )
                    )
                }
                if (needUpdateContainer) {
                    needUpdateStage = true
                }
                containerSeq++
            }

            // 非触发Stage填充默认参数
            var stageOption: PipelineBuildStageControlOption? = null
            if (index != 0) {
                stageOption = PipelineBuildStageControlOption(
                    stageControlOption = stage.stageControlOption ?: StageControlOption(
                        enable = true,
                        runCondition = StageRunCondition.AFTER_LAST_FINISHED,
                        timeout = Timeout.DEFAULT_STAGE_TIMEOUT_HOURS,
                        manualTrigger = false,
                        triggerUsers = null
                    ),
                    fastKill = stage.fastKill
                )
                if (stage.name.isNullOrBlank()) stage.name = stage.id
                if (stage.tag == null) stage.tag = listOf(defaultStageTagId)
            }

            if (stageOption?.stageControlOption?.manualTrigger == true) {
                stage.reviewStatus = BuildStatus.QUEUE.name
            }

            if (lastTimeBuildStageRecords.isNotEmpty()) {
                if (needUpdateStage) {
                    run findHistoryStage@{
                        lastTimeBuildStageRecords.forEach {
                            if (it.stageId == stageId) {
                                it.status = BuildStatus.QUEUE.ordinal
                                it.executeCount += 1
                                updateStageExistsRecord.add(it)
                                return@findHistoryStage
                            }
                        }
                    }
                }
            } else {
                buildStages.add(
                    PipelineBuildStage(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stageId,
                        seq = index,
                        status = BuildStatus.QUEUE,
                        controlOption = stageOption
                    )
                )
            }
        }
        dslContext.transaction { configuration ->

            val transactionContext = DSL.using(configuration)

            if (buildHistoryRecord != null) {
                buildHistoryRecord.status = startBuildStatus.ordinal
                transactionContext.batchStore(buildHistoryRecord).execute()
                // 重置状态和人
                buildDetailDao.update(transactionContext, buildId, JsonUtil.toJson(sModel), startBuildStatus, "")
            } else { // 创建构建记录
                // 构建号递增
                val buildNum = pipelineBuildSummaryDao.updateBuildNum(transactionContext, pipelineInfo.pipelineId)
                pipelineBuildDao.create(
                    dslContext = transactionContext,
                    projectId = pipelineInfo.projectId,
                    pipelineId = pipelineInfo.pipelineId,
                    buildId = buildId,
                    version = params[PIPELINE_VERSION] as Int,
                    buildNum = buildNum,
                    trigger = startType.name,
                    status = startBuildStatus,
                    startUser = userId,
                    triggerUser = triggerUser,
                    taskCount = taskCount,
                    firstTaskId = firstTaskId,
                    channelCode = channelCode,
                    parentBuildId = parentBuildId,
                    parentTaskId = parentTaskId,
                    webhookType = params[PIPELINE_WEBHOOK_TYPE] as String?,
                    webhookInfo = getWebhookInfo(params)
                )
                // detail记录,未正式启动，先排队状态
                buildDetailDao.create(
                    dslContext = transactionContext,
                    buildId = buildId,
                    startUser = userId,
                    startType = startType,
                    buildNum = buildNum,
                    model = JsonUtil.toJson(sModel),
                    buildStatus = BuildStatus.QUEUE
                )
                // 写入版本号
                buildVariableService.saveVariable(
                    dslContext = transactionContext,
                    projectId = pipelineInfo.projectId,
                    pipelineId = pipelineInfo.pipelineId,
                    buildId = buildId,
                    name = PIPELINE_BUILD_NUM,
                    value = buildNum
                )
            }

            // 保存参数
            buildVariableService.batchSetVariable(
                dslContext = transactionContext,
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                buildId = buildId,
                variables = startParamsWithType.map { it.key to it.value }.toMap()
            )

            // 保存链路信息
            addTraceVar(projectId = pipelineInfo.projectId, pipelineId = pipelineInfo.pipelineId, buildId = buildId)

            // 上一次存在的需要重试的任务直接Update，否则就插入
            if (updateExistsRecord.isEmpty()) {
                // 保持要执行的任务
                logger.info("batch save to pipelineBuildTask, buildTaskList size: ${buildTaskList.size}")
                pipelineBuildTaskDao.batchSave(transactionContext, buildTaskList)
            } else {
                logger.info("batch store to pipelineBuildTask, updateExistsRecord size: ${updateExistsRecord.size}")
                pipelineBuildTaskDao.batchUpdate(transactionContext, updateExistsRecord)
            }

            if (updateContainerExistsRecord.isEmpty()) {
                pipelineBuildContainerDao.batchSave(transactionContext, buildContainers)
            } else {
                pipelineBuildContainerDao.batchUpdate(transactionContext, updateContainerExistsRecord)
            }

            if (updateStageExistsRecord.isEmpty()) {
                pipelineBuildStageDao.batchSave(transactionContext, buildStages)
            } else {
                pipelineBuildStageDao.batchUpdate(transactionContext, updateStageExistsRecord)
            }
            // 排队计数+1
            pipelineBuildSummaryDao.updateQueueCount(transactionContext, pipelineInfo.pipelineId, 1)
        }

        // 发送开始事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "startBuild",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = userId,
                buildId = buildId,
                taskId = firstTaskId,
                status = startBuildStatus,
                actionType = actionType
            ), // 监控事件
            PipelineBuildMonitorEvent(
                source = "startBuild",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = userId,
                buildId = buildId,
                buildStatus = startBuildStatus
            )
        )

        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildHistoryMessage(buildId, pipelineInfo.projectId, pipelineInfo.pipelineId, userId),
            pipelineWebsocketService.buildDetailMessage(buildId, pipelineInfo.projectId, pipelineInfo.pipelineId, userId)
        )

        return buildId
    }

    private fun getWebhookInfo(params: Map<String, Any>): String? {
        if (params[PIPELINE_START_TYPE] != StartType.WEB_HOOK.name) {
            return null
        }
        return JsonUtil.toJson(
            WebhookInfo(
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE] as String?,
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL] as String?,
                webhookType = params[PIPELINE_WEBHOOK_TYPE] as String?,
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH] as String?,
                webhookEventType = params[PIPELINE_WEBHOOK_EVENT_TYPE] as String?
            )
        )
    }

    private fun calculateStartVMTaskSeq(taskSeq: Int, container: Container, atomElement: Element): Int {
        // 在当前位置插入启动构建机
        if (container is VMBuildContainer) {
            return taskSeq
        }
        // 如果是无编译环境，检查是否存在研发商店上架类插件，需要启动无编译环境机器
        if (container is NormalContainer) {
            if (atomElement is MarketBuildAtomElement || atomElement is MarketBuildLessAtomElement) {
                return taskSeq
            }
        }
        return -1
    }

    private fun supplyVMTask(
        stage: Stage,
        container: Container,
        containerSeq: Int,
        startVMTaskSeq: Int,
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        updateExistsRecord: MutableList<TPipelineBuildTaskRecord>,
        buildTaskList: MutableList<PipelineBuildTask>,
        pipelineInfo: PipelineInfo,
        retryCount: Int,
        buildId: String,
        stageId: String,
        userId: String
    ) {
        if (startVMTaskSeq <= 0) {
            return
        }

        if (lastTimeBuildTaskRecords.isEmpty()) {
            // 是否有原子市场的原子，则需要启动docker来运行
            if (container is NormalContainer) {
                buildTaskList.add(
                    DispatchBuildLessDockerStartupTaskAtom.makePipelineBuildTask(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stageId,
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = startVMTaskSeq,
                        userId = userId
                    )
                )
                buildTaskList.addAll(
                    DispatchBuildLessDockerShutdownTaskAtom.makePipelineBuildTasks(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stageId,
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = startVMTaskSeq,
                        userId = userId
                    )
                )
            } else {
                buildTaskList.add(
                    DispatchVMStartupTaskAtom.makePipelineBuildTask(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stageId,
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = startVMTaskSeq,
                        userId = userId
                    )
                )
                buildTaskList.addAll(
                    DispatchVMShutdownTaskAtom.makePipelineBuildTasks(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stageId,
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = startVMTaskSeq,
                        userId = userId
                    )
                )
            }
        } else {
            val startTaskVMId = VMUtils.genStartVMTaskId(container.id!!)
            var taskRecord = retryDetailModelStatus(
                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                stage = stage,
                container = container,
                retryCount = retryCount,
                retryStartTaskId = startTaskVMId
            )
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
            } else {
                logger.info("[$buildId]|RETRY| not found $startTaskVMId(${container.name})")
            }

            val endPointTaskId = VMUtils.genEndPointTaskId(VMUtils.genVMSeq(containerSeq = containerSeq, taskSeq = startVMTaskSeq - 1))
            taskRecord = retryDetailModelStatus(
                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                stage = stage,
                container = container,
                retryCount = retryCount,
                retryStartTaskId = endPointTaskId
            )
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
                val stopVmTaskId = VMUtils.genStopVMTaskId(VMUtils.genVMSeq(containerSeq = containerSeq, taskSeq = startVMTaskSeq))
                taskRecord = retryDetailModelStatus(
                    lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                    stage = stage,
                    container = container,
                    retryCount = retryCount,
                    retryStartTaskId = stopVmTaskId
                )
                if (taskRecord != null) {
                    updateExistsRecord.add(taskRecord)
                } else {
                    logger.warn("[$buildId]|RETRY| not found $stopVmTaskId(${container.name})")
                }
            } else {
                logger.info("[$buildId]|RETRY| not found $endPointTaskId(${container.name})")
            }
        }
    }

    /**
     * 刷新要重试的任务，如果任务是在当前容器，需要将当前容器的状态一并刷新
     * @param lastTimeBuildTaskRecords 之前重试任务记录列表
     * @param container 当前任务所在构建容器
     * @param retryStartTaskId 要重试的任务i
     * @param atomElement 需要重置状态的任务原子Element，可以为空。
     */
    private fun retryDetailModelStatus(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        stage: Stage,
        container: Container,
        retryStartTaskId: String,
        retryCount: Int,
        atomElement: Element? = null
    ): TPipelineBuildTaskRecord? {
        val target: TPipelineBuildTaskRecord? = findTaskRecord(
            lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
            container = container,
            retryStartTaskId = retryStartTaskId
        )

        if (target != null) {
            target.executeCount = retryCount + 1 // 执行次数增1
            target.status = BuildStatus.QUEUE.ordinal // 进入排队状态
            stage.status = null
            stage.startEpoch = null
            stage.elapsed = null
            container.status = null // 重置状态为空
            container.startEpoch = null
            container.elementElapsed = null
            container.systemElapsed = null
            container.executeCount = target.executeCount
            if (atomElement != null) { // 将原子状态重置
                atomElement.status = null // BuildStatus.QUEUE.name
                atomElement.executeCount = target.executeCount
                atomElement.elapsed = null
                atomElement.startEpoch = null
                atomElement.canRetry = false
                target.taskParams = JsonUtil.toJson(atomElement.genTaskParams()) // 更新参数
            }
        }
        return target
    }

    private fun findTaskRecord(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        container: Container,
        retryStartTaskId: String?
    ): TPipelineBuildTaskRecord? {
        var target: TPipelineBuildTaskRecord? = null
        run findOutRetryTask@{
            lastTimeBuildTaskRecords.forEach {
                if (it.containerId == container.id && retryStartTaskId == it.taskId) {
                    target = it
                    logger.info("found|container=${container.name}|retryStartTaskId=$retryStartTaskId")
                    return@findOutRetryTask
                }
            }
        }
        return target
    }

    /**
     * 手动完成任务
     */
    fun manualDealBuildTask(buildId: String, taskId: String, userId: String, manualAction: ManualReviewAction) {
        dslContext.transaction { configuration ->
            val transContext = DSL.using(configuration)
            val taskRecord = pipelineBuildTaskDao.get(transContext, buildId, taskId)
            if (taskRecord != null) {
                with(taskRecord) {
                    if (BuildStatus.isRunning(BuildStatus.values()[status])) {
                        val taskParam = JsonUtil.toMutableMapSkipEmpty(taskParams)
                        taskParam[BS_MANUAL_ACTION] = manualAction
                        taskParam[BS_MANUAL_ACTION_USERID] = userId
                        val result = pipelineBuildTaskDao.updateTaskParam(
                            dslContext,
                            buildId,
                            taskId,
                            JsonUtil.toJson(taskParam)
                        )
                        if (result != 1) {
                            logger.info("[{}]|taskId={}| update task param failed", buildId, taskId)
                        }
                        pipelineEventDispatcher.dispatch(
                            PipelineBuildAtomTaskEvent(
                                javaClass.simpleName,
                                projectId, pipelineId, starter, buildId, stageId,
                                containerId, containerType, taskId,
                                taskParam, ActionType.REFRESH
                            )
                        )
                    }
                }
            }
        }
    }

    fun manualDealBuildTask(buildId: String, taskId: String, userId: String, params: ReviewParam) {
        dslContext.transaction { configuration ->
            val transContext = DSL.using(configuration)
            val taskRecord = pipelineBuildTaskDao.get(transContext, buildId, taskId)
            if (taskRecord != null) {
                with(taskRecord) {
                    if (BuildStatus.isRunning(BuildStatus.values()[status])) {
                        val taskParam = JsonUtil.toMutableMapSkipEmpty(taskParams)
                        taskParam[BS_MANUAL_ACTION] = params.status.toString()
                        taskParam[BS_MANUAL_ACTION_USERID] = userId
                        taskParam[BS_MANUAL_ACTION_DESC] = params.desc ?: ""
                        taskParam[BS_MANUAL_ACTION_PARAMS] = JsonUtil.toJson(params.params)
                        taskParam[BS_MANUAL_ACTION_SUGGEST] = params.suggest ?: ""
                        val result = pipelineBuildTaskDao.updateTaskParam(
                            dslContext = dslContext,
                            buildId = buildId,
                            taskId = taskId,
                            taskParam = JsonUtil.toJson(taskParam)
                        )
                        if (result != 1) {
                            logger.info("[{}]|taskId={}| update task param failed|result:{}", buildId, taskId, result)
                        }
                        buildVariableService.batchSetVariable(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            variables = params.params.map {
                                it.key.toString() to it.value.toString()
                            }.toMap()
                        )
                        pipelineEventDispatcher.dispatch(
                            PipelineBuildAtomTaskEvent(
                                javaClass.simpleName,
                                projectId, pipelineId, starter, buildId, stageId,
                                containerId, containerType, taskId,
                                taskParam, ActionType.REFRESH
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * 认领构建任务
     */
    fun claimBuildTask(buildId: String, task: PipelineBuildTask, userId: String) {
        updateTaskStatus(buildId, task, userId, BuildStatus.RUNNING)
    }

    /**
     * 完成认领构建的任务
     */
    fun completeClaimBuildTask(
        buildId: String,
        taskId: String,
        userId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ) {
        val buildTask = getBuildTask(buildId = buildId, taskId = taskId)
        if (buildTask != null) {
            updateTaskStatus(
                buildId = buildId,
                task = buildTask,
                userId = userId,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
            // 刷新容器，下发后面的任务
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(
                    source = "completeClaimBuildTask",
                    projectId = buildTask.projectId,
                    pipelineId = buildTask.pipelineId,
                    userId = userId,
                    buildId = buildTask.buildId,
                    stageId = buildTask.stageId,
                    containerId = buildTask.containerId,
                    containerType = buildTask.containerType,
                    actionType = ActionType.REFRESH
                )
            )
        }
    }

    fun updateBuildNo(pipelineId: String, buildNo: Int) {
        pipelineBuildSummaryDao.updateBuildNo(dslContext, pipelineId, buildNo)
    }

    fun getBuildNo(pipelineId: String): Int {
        return pipelineBuildSummaryDao.getBuildNo(dslContext, pipelineId)
    }

    /**
     * 开始最新一次构建
     */
    fun startLatestRunningBuild(latestRunningBuild: LatestRunningBuild, retry: Boolean) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            buildDetailDao.updateStatus(
                transactionContext,
                latestRunningBuild.buildId,
                BuildStatus.RUNNING,
                LocalDateTime.now()
            )
            pipelineBuildDao.startBuild(transactionContext, latestRunningBuild.buildId, retry)
            pipelineBuildSummaryDao.startLatestRunningBuild(transactionContext, latestRunningBuild)
        }
        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildHistoryMessage(
                buildId = latestRunningBuild.buildId,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                userId = latestRunningBuild.userId
            ),
            pipelineWebsocketService.buildDetailMessage(
                buildId = latestRunningBuild.buildId,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                userId = latestRunningBuild.userId
            ),
            pipelineWebsocketService.buildStatusMessage(
                buildId = latestRunningBuild.buildId,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                userId = latestRunningBuild.userId
            )
        )

        logger.info("[${latestRunningBuild.pipelineId}]|startLatestRunningBuild-${latestRunningBuild.buildId}")
    }

    /**
     * 结束构建
     * @param latestRunningBuild 最一次构建的要更新的状态信息
     * @param currentBuildStatus 当前一次构建的当前状态
     */
    fun finishLatestRunningBuild(
        latestRunningBuild: LatestRunningBuild,
        currentBuildStatus: BuildStatus,
        errorInfoList: List<ErrorInfo>?
    ) {
        if (BuildStatus.isReadyToRun(currentBuildStatus)) {
            // 减1,当作没执行过
            pipelineBuildSummaryDao.updateQueueCount(dslContext, latestRunningBuild.pipelineId, -1)
        } else {
            pipelineBuildSummaryDao.finishLatestRunningBuild(
                dslContext = dslContext,
                latestRunningBuild = latestRunningBuild,
                isStageFinish = currentBuildStatus.name == BuildStatus.STAGE_SUCCESS.name
            )
        }
        with(latestRunningBuild) {
            val executeTime = try {
                getExecuteTime(pipelineId, buildId)
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|getExecuteTime-$buildId exception:", e)
                0L
            }
            logger.info("[$pipelineId]|getExecuteTime-$buildId executeTime: $executeTime")

            val buildParameters: List<BuildParameters> = try {
                getBuildParameters(buildId)
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|getBuildParameters-$buildId exception:", e)
                mutableListOf()
            }
            logger.info("[$pipelineId]|getBuildParameters-$buildId buildParameters: ${JsonUtil.toJson(buildParameters)}")

            val recommendVersion = try {
                getRecommendVersion(buildParameters)
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|getRecommendVersion-$buildId exception:", e)
                null
            }
            logger.info("[$pipelineId]|getRecommendVersion-$buildId recommendVersion: $recommendVersion")
            val remark = buildVariableService.getVariable(buildId, PIPELINE_BUILD_REMARK)
            val finalStatus = if (BuildStatus.isFinish(status) || status.name == BuildStatus.STAGE_SUCCESS.name) {
                status
            } else {
                BuildStatus.FAILED
            }
            pipelineBuildDao.finishBuild(
                dslContext = dslContext,
                buildId = buildId,
                buildStatus = finalStatus,
                executeTime = executeTime,
                buildParameters = JsonUtil.toJson(buildParameters),
                recommendVersion = recommendVersion,
                remark = remark,
                errorInfoList = errorInfoList
            )
            webSocketDispatcher.dispatch(
                pipelineWebsocketService.buildHistoryMessage(
                    buildId = latestRunningBuild.buildId,
                    projectId = latestRunningBuild.projectId,
                    pipelineId = latestRunningBuild.pipelineId,
                    userId = latestRunningBuild.userId
                ),
                pipelineWebsocketService.buildDetailMessage(
                    buildId = latestRunningBuild.buildId,
                    projectId = latestRunningBuild.projectId,
                    pipelineId = latestRunningBuild.pipelineId,
                    userId = latestRunningBuild.userId
                ),
                pipelineWebsocketService.buildStatusMessage(
                    buildId = latestRunningBuild.buildId,
                    projectId = latestRunningBuild.projectId,
                    pipelineId = latestRunningBuild.pipelineId,
                    userId = latestRunningBuild.userId
                )
            )
            logger.info("[$pipelineId]|finishLatestRunningBuild-$buildId|status=$status")
        }
    }

    fun getRecommendVersion(buildParameters: List<BuildParameters>): String? {
        val majorVersion = if (!buildParameters.none { it.key == MAJORVERSION || it.key == "MajorVersion" }) {
            buildParameters.filter { it.key == MAJORVERSION || it.key == "MajorVersion" }[0].value.toString()
        } else return null

        val minorVersion = if (!buildParameters.none { it.key == MINORVERSION || it.key == "MinorVersion" }) {
            buildParameters.filter { it.key == MINORVERSION || it.key == "MinorVersion" }[0].value.toString()
        } else return null

        val fixVersion = if (!buildParameters.none { it.key == FIXVERSION || it.key == "FixVersion" }) {
            buildParameters.filter { it.key == FIXVERSION || it.key == "FixVersion" }[0].value.toString()
        } else return null

        val buildNo = if (!buildParameters.none { it.key == BUILD_NO || it.key == "BuildNo" }) {
            buildParameters.filter { it.key == BUILD_NO || it.key == "BuildNo" }[0].value.toString()
        } else return null

        return "$majorVersion.$minorVersion.$fixVersion.$buildNo"
    }

    private fun getBuildParameters(buildId: String): List<BuildParameters> {
        return try {
            val startupParam = buildStartupParamService.getParam(buildId)
            if (startupParam == null || startupParam.isEmpty()) {
                emptyList()
            } else {
                val map: Map<String, Any> = JsonUtil.toMap(startupParam)
                map.map { transform ->
                    BuildParameters(transform.key, transform.value)
                }.toList().filter { !it.key.startsWith(SkipElementUtils.prefix) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getExecuteTime(pipelineId: String, buildId: String): Long {
        val executeTask = pipelineBuildTaskDao.getByBuildId(dslContext, buildId)
            .filter { it.taskType != ManualReviewUserTaskElement.classType }
        var executeTime = 0L
        executeTask.forEach {
            executeTime += it.totalTime ?: 0
        }
        return executeTime
    }

    fun getLastTimeBuild(projectId: String, pipelineId: String): BuildInfo? {
        return pipelineBuildDao.convert(pipelineBuildDao.getLatestBuild(dslContext, projectId, pipelineId))
    }

    fun updateTaskSubBuildId(buildId: String, taskId: String, subBuildId: String) {
        pipelineBuildTaskDao.updateSubBuildId(
            dslContext = dslContext,
            buildId = buildId,
            taskId = taskId,
            subBuildId = subBuildId
        )
    }

    fun setTaskErrorInfo(
        buildId: String,
        taskId: String,
        errorType: ErrorType,
        errorCode: Int,
        errorMsg: String
    ) {
        pipelineBuildTaskDao.setTaskErrorInfo(
            dslContext = dslContext,
            buildId = buildId,
            taskId = taskId,
            errorType = errorType,
            errorCode = errorCode,
            errorMsg = errorMsg
        )
    }

    fun updateTaskStatus(
        buildId: String,
        taskId: String,
        userId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ) {
        logger.info("[$buildId]|updateTaskStatus|taskId=$taskId|buildStatus=$buildStatus|errorType=$errorType|errorCode=$errorCode|errorMsg=$errorMsg")
        val task = getBuildTask(buildId, taskId)
        if (task != null) {
            updateTaskStatus(
                buildId = buildId,
                task = task,
                userId = userId,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
            if (buildStatus == BuildStatus.SKIP) {
                SpringContextUtil.getBean(PipelineBuildDetailService::class.java).taskSkip(buildId, taskId)
            }
        }
    }

    private fun updateTaskStatus(
        buildId: String,
        task: PipelineBuildTask,
        userId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            logger.info("$buildId|${task.taskName} update status, status: ${buildStatus.name}, userId: $userId")
            pipelineBuildTaskDao.updateStatus(
                dslContext = transactionContext,
                buildId = buildId,
                taskId = task.taskId,
                userId = userId,
                buildStatus = buildStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
            pipelineBuildSummaryDao.updateCurrentBuildTask(
                dslContext = transactionContext,
                pipelineId = task.pipelineId,
                buildId = buildId,
                currentTaskId = task.taskId,
                currentTaskName = task.taskName
            )
        }
        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildStatusMessage(
                buildId = buildId,
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = userId
            )
        )
    }

    fun getPipelineBuildHistoryCount(projectId: String, pipelineId: String): Int {
        return pipelineBuildDao.count(dslContext = dslContext, projectId = projectId, pipelineId = pipelineId)
    }

    fun getPipelineBuildHistoryCount(
        projectId: String,
        pipelineId: String,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?
    ): Int {
        return pipelineBuildDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            materialAlias = materialAlias,
            materialUrl = materialUrl,
            materialBranch = materialBranch,
            materialCommitId = materialCommitId,
            materialCommitMessage = materialCommitMessage,
            status = status,
            trigger = trigger,
            queueTimeStartTime = queueTimeStartTime,
            queueTimeEndTime = queueTimeEndTime,
            startTimeStartTime = startTimeStartTime,
            startTimeEndTime = startTimeEndTime,
            endTimeStartTime = endTimeStartTime,
            endTimeEndTime = endTimeEndTime,
            totalTimeMin = totalTimeMin,
            totalTimeMax = totalTimeMax,
            remark = remark,
            buildNoStart = buildNoStart,
            buildNoEnd = buildNoEnd
        )
    }

    fun getAllBuildNum(projectId: String, pipelineId: String): Collection<Int> {
        return pipelineBuildDao.listPipelineBuildNum(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = 0,
            limit = Int.MAX_VALUE
        )
    }

    // 性能点
    fun totalRunningBuildCount(): Int {
        return pipelineBuildDao.countAllByStatus(dslContext, BuildStatus.RUNNING)
    }

    // 获取流水线最后的构建号
    fun getLatestBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestBuild(dslContext, projectId, pipelineId)?.buildId
    }

    // 获取流水线最后完成的构建号
    fun getLatestFinishedBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestFinishedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    // 获取流水线最后成功的构建号
    fun getLatestSucceededBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestSuccessedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    // 获取流水线最后失败的构建号
    fun getLatestFailedBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestFailedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    fun getBuildIdbyBuildNo(projectId: String, pipelineId: String, buildNo: Int): String? {
        return pipelineBuildDao.getBuildByBuildNo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildNo = buildNo
        )?.buildId
    }

    fun saveBuildVmInfo(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, vmInfo: VmInfo) {
        val record = buildDetailDao.get(dslContext, buildId)
        if (record == null) {
            logger.warn("build not exists, buildId: $buildId")
            return
        }
        val model = JsonUtil.getObjectMapper().readValue(record.model, Model::class.java)
        model.stages.forEach s@{ stage ->
            stage.containers.forEach c@{ container ->
                if (container is VMBuildContainer && container.showBuildResource == true && container.id == vmSeqId) {
                    container.name = vmInfo.name
                    buildDetailDao.updateModel(
                        dslContext = dslContext,
                        buildId = buildId,
                        model = JsonUtil.toJson(model)
                    )
                    return
                }
            }
        }
    }

    fun updateBuildInfoStatus2Queue(buildId: String, oldStatus: BuildStatus) {
        pipelineBuildDao.updateStatus(dslContext, buildId, oldStatus, BuildStatus.QUEUE)
    }

    fun updateArtifactList(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactListJsonString: String
    ): Boolean {
        return pipelineBuildDao.updateArtifactList(
            dslContext = dslContext,
            artifactList = artifactListJsonString,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        ) == 1
    }

    /**
     * 如果是重试，不应该更新启动参数, 直接返回
     */
    fun writeStartParam(projectId: String, pipelineId: String, buildId: String, model: Model) {
        val allVariable = buildVariableService.getAllVariable(buildId)
        if (allVariable[PIPELINE_RETRY_COUNT] != null) return

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val params = allVariable.filter {
            it.key.startsWith(SkipElementUtils.prefix) || it.key == BUILD_NO || it.key == PIPELINE_RETRY_COUNT
        }.toMutableMap()
        if (triggerContainer.buildNo != null) {
            val buildNo = getBuildNo(pipelineId)
            buildVariableService.setVariable(
                projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, varName = BUILD_NO, varValue = buildNo
            )
            params[BUILD_NO] = buildNo.toString()
        }

        if (triggerContainer.params.isNotEmpty()) {
            // 只有在构建参数中的才设置
            params.putAll(
                triggerContainer.params.map {
                    // 做下真实传值的替换
                    if (allVariable.containsKey(it.id)) it.id to allVariable[it.id].toString()
                    else it.id to it.defaultValue.toString()
                }.toMap()
            )
            buildStartupParamService.addParam(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                param = JsonUtil.getObjectMapper().writeValueAsString(params)
            )
        }
    }

    private fun addTraceVar(projectId: String, pipelineId: String, buildId: String) {
        val traceMap = mutableMapOf<String, String>()
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrEmpty()) {
            traceMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] = MDC.get(TraceTag.BIZID)
            buildVariableService.batchSetVariable(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    variables = traceMap
            )
        }
    }
}
