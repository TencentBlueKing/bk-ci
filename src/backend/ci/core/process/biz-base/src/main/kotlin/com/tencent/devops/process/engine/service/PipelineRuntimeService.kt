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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_DESC
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.context.StartBuildContext
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.control.lock.PipelineBuildNoLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.builds.CompleteTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM_ALIAS
import com.tencent.devops.process.utils.PIPELINE_BUILD_REMARK
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
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
@Suppress(
    "LongParameterList",
    "LargeClass",
    "TooManyFunctions",
    "MagicNumber",
    "ComplexMethod",
    "LongMethod",
    "ReturnCount",
    "NestedBlockDepth"
)
@Service
class PipelineRuntimeService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val stageTagService: StageTagService,
    private val buildIdGenerator: BuildIdGenerator,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildDetailDao: BuildDetailDao,
    private val buildVariableService: BuildVariableService,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuleService: PipelineRuleService,
    private val projectCacheService: ProjectCacheService,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
    }

    fun deletePipelineBuilds(projectId: String, pipelineId: String) {
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
            pipelineStageService.deletePipelineBuildStages(
                transactionContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineContainerService.deletePipelineBuildContainers(
                transactionContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            pipelineTaskService.deletePipelineBuildTasks(
                transactionContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
        }
        buildVariableService.deletePipelineBuildVar(projectId = projectId, pipelineId = pipelineId)
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
        buildNoEnd: Int?,
        buildMsg: String?
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
            buildNoEnd = buildNoEnd,
            buildMsg = buildMsg
        )
        val result = mutableListOf<BuildHistory>()
        val buildStatus = BuildStatus.values()
        list.forEach {
            result.add(genBuildHistory(it, buildStatus, currentTimestamp))
        }
        return result
    }

    fun updateBuildRemark(projectId: String, pipelineId: String, buildId: String, remark: String?) {
        pipelineBuildDao.updateBuildRemark(dslContext, projectId, pipelineId, buildId, remark)
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
                executeTime = executeTime ?: 0L,
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
                    } catch (ignored: Exception) {
                        null
                    }
                } else {
                    null
                },
                buildMsg = BuildMsgUtils.getBuildMsg(
                    buildMsg = buildMsg,
                    startType = StartType.toStartType(trigger),
                    channelCode = ChannelCode.valueOf(channel)
                ),
                buildNumAlias = buildNumAlias
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
        if (records.isEmpty()) {
            return result
        }

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
        if (records.isEmpty()) {
            return result
        }
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

    fun startBuild(
        pipelineInfo: PipelineInfo,
        fullModel: Model,
        originStartParams: List<BuildParameters>,
        startParamsWithType: List<BuildParameters>,
        buildNo: Int? = null,
        buildNumRule: String? = null,
        acquire: Boolean? = false
    ): String {
        val startParamMap = startParamsWithType.associate { it.key to it.value }
        val startBuildStatus: BuildStatus = BuildStatus.QUEUE // 默认都是排队状态
        // 2019-12-16 产品 rerun 需求
        val pipelineId = pipelineInfo.pipelineId
        val buildId = startParamMap[PIPELINE_RETRY_BUILD_ID]?.toString() ?: buildIdGenerator.getNextId()
        val projectName = projectCacheService.getProjectName(pipelineInfo.projectId) ?: ""
        val context = StartBuildContext.init(startParamMap)

        val updateTaskExistsRecord: MutableList<TPipelineBuildTaskRecord> = mutableListOf()
        val defaultStageTagId by lazy { stageTagService.getDefaultStageTag().data?.id }
        val lastTimeBuildTaskRecords = pipelineTaskService.listByBuildId(buildId)
        val lastTimeBuildContainerRecords = pipelineContainerService.listByBuildId(buildId)
        val lastTimeBuildStageRecords = pipelineStageService.listByBuildId(buildId)

        val buildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, buildId)

        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val buildContainers = mutableListOf<PipelineBuildContainer>()
        val buildStages = mutableListOf<PipelineBuildStage>()

        val updateStageExistsRecord: MutableList<TPipelineBuildStageRecord> = mutableListOf()
        val updateContainerExistsRecord: MutableList<TPipelineBuildContainerRecord> = mutableListOf()

        var currentBuildNo = buildNo
        var buildNoType: BuildNoType? = null
        // --- 第1层循环：Stage遍历处理 ---
        fullModel.stages.forEachIndexed nextStage@{ index, stage ->
            var needUpdateStage = stage.finally // final stage 每次重试都会参与执行检查

            // #2318 如果是stage重试不是当前stage且当前stage已经是完成状态，或者该stage被禁用，则直接跳过
            if (context.needSkipWhenStageFailRetry(stage) || stage.stageControlOption?.enable == false) {
                logger.info("[$buildId|EXECUTE|#${stage.id!!}|${stage.status}|NOT_EXECUTE_STAGE")
                context.containerSeq += stage.containers.size // Job跳过计数也需要增加
                return@nextStage
            }

            DependOnUtils.initDependOn(stage = stage, params = startParamMap)
            // --- 第2层循环：Container遍历处理 ---
            stage.containers.forEach nextContainer@{ container ->
                // #4518 Model中的container.containerId转移至container.containerHashId，进行新字段值补充
                container.containerHashId = container.containerHashId ?: container.containerId
                container.containerId = container.id

                if (container is TriggerContainer) { // 寻找触发点
                    val buildNoObj = container.buildNo
                    if (buildNoObj != null && context.actionType == ActionType.START) {
                        buildNoType = buildNoObj.buildNoType
                        val buildNoLock = if (acquire != true) PipelineBuildNoLock(
                            redisOperation = redisOperation,
                            pipelineId = pipelineId
                        ) else null
                        try {
                            buildNoLock?.lock()
                            if (buildNoType == BuildNoType.CONSISTENT) {
                                if (currentBuildNo != null) {
                                    // 只有用户勾选中"锁定构建号"这种类型才允许指定构建号
                                    updateBuildNo(pipelineId, currentBuildNo!!)
                                    logger.info("[$pipelineId] buildNo was changed to [$currentBuildNo]")
                                }
                            } else if (buildNoType == BuildNoType.EVERY_BUILD_INCREMENT) {
                                val buildSummary = getBuildSummaryRecord(pipelineId)
                                // buildNo根据数据库的记录值每次新增1
                                currentBuildNo = if (buildSummary == null || buildSummary.buildNo == null) {
                                    1
                                } else buildSummary.buildNo + 1
                                updateBuildNo(pipelineId, currentBuildNo!!)
                            }
                            // 兼容buildNo为空的情况
                            if (currentBuildNo == null) {
                                currentBuildNo = getBuildSummaryRecord(pipelineId)?.buildNo
                                    ?: buildNoObj.buildNo
                            }
                        } finally {
                            buildNoLock?.unlock()
                        }
                    }
                    container.executeCount = context.executeCount
                    container.elements.forEach { atomElement ->
                        if (context.firstTaskId.isBlank() && atomElement.isElementEnable()) {
                            context.firstTaskId = atomElement.findFirstTaskIdByStartType(context.startType)
                        }
                    }
                    context.containerSeq++
                    return@nextContainer
                } else if (container is NormalContainer) {
                    if (!ContainerUtils.isNormalContainerEnable(container)) {
                        context.containerSeq++
                        return@nextContainer
                    }
                } else if (container is VMBuildContainer) {
                    if (!ContainerUtils.isVMBuildContainerEnable(container)) {
                        context.containerSeq++
                        return@nextContainer
                    }
                }
                /* #2318
                    原则：当存在多个失败插件时，进行失败插件重试时，一次只能对单个插件进行重试，其他失败插件不会重试，所以：
                    如果是插件失败重试，并且当前的Job状态是失败的，则检查重试的插件是不是属于该失败Job:
                    如果不属于，则表示该Job在本次重试不会被执行到，则不做处理，保持原状态, 跳过
                 */
                if (context.needSkipContainerWhenFailRetry(stage, container) &&
                    lastTimeBuildContainerRecords.isNotEmpty()) {
                    if (null == pipelineContainerService.findTaskRecord(
                            lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                            container = container,
                            retryStartTaskId = context.retryStartTaskId)) {

                        logger.info("[$buildId|RETRY_SKIP_JOB|j(${container.id!!})|${container.name}")
                        context.containerSeq++
                        return@nextContainer
                    }
                }

                /*
                    #3138 整合重试Stage下所有失败Job的功能，并对finallyStage做了特殊处理：
                    finallyStage如果不是属于重试的Stage，则需要将所有状态重置，不允许跳过
                */
                if (context.isRetryFailedContainer(container = container, stage = stage)) {
                    logger.info("[$buildId|RETRY_SKIP_SUCCESSFUL_JOB|j(${container.containerId})|${container.name}")
                    context.containerSeq++
                    return@nextContainer
                }

                /*
                    #4518 整合组装Task和刷新已有Container的逻辑
                    构建矩阵特殊处理，即使重试也要重新计算执行策略
                */
                if (container.matrixGroupFlag == true) {
                    if (container is VMBuildContainer) container.retryFreshMatrixOption()
                    else if (container is NormalContainer) container.retryFreshMatrixOption()
                    pipelineContainerService.deleteTasksInMatrixGroupContainer(
                        transactionContext = dslContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId
                    )
                }
                // --- 第3层循环：Element遍历处理 ---
                needUpdateStage = pipelineContainerService.prepareBuildContainerTasks(
                    projectId = pipelineInfo.projectId,
                    pipelineId = pipelineInfo.pipelineId,
                    buildId = buildId,
                    container = container,
                    startParamMap = startParamMap,
                    context = context,
                    stage = stage,
                    buildContainers = buildContainers,
                    buildTaskList = buildTaskList,
                    updateContainerExistsRecord = updateContainerExistsRecord,
                    updateTaskExistsRecord = updateTaskExistsRecord,
                    lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                    lastTimeBuildContainerRecords = lastTimeBuildContainerRecords
                )
                context.containerSeq++
            }

            // 非触发Stage填充默认参数
            var stageOption: PipelineBuildStageControlOption? = null
            if (index != 0) {
                stageOption = PipelineBuildStageControlOption(
                    stageControlOption = stage.stageControlOption ?: StageControlOption(
                        enable = true,
                        runCondition = StageRunCondition.AFTER_LAST_FINISHED,
                        timeout = Timeout.DEFAULT_STAGE_TIMEOUT_HOURS
                    ),
                    finally = stage.finally,
                    fastKill = stage.fastKill
                )
                if (stage.name.isNullOrBlank()) stage.name = stage.id
                if (stage.tag == null) stage.tag = listOf(defaultStageTagId)
            }

            // TODO 只在第一次启动时刷新为QUEUE，后续只需保留兼容数据刷新
            stage.refreshReviewOption(true)

            if (lastTimeBuildStageRecords.isNotEmpty()) {
                if (needUpdateStage) {
                    run findHistoryStage@{
                        lastTimeBuildStageRecords.forEach {
                            if (it.stageId == stage.id!!) {
                                it.status = BuildStatus.QUEUE.ordinal
                                it.startTime = null
                                it.endTime = null
                                it.executeCount = context.executeCount
                                it.checkIn = stage.checkIn?.let { self -> JsonUtil.toJson(self, formatted = false) }
                                it.checkOut = stage.checkOut?.let { self -> JsonUtil.toJson(self, formatted = false) }
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
                        stageId = stage.id!!,
                        seq = index,
                        status = BuildStatus.QUEUE,
                        controlOption = stageOption,
                        checkIn = stage.checkIn,
                        checkOut = stage.checkOut
                    )
                )
            }
        }
        val lock = if (!buildNumRule.isNullOrBlank()) {
            RedisLock(redisOperation, "process:build:history:lock:$pipelineId", 10)
        } else null
        try {
            lock?.lock()
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                // 保存参数 过滤掉不需要保存持久化的临时参数
                val buildVariables = startParamsWithType
                    .filter { it.valueType != BuildFormPropertyType.TEMPORARY }.toMutableList()
                buildVariables.add(BuildParameters(PIPELINE_BUILD_ID, buildId, BuildFormPropertyType.STRING))
                buildVariables.add(BuildParameters(PROJECT_NAME, pipelineInfo.projectId, BuildFormPropertyType.STRING))
                buildVariables.add(BuildParameters(PROJECT_NAME_CHINESE, projectName, BuildFormPropertyType.STRING))

                buildVariableService.batchSetVariable(
                    dslContext = transactionContext,
                    projectId = pipelineInfo.projectId,
                    pipelineId = pipelineInfo.pipelineId,
                    buildId = buildId,
                    variables = buildVariables
                )

                if (buildHistoryRecord != null) {
                    if (!context.stageRetry &&
                        context.actionType.isRetry() &&
                        context.retryStartTaskId.isNullOrEmpty()) {
                        // 完整重试,重置启动时间
                        buildHistoryRecord.startTime = LocalDateTime.now()
                    }
                    buildHistoryRecord.endTime = null
                    buildHistoryRecord.queueTime = LocalDateTime.now() // for EPC
                    buildHistoryRecord.status = startBuildStatus.ordinal
                    transactionContext.batchStore(buildHistoryRecord).execute()
                    // 重置状态和人
                    buildDetailDao.update(
                        dslContext = transactionContext,
                        buildId = buildId,
                        model = JsonUtil.toJson(fullModel, formatted = false),
                        buildStatus = startBuildStatus,
                        cancelUser = ""
                    )
                } else { // 创建构建记录
                    val buildNumAlias = if (!buildNumRule.isNullOrBlank()) {
                        val parsedValue = pipelineRuleService.parsePipelineRule(
                            pipelineId = pipelineId,
                            buildId = buildId,
                            busCode = PipelineRuleBusCodeEnum.BUILD_NUM.name,
                            ruleStr = buildNumRule
                        )
                        if (parsedValue.length > 256) parsedValue.substring(0, 256) else parsedValue
                    } else null
                    // 写自定义构建号信息
                    if (!buildNumAlias.isNullOrBlank()) {
                        buildVariableService.setVariable(
                            projectId = pipelineInfo.projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            varName = PIPELINE_BUILD_NUM_ALIAS,
                            varValue = buildNumAlias
                        )
                    }
                    // 构建号递增
                    val buildNum = pipelineBuildSummaryDao.updateBuildNum(
                        dslContext = transactionContext,
                        pipelineId = pipelineId,
                        buildNumAlias = buildNumAlias
                    )
                    buildVariableService.setVariable(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        varName = PIPELINE_BUILD_NUM,
                        varValue = buildNum
                    )
                    pipelineBuildDao.create(
                        dslContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        version = startParamMap[PIPELINE_VERSION] as Int,
                        buildNum = buildNum,
                        trigger = context.startType.name,
                        status = startBuildStatus,
                        startUser = context.userId,
                        triggerUser = context.triggerUser,
                        taskCount = context.taskCount,
                        firstTaskId = context.firstTaskId,
                        channelCode = context.channelCode,
                        parentBuildId = context.parentBuildId,
                        parentTaskId = context.parentTaskId,
                        buildParameters = originStartParams.plus(
                            BuildParameters(
                                key = BUILD_NO,
                                value = currentBuildNo.toString()
                            )
                        ),
                        webhookType = startParamMap[PIPELINE_WEBHOOK_TYPE] as String?,
                        webhookInfo = getWebhookInfo(startParamMap),
                        buildMsg = getBuildMsg(startParamMap[PIPELINE_BUILD_MSG] as String?),
                        buildNumAlias = buildNumAlias
                    )
                    // detail记录,未正式启动，先排队状态
                    buildDetailDao.create(
                        dslContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        buildId = buildId,
                        startUser = context.userId,
                        startType = context.startType,
                        buildNum = buildNum,
                        model = JsonUtil.toJson(fullModel, formatted = false),
                        buildStatus = BuildStatus.QUEUE
                    )
                    // 写入BuildNo
                    if (buildNoType != BuildNoType.SUCCESS_BUILD_INCREMENT && currentBuildNo != null &&
                        context.actionType == ActionType.START
                    ) {
                        buildVariableService.setVariable(
                            projectId = pipelineInfo.projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            varName = BUILD_NO,
                            varValue = currentBuildNo.toString()
                        )
                    }
                    // 设置流水线每日构建次数
                    pipelineSettingService.setCurrentDayBuildCount(transactionContext, pipelineId)
                }

                // 保存链路信息
                addTraceVar(projectId = pipelineInfo.projectId, pipelineId = pipelineInfo.pipelineId, buildId = buildId)

                // 上一次存在的需要重试的任务直接Update，否则就插入
                if (updateTaskExistsRecord.isEmpty()) {
                    // 保持要执行的任务
                    logger.info("batch save to pipelineBuildTask, buildTaskList size: ${buildTaskList.size}")
                    pipelineTaskService.batchSave(transactionContext, buildTaskList)
                } else {
                    logger.info("batch store to pipelineBuildTask, " +
                        "updateExistsRecord size: ${updateTaskExistsRecord.size}")
                    pipelineTaskService.batchUpdate(transactionContext, updateTaskExistsRecord)
                }

                if (updateContainerExistsRecord.isEmpty()) {
                    pipelineContainerService.batchSave(transactionContext, buildContainers)
                } else {
                    pipelineContainerService.batchUpdate(transactionContext, updateContainerExistsRecord)
                }

                if (updateStageExistsRecord.isEmpty()) {
                    pipelineStageService.batchSave(transactionContext, buildStages)
                } else {
                    pipelineStageService.batchUpdate(transactionContext, updateStageExistsRecord)
                }
                // 排队计数+1
                pipelineBuildSummaryDao.updateQueueCount(transactionContext, pipelineInfo.pipelineId, 1)
            }
        } finally {
            lock?.unlock()
        }

        // 发送开始事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "startBuild",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = context.userId,
                buildId = buildId,
                taskId = context.firstTaskId,
                status = startBuildStatus,
                actionType = context.actionType,
                buildNoType = buildNoType
            ), // 监控事件
            PipelineBuildMonitorEvent(
                source = "startBuild",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = context.userId,
                buildId = buildId,
                buildStatus = startBuildStatus,
                executeCount = context.executeCount
            ), // #3400 点启动处于DETAIL界面，以操作人视角，没有刷历史列表的必要，在buildStart真正启动时也会有HISTORY，减少负载
            PipelineBuildWebSocketPushEvent(
                source = "startBuild",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = context.userId,
                buildId = buildId,
                // 刷新历史列表和详情页面
                refreshTypes = RefreshType.DETAIL.binary
            ), // 广播构建排队事件
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = pipelineInfo.projectId,
                pipelineId = pipelineInfo.pipelineId,
                userId = context.userId,
                buildId = buildId,
                actionType = context.actionType,
                triggerType = context.startType.name
            )
        )

        return buildId
    }

    private fun getWebhookInfo(params: Map<String, Any>): String? {
        if (params[PIPELINE_START_TYPE] != StartType.WEB_HOOK.name) {
            return null
        }
        return JsonUtil.toJson(
            bean = WebhookInfo(
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE] as String?,
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL] as String?,
                webhookType = params[PIPELINE_WEBHOOK_TYPE] as String?,
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH] as String?,
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = if (params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name) {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] as String?
                } else {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE] as String?
                },
                webhookCommitId = params[PIPELINE_WEBHOOK_REVISION] as String?,
                webhookMergeCommitSha = params[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA] as String?
            ),
            formatted = false
        )
    }

    private fun getBuildMsg(buildMsg: String?): String? {
        return buildMsg?.substring(0, buildMsg.length.coerceAtMost(255))
    }

    /**
     * 手动完成任务
     */
    fun manualDealBuildTask(buildId: String, taskId: String, userId: String, manualAction: ManualReviewAction) {
        dslContext.transaction { configuration ->
            val transContext = DSL.using(configuration)
            val taskRecord = pipelineTaskService.getTaskRecord(transContext, buildId, taskId)
            if (taskRecord != null) {
                with(taskRecord) {
                    if (BuildStatus.values()[status].isRunning()) {
                        val taskParam = JsonUtil.toMutableMap(taskParams)
                        taskParam[BS_MANUAL_ACTION] = manualAction
                        taskParam[BS_MANUAL_ACTION_USERID] = userId
                        val result = pipelineTaskService.updateTaskParam(
                            transactionContext = transContext,
                            buildId = buildId,
                            taskId = taskId,
                            taskParam = JsonUtil.toJson(taskParam, formatted = false)
                        )
                        if (result != 1) {
                            logger.info("[{}]|taskId={}| update task param failed", buildId, taskId)
                        }
                        pipelineEventDispatcher.dispatch(
                            PipelineBuildAtomTaskEvent(
                                source = javaClass.simpleName,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                userId = starter,
                                buildId = buildId,
                                stageId = stageId,
                                containerId = containerId,
                                containerHashId = containerHashId,
                                containerType = containerType,
                                taskId = taskId,
                                taskParam = taskParam,
                                actionType = ActionType.REFRESH
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
            val taskRecord = pipelineTaskService.getTaskRecord(transContext, buildId, taskId)
            if (taskRecord != null) {
                with(taskRecord) {
                    if (BuildStatus.values()[status].isRunning()) {
                        val taskParam = JsonUtil.toMutableMap(taskParams)
                        taskParam[BS_MANUAL_ACTION] = params.status.toString()
                        taskParam[BS_MANUAL_ACTION_USERID] = userId
                        taskParam[BS_MANUAL_ACTION_DESC] = params.desc ?: ""
                        taskParam[BS_MANUAL_ACTION_PARAMS] = JsonUtil.toJson(params.params, formatted = false)
                        taskParam[BS_MANUAL_ACTION_SUGGEST] = params.suggest ?: ""
                        val result = pipelineTaskService.updateTaskParam(
                            transactionContext = transContext,
                            buildId = buildId,
                            taskId = taskId,
                            taskParam = JsonUtil.toJson(taskParam, formatted = false)
                        )
                        if (result != 1) {
                            logger.info("[{}]|taskId={}| update task param failed|result:{}", buildId, taskId, result)
                        }
                        buildVariableService.batchUpdateVariable(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            variables = params.params.associate { it.key to it.value.toString() }
                        )
                        pipelineEventDispatcher.dispatch(
                            PipelineBuildAtomTaskEvent(
                                source = "manualDealBuildTask", projectId = projectId, pipelineId = pipelineId,
                                userId = starter, buildId = buildId, stageId = stageId, containerId = containerId,
                                containerHashId = containerHashId, containerType = containerType, taskId = taskId,
                                taskParam = taskParam, actionType = ActionType.REFRESH
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
    fun claimBuildTask(task: PipelineBuildTask, userId: String) {
        pipelineTaskService.updateTaskStatus(task = task, userId = userId, buildStatus = BuildStatus.RUNNING)
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "updateTaskStatus",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = userId,
                buildId = task.buildId,
                refreshTypes = RefreshType.STATUS.binary
            )
        )
    }

    /**
     * 完成认领构建的任务[completeTask]
     * [endBuild]表示最后一步，当前容器要结束
     */
    fun completeClaimBuildTask(completeTask: CompleteTask, endBuild: Boolean = false): PipelineBuildTask? {
        val buildTask = pipelineTaskService.getBuildTask(buildId = completeTask.buildId, taskId = completeTask.taskId)
        if (buildTask != null) {
            pipelineTaskService.updateTaskStatus(
                task = buildTask,
                userId = completeTask.userId,
                buildStatus = completeTask.buildStatus,
                errorType = completeTask.errorType,
                errorCode = completeTask.errorCode,
                errorMsg = completeTask.errorMsg
            )
            // 刷新容器，下发后面的任务
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "updateTaskStatus",
                    projectId = buildTask.projectId,
                    pipelineId = buildTask.pipelineId,
                    userId = buildTask.starter,
                    buildId = buildTask.buildId,
                    refreshTypes = RefreshType.STATUS.binary
                ),
                PipelineBuildContainerEvent(
                    source = "completeClaimBuildTask",
                    projectId = buildTask.projectId,
                    pipelineId = buildTask.pipelineId,
                    userId = completeTask.userId,
                    buildId = buildTask.buildId,
                    stageId = buildTask.stageId,
                    containerId = buildTask.containerId,
                    containerHashId = buildTask.containerHashId,
                    containerType = buildTask.containerType,
                    actionType = if (endBuild) ActionType.END else ActionType.REFRESH
                )
            )
        }
        return buildTask
    }

    fun updateBuildNo(pipelineId: String, buildNo: Int) {
        pipelineBuildSummaryDao.updateBuildNo(dslContext, pipelineId, buildNo)
    }

    fun updateRecommendVersion(buildId: String, recommendVersion: String) {
        pipelineBuildDao.updateRecommendVersion(dslContext, buildId, recommendVersion)
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
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "buildStart",
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                userId = latestRunningBuild.userId,
                buildId = latestRunningBuild.buildId,
                // 刷新历史列表、详情、状态页面
                refreshTypes = RefreshType.HISTORY.binary or RefreshType.DETAIL.binary or RefreshType.STATUS.binary
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
        if (currentBuildStatus.isReadyToRun() || currentBuildStatus.isNeverRun()) {
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
                getExecuteTime(buildId)
            } catch (ignored: Throwable) {
                logger.error("[$pipelineId]|getExecuteTime-$buildId exception:", ignored)
                0L
            }
            logger.info("[$pipelineId]|getExecuteTime-$buildId executeTime: $executeTime")

            val buildParameters = getBuildParametersFromStartup(buildId)

            val recommendVersion = try {
                getRecommendVersion(buildParameters)
            } catch (ignored: Throwable) {
                logger.error("[$pipelineId]|getRecommendVersion-$buildId exception:", ignored)
                null
            }
            logger.info("[$pipelineId]|getRecommendVersion-$buildId recommendVersion: $recommendVersion")
            val remark = buildVariableService.getVariable(buildId, PIPELINE_BUILD_REMARK)
            pipelineBuildDao.finishBuild(
                dslContext = dslContext,
                buildId = buildId,
                buildStatus = status,
                executeTime = executeTime,
                recommendVersion = recommendVersion,
                remark = remark,
                errorInfoList = errorInfoList
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "startBuild",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    // 刷新详情、状态页面
                    // #3400 在BuildEnd处会有HISTORY，历史列表此处不需要，减少负载
                    refreshTypes = RefreshType.DETAIL.binary or RefreshType.STATUS.binary
                )
            )
            logger.info("[$pipelineId]|finishLatestRunningBuild-$buildId|status=$status")
        }
    }

    fun getRecommendVersion(buildParameters: List<BuildParameters>): String? {
        val recommendVersionPrefix = getRecommendVersionPrefix(buildParameters) ?: return null
        val buildNo = if (!buildParameters.none { it.key == BUILD_NO || it.key == "BuildNo" }) {
            buildParameters.filter { it.key == BUILD_NO || it.key == "BuildNo" }[0].value.toString()
        } else return null
        return "$recommendVersionPrefix.$buildNo"
    }

    fun getRecommendVersionPrefix(buildParameters: List<BuildParameters>): String? {
        val majorVersion = if (!buildParameters.none { it.key == MAJORVERSION || it.key == "MajorVersion" }) {
            buildParameters.filter { it.key == MAJORVERSION || it.key == "MajorVersion" }[0].value.toString()
        } else return null

        val minorVersion = if (!buildParameters.none { it.key == MINORVERSION || it.key == "MinorVersion" }) {
            buildParameters.filter { it.key == MINORVERSION || it.key == "MinorVersion" }[0].value.toString()
        } else return null

        val fixVersion = if (!buildParameters.none { it.key == FIXVERSION || it.key == "FixVersion" }) {
            buildParameters.filter { it.key == FIXVERSION || it.key == "FixVersion" }[0].value.toString()
        } else return null

        return "$majorVersion.$minorVersion.$fixVersion"
    }

    fun getBuildParametersFromStartup(buildId: String): List<BuildParameters> {
        return try {
            val buildParameters = pipelineBuildDao.getBuildInfo(dslContext, buildId)?.buildParameters
            return if (buildParameters == null || buildParameters.isEmpty()) {
                emptyList()
            } else {
                (JsonUtil.getObjectMapper().readValue(buildParameters) as List<BuildParameters>)
                    .filter { !it.key.startsWith(SkipElementUtils.prefix) }
            }
        } catch (ignore: Exception) {
            emptyList()
        }
    }

    fun getExecuteTime(buildId: String): Long {
        val filter = setOf(
            EnvControlTaskType.VM.name,
            EnvControlTaskType.NORMAL.name,
            QualityGateInElement.classType,
            QualityGateOutElement.classType,
            ManualReviewUserTaskElement.classType
        )
        val executeTask = pipelineTaskService.listByBuildId(buildId)
            .filter { !filter.contains(it.taskType) }
        var executeTime = 0L
        val stageTotalTime = mutableMapOf<String, MutableMap<String, Long>>()
        executeTask.forEach { task ->
            val jobTime = stageTotalTime.computeIfAbsent(task.stageId) { mutableMapOf(task.containerId to 0L) }
            jobTime[task.containerId] = (jobTime[task.containerId] ?: 0L) + (task.totalTime ?: 0L)
        }
        stageTotalTime.forEach { job ->
            var maxJobTime = 0L
            job.value.forEach {
                if (maxJobTime < it.value) {
                    maxJobTime = it.value
                }
            }
            executeTime += maxJobTime
        }
        return executeTime
    }

    fun getLastTimeBuild(projectId: String, pipelineId: String): BuildInfo? {
        return pipelineBuildDao.convert(pipelineBuildDao.getLatestBuild(dslContext, projectId, pipelineId))
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
        buildNoEnd: Int?,
        buildMsg: String?
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
            buildNoEnd = buildNoEnd,
            buildMsg = buildMsg
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

    fun updateBuildInfoStatus2Queue(projectId: String, buildId: String, oldStatus: BuildStatus) {
        pipelineBuildDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            oldBuildStatus = oldStatus,
            newBuildStatus = BuildStatus.QUEUE
        )
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

    private fun addTraceVar(projectId: String, pipelineId: String, buildId: String) {
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrEmpty()) {
            buildVariableService.batchSetVariable(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = listOf(BuildParameters(TraceTag.TRACE_HEADER_DEVOPS_BIZID, MDC.get(TraceTag.BIZID)))
            )
        }
    }

    fun updateBuildHistoryStageState(buildId: String, allStageStatus: List<BuildStageStatus>) {
        pipelineBuildDao.updateBuildStageStatus(dslContext, buildId, stageStatus = allStageStatus)
    }
}
