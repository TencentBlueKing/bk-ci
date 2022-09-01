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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.constant.BUILD_QUEUE
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCancelBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
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
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_AUTH_USER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_DESC
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.context.StartBuildContext
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
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
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.BuildVariableService
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
import com.tencent.devops.process.utils.PIPELINE_BUILD_URL
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

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
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildDetailDao: BuildDetailDao,
    private val buildVariableService: BuildVariableService,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuleService: PipelineRuleService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
        private const val STATUS_STAGE = "stage-1"
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

    fun getBuildInfo(projectId: String, buildId: String): BuildInfo? {
        val t = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId)
        return pipelineBuildDao.convert(t)
    }

    fun getBuildInfo(projectId: String, pipelineId: String, buildId: String): BuildInfo? {
        val t = pipelineBuildDao.getBuildInfo(dslContext, projectId, pipelineId, buildId) ?: return null
        return pipelineBuildDao.convert(t)
    }

    fun getBuildInfoListByConcurrencyGroup(
        projectId: String,
        concurrencyGroup: String,
        status: List<BuildStatus>
    ): List<Pair<String, String>> {
        return pipelineBuildDao.getBuildTasksByConcurrencyGroup(
            dslContext = dslContext,
            projectId = projectId,
            concurrencyGroup = concurrencyGroup,
            statusSet = status
        ).map { Pair(it.value1(), it.value2()) }
    }

    fun getBuildNoByByPair(buildIds: Set<String>, projectId: String?): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val buildInfoList = pipelineBuildDao.listBuildInfoByBuildIds(
            dslContext = dslContext,
            buildIds = buildIds,
            projectId = projectId
        )
        buildInfoList.forEach {
            result[it.buildId] = it.buildNum.toString()
        }
        return result
    }

    fun getBuildSummaryRecord(projectId: String, pipelineId: String): TPipelineBuildSummaryRecord? {
        return pipelineBuildSummaryDao.get(dslContext, projectId, pipelineId)
    }

    fun getBuildPipelineRecords(
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
    ): Result<TPipelineInfoRecord> {
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

    fun getLatestBuild(projectId: String, pipelineIds: List<String>): Map<String, PipelineLatestBuild> {
        val records = pipelineBuildSummaryDao.listSummaryByPipelineIds(dslContext, pipelineIds, projectId)
        val ret = mutableMapOf<String, PipelineLatestBuild>()
        records.forEach {
            val status = it.latestStatus
            val pipelineId = it.pipelineId
            ret[pipelineId] = PipelineLatestBuild(
                buildId = it.latestBuildId ?: "",
                startUser = it.latestStartUser ?: "",
                startTime = DateTimeUtil.toDateTime(it.latestStartTime),
                endTime = DateTimeUtil.toDateTime(it.latestEndTime),
                status = if (status != null) BuildStatus.values()[status].name else null
            )
        }

        return ret
    }

    fun listPipelineBuildHistory(
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int,
        updateTimeDesc: Boolean? = null
    ): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = if (limit < 0) 1000 else limit,
            updateTimeDesc = updateTimeDesc
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
        buildMsg: String?,
        startUser: List<String>?,
        updateTimeDesc: Boolean? = null
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
            buildMsg = buildMsg,
            startUser = startUser,
            updateTimeDesc = updateTimeDesc
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
        val aliasNames = if (aliasList.isNullOrEmpty()) {
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
            val channelCode = ChannelCode.valueOf(channel)
            val startType = StartType.toStartType(trigger)
            BuildHistory(
                id = buildId,
                userId = triggerUser ?: startUser,
                trigger = StartType.toReadableString(trigger, channelCode),
                buildNum = buildNum,
                pipelineVersion = version,
                startTime = startTime?.timestampmilli() ?: 0L,
                endTime = endTime?.timestampmilli(),
                status = buildStatus[status].name,
                stageStatus = stageStatus?.let { self ->
                    JsonUtil.to(self, object : TypeReference<List<BuildStageStatus>>() {})
                },
                currentTimestamp = currentTimestamp,
                material =
                material?.let { self ->
                    JsonUtil.to(self, object : TypeReference<List<PipelineBuildMaterial>?>() {})
                        ?.sortedBy { it.aliasName }
                },
                queueTime = queueTime?.timestampmilli(),
                artifactList = artifactInfo?.let { self ->
                    JsonUtil.to(self, object : TypeReference<List<FileInfo>?>() {})
                },
                remark = remark,
                totalTime = startTime?.let { s -> endTime?.let { e -> Duration.between(s, e).toMillis() } ?: 0 } ?: 0,
                executeTime = executeTime ?: 0L,
                buildParameters = buildParameters?.let { self ->
                    JsonUtil.to(self, object : TypeReference<List<BuildParameters>?>() {})
                },
                webHookType = webhookType,
                webhookInfo = webhookInfo?.let { self -> JsonUtil.to(self, object : TypeReference<WebhookInfo?>() {}) },
                startType = getStartType(trigger, webhookType),
                recommendVersion = recommendVersion,
                retry = isRetry ?: false,
                errorInfoList = errorInfo?.let { self ->
                    // 特殊兼容修改数据类型前的老数据，必须保留try catch
                    try {
                        JsonUtil.to(self, object : TypeReference<List<ErrorInfo>?>() {})
                    } catch (ignore: Throwable) {
                        null
                    }
                },
                buildMsg = BuildMsgUtils.getBuildMsg(buildMsg, startType = startType, channelCode = channelCode),
                buildNumAlias = buildNumAlias,
                updateTime = updateTime?.timestampmilli() ?: endTime?.timestampmilli() ?: 0L, // 防止空异常
                concurrencyGroup = concurrencyGroup
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
        val records = pipelineBuildDao.listBuildInfoByBuildIds(dslContext = dslContext, buildIds = buildIds)
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

    fun getBuildHistoryById(projectId: String, buildId: String): BuildHistory? {
        val record = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId) ?: return null
        val values = BuildStatus.values()
        val currentTimestamp = System.currentTimeMillis()
        return genBuildHistory(record, values, currentTimestamp)
    }

    fun getBuildHistoryByIds(
        buildIds: Set<String>,
        startBeginTime: String? = null,
        endBeginTime: String? = null,
        projectId: String? = null
    ): List<BuildHistory> {
        val records = pipelineBuildDao.listBuildInfoByBuildIds(
            dslContext = dslContext,
            buildIds = buildIds,
            startBeginTime = startBeginTime,
            endBeginTime = endBeginTime,
            projectId = projectId
        )
        val result = mutableListOf<BuildHistory>()
        if (records.isEmpty()) {
            return result
        }
        val values = BuildStatus.values()
        val currentTimestamp = System.currentTimeMillis()
        val historyBuildIds = mutableListOf<String>()
        records.forEach {
            val buildId = it.buildId
            if (historyBuildIds.contains(buildId)) {
                return@forEach
            }
            historyBuildIds.add(buildId)
            result.add(genBuildHistory(it, values, currentTimestamp))
        }
        return result
    }

    fun cancelBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        buildStatus: BuildStatus,
        terminateFlag: Boolean = false
    ): Boolean {
        logger.info("[$buildId]|SHUTDOWN_BUILD|userId=$userId|status=$buildStatus|terminateFlag=$terminateFlag")
        // 发送取消事件
        val actionType = if (terminateFlag) ActionType.TERMINATE else ActionType.END
        // 发送取消事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildCancelEvent(
                source = javaClass.simpleName,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus,
                actionType = actionType
            ),
            PipelineBuildCancelBroadCastEvent(
                source = "cancelBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = actionType
            )
        )
        // 给未结束的job发送心跳监控事件
        val statusSet = setOf(
            BuildStatus.QUEUE,
            BuildStatus.QUEUE_CACHE,
            BuildStatus.DEPENDENT_WAITING,
            BuildStatus.LOOP_WAITING,
            BuildStatus.PREPARE_ENV,
            BuildStatus.RUNNING
        )
        val containers = pipelineContainerService.listContainers(
            projectId = projectId,
            buildId = buildId,
            statusSet = statusSet
        )
        containers.forEach { container ->
            pipelineEventDispatcher.dispatch(
                PipelineContainerAgentHeartBeatEvent(
                    source = BS_CANCEL_BUILD_SOURCE,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    containerId = container.containerId,
                    executeCount = container.executeCount,
                    delayMills = 20000
                )
            )
        }
        return true
    }

    fun startBuild(
        pipelineInfo: PipelineInfo,
        fullModel: Model,
        originStartParams: MutableList<BuildParameters>,
        pipelineParamMap: MutableMap<String, BuildParameters>,
        setting: PipelineSetting?,
        buildNo: Int? = null,
        buildNumRule: String? = null,
        acquire: Boolean? = false
    ): String {
        val now = LocalDateTime.now()
        val startParamMap = pipelineParamMap.values.associate { it.key to it.value.toString() }
        val startBuildStatus: BuildStatus = BuildStatus.QUEUE // 默认都是排队状态
        // 2019-12-16 产品 rerun 需求
        val projectId = pipelineInfo.projectId
        val pipelineId = pipelineInfo.pipelineId
        val buildId = startParamMap[PIPELINE_RETRY_BUILD_ID] ?: buildIdGenerator.getNextId()
        val detailUrl = pipelineUrlBean.genBuildDetailUrl(
            projectId, pipelineId, buildId, null, null, false
        )
        val context = StartBuildContext.init(projectId, pipelineId, buildId, startParamMap)
        buildLogPrinter.startLog(buildId, null, null, context.executeCount)

        val defaultStageTagId by lazy { stageTagService.getDefaultStageTag().data?.id }

        val lastTimeBuildTaskRecords = pipelineTaskService.listByBuildId(projectId, buildId)
        val lastTimeBuildContainerRecords = pipelineContainerService.listByBuildId(projectId, buildId)
        val lastTimeBuildStages = pipelineStageService.listStages(projectId, buildId)

        val buildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId)

        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val buildContainers = mutableListOf<PipelineBuildContainer>()
        val buildStages = ArrayList<PipelineBuildStage>(fullModel.stages.size)

        val updateTaskExistsRecord: MutableList<PipelineBuildTask> = mutableListOf()
        val updateStageExistsRecord: MutableList<PipelineBuildStage> = ArrayList(fullModel.stages.size)
        val updateContainerExistsRecord: MutableList<PipelineBuildContainer> = mutableListOf()

        context.currentBuildNo = buildNo
//        var buildNoType: BuildNoType? = null
        // --- 第1层循环：Stage遍历处理 ---
        fullModel.stages.forEachIndexed nextStage@{ index, stage ->
            context.needUpdateStage = stage.finally // final stage 每次重试都会参与执行检查

            // #2318 如果是stage重试不是当前stage且当前stage已经是完成状态，或者该stage被禁用，则直接跳过
            if (context.needSkipWhenStageFailRetry(stage) || stage.stageControlOption?.enable == false) {
                logger.info("[$buildId|EXECUTE|#${stage.id!!}|${stage.status}|NOT_EXECUTE_STAGE")
                context.containerSeq += stage.containers.size // Job跳过计数也需要增加
                if (index == 0) {
                    stage.containers.forEach {
                        if (it is TriggerContainer) {
                            it.status = BuildStatus.RUNNING.name
                            ContainerUtils.setQueuingWaitName(it)
                        }
                    }
                }
                return@nextStage
            }

            DependOnUtils.initDependOn(stage = stage, params = startParamMap)
            // --- 第2层循环：Container遍历处理 ---
            stage.containers.forEach nextContainer@{ container ->
                if (container is TriggerContainer) { // 寻找触发点
                    pipelineContainerService.setUpTriggerContainer(container, context)
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
                    lastTimeBuildContainerRecords.isNotEmpty()
                ) {
                    if (null == pipelineContainerService.findTaskRecord(
                            lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                            container = container,
                            retryStartTaskId = context.retryStartTaskId
                        )
                    ) {

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
                    container.retryFreshMatrixOption()
                    pipelineContainerService.cleanContainersInMatrixGroup(
                        transactionContext = dslContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        matrixGroupId = container.id!!
                    )
                }
                // --- 第3层循环：Element遍历处理 ---
                pipelineContainerService.prepareBuildContainerTasks(
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
            var stageStatus = BuildStatus.QUEUE
            var stageStartTime: LocalDateTime? = null
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
                if (stage.tag == null) stage.tag = defaultStageTagId?.let { self -> listOf(self) }
            } else {
                stageStatus = BuildStatus.RUNNING // Stage-1 一开始就计算为启动
                stageStartTime = now
            }

            if (lastTimeBuildStages.isNotEmpty()) {
                if (context.needUpdateStage) {
                    stage.resetBuildOption(true)
                    run findHistoryStage@{
                        lastTimeBuildStages.forEach {
                            if (it.stageId == stage.id!!) {
                                it.status = stageStatus
                                it.startTime = stageStartTime
                                it.endTime = null
                                it.executeCount = context.executeCount
                                it.checkIn = stage.checkIn
                                it.checkOut = stage.checkOut
                                updateStageExistsRecord.add(it)
                                return@findHistoryStage
                            }
                        }
                    }
                }
            } else {
                stage.resetBuildOption(true)
                buildStages.add(
                    PipelineBuildStage(
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        stageId = stage.id!!,
                        seq = index,
                        status = stageStatus,
                        startTime = stageStartTime,
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
                pipelineParamMap[PIPELINE_BUILD_ID] = BuildParameters(PIPELINE_BUILD_ID, buildId, readOnly = true)
                pipelineParamMap[PIPELINE_BUILD_URL] = BuildParameters(PIPELINE_BUILD_URL, detailUrl, readOnly = true)
//                    .filter { it.valueType != BuildFormPropertyType.TEMPORARY }.toMutableList()
                val bizId = MDC.get(TraceTag.BIZID)
                if (!bizId.isNullOrBlank()) { // 保存链路信息
                    pipelineParamMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] = BuildParameters(
                        key = TraceTag.TRACE_HEADER_DEVOPS_BIZID, value = bizId
                    )
                }
                // 写入BuildNo
                context.currentBuildNo?.let { bn ->
                    if (
                        context.buildNoType != BuildNoType.SUCCESS_BUILD_INCREMENT &&
                        context.actionType == ActionType.START
                    ) {
                        val buildParameters = BuildParameters(key = BUILD_NO, value = bn, readOnly = true)
                        pipelineParamMap[BUILD_NO] = buildParameters
                        originStartParams.add(buildParameters)
                    }
                }
                pipelineParamMap[PIPELINE_START_TASK_ID] =
                    BuildParameters(PIPELINE_START_TASK_ID, context.firstTaskId, readOnly = true)

                if (buildHistoryRecord != null) {
                    if (context.actionType.isRetry() && context.retryStartTaskId.isNullOrBlank()) {
                        // 完整重试,重置启动时间
                        buildHistoryRecord.startTime = now
                    }
                    buildHistoryRecord.endTime = null
                    buildHistoryRecord.queueTime = now // for EPC
                    buildHistoryRecord.status = startBuildStatus.ordinal
                    // 重试时启动参数只需要刷新执行次数
                    buildHistoryRecord.buildParameters = buildHistoryRecord.buildParameters?.let { self ->
                        val retryCount = context.executeCount - 1
                        val list = JsonUtil.getObjectMapper().readValue(self) as MutableList<BuildParameters>
                        list.find { it.key == PIPELINE_RETRY_COUNT }?.let { param ->
                            param.value = retryCount
                        } ?: run {
                            list.add(
                                BuildParameters(
                                    key = PIPELINE_RETRY_COUNT,
                                    value = retryCount
                                )
                            )
                        }
                        JsonUtil.toJson(list)
                    }
                    transactionContext.batchStore(buildHistoryRecord).execute()
                    // 重置状态和人
                    buildDetailDao.update(
                        dslContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        buildId = buildId,
                        model = JsonUtil.toJson(fullModel, formatted = false),
                        buildStatus = startBuildStatus,
                        cancelUser = ""
                    )
                    val buildNum = buildHistoryRecord.buildNum
                    pipelineParamMap[PIPELINE_BUILD_NUM] = BuildParameters(
                        key = PIPELINE_BUILD_NUM, value = buildNum.toString(), readOnly = true
                    )
                } else { // 创建构建记录
                    val buildNumAlias = if (!buildNumRule.isNullOrBlank()) {
                        val parsedValue = pipelineRuleService.parsePipelineRule(
                            projectId = pipelineInfo.projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            busCode = PipelineRuleBusCodeEnum.BUILD_NUM.name,
                            ruleStr = buildNumRule
                        )
                        if (parsedValue.length > 256) parsedValue.substring(0, 256) else parsedValue
                    } else null
                    // 写自定义构建号信息
                    if (!buildNumAlias.isNullOrBlank()) {
                        pipelineParamMap[PIPELINE_BUILD_NUM_ALIAS] = BuildParameters(
                            key = PIPELINE_BUILD_NUM_ALIAS, value = buildNumAlias, readOnly = true
                        )
                    }
                    // 构建号递增
                    val buildNum = pipelineBuildSummaryDao.updateBuildNum(
                        dslContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineId,
                        buildNumAlias = buildNumAlias
                    )
                    pipelineParamMap[PIPELINE_BUILD_NUM] = BuildParameters(
                        key = PIPELINE_BUILD_NUM, value = buildNum.toString(), readOnly = true
                    )

                    // 优化并发组逻辑，只在GROUP_LOCK时才保存进history表
                    val concurrencyGroup = if (setting?.runLockType == PipelineRunLockType.GROUP_LOCK) {
                        setting.concurrencyGroup
                    } else null

                    pipelineBuildDao.create(
                        dslContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineInfo.pipelineId,
                        buildId = buildId,
                        version = startParamMap[PIPELINE_VERSION].toString().toInt(),
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
                        buildParameters = originStartParams,
                        webhookType = startParamMap[PIPELINE_WEBHOOK_TYPE],
                        webhookInfo = getWebhookInfo(startParamMap),
                        buildMsg = getBuildMsg(startParamMap[PIPELINE_BUILD_MSG]),
                        buildNumAlias = buildNumAlias,
                        concurrencyGroup = concurrencyGroup
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
                        buildStatus = startBuildStatus
                    )

                    // 设置流水线每日构建次数
                    pipelineSettingService.setCurrentDayBuildCount(
                        transactionContext = transactionContext,
                        projectId = pipelineInfo.projectId,
                        pipelineId = pipelineId
                    )
                }

                buildVariableService.batchSetVariable(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    variables = pipelineParamMap
                )

                if (updateTaskExistsRecord.isNotEmpty()) {
                    pipelineTaskService.batchUpdate(transactionContext, updateTaskExistsRecord)
                }
                if (buildTaskList.isNotEmpty()) {
                    pipelineTaskService.batchSave(transactionContext, buildTaskList)
                }

                if (updateContainerExistsRecord.isNotEmpty()) {
                    pipelineContainerService.batchUpdate(transactionContext, updateContainerExistsRecord)
                }
                if (buildContainers.isNotEmpty()) {
                    pipelineContainerService.batchSave(transactionContext, buildContainers)
                }

                if (updateStageExistsRecord.isNotEmpty()) {
                    pipelineStageService.batchUpdate(transactionContext, updateStageExistsRecord)
                }
                if (buildStages.isNotEmpty()) {
                    pipelineStageService.batchSave(transactionContext, buildStages)
                }
                // 排队计数+1
                pipelineBuildSummaryDao.updateQueueCount(
                    dslContext = transactionContext,
                    projectId = pipelineInfo.projectId,
                    pipelineId = pipelineInfo.pipelineId,
                    queueIncrement = 1
                )
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
                buildNoType = context.buildNoType
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
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE]?.toString(),
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL]?.toString(),
                webhookType = params[PIPELINE_WEBHOOK_TYPE]?.toString(),
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH]?.toString(),
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = if (params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name) {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]?.toString()
                } else {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE]?.toString()
                },
                webhookCommitId = params[PIPELINE_WEBHOOK_REVISION] as String?,
                webhookMergeCommitSha = params[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA]?.toString(),
                webhookSourceBranch = params[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH]?.toString(),
                mrId = params[BK_REPO_GIT_WEBHOOK_MR_ID]?.toString(),
                mrIid = params[BK_REPO_GIT_WEBHOOK_MR_NUMBER]?.toString(),
                mrUrl = params[BK_REPO_GIT_WEBHOOK_MR_URL]?.toString(),
                repoAuthUser = params[BK_REPO_WEBHOOK_REPO_AUTH_USER]?.toString()
            ),
            formatted = false
        )
    }

    private fun getBuildMsg(buildMsg: String?): String? {
        return buildMsg?.substring(0, buildMsg.length.coerceAtMost(255))
    }

    /**
     * 手动审批
     */
    fun manualDealReview(taskId: String, userId: String, params: ReviewParam) {
        // # 5108 消除了人工审核非必要的事务，防止在发送MQ挂住时，导致的长时间锁定
        pipelineTaskService.getByTaskId(projectId = params.projectId, buildId = params.buildId, taskId = taskId)
            ?.run {
                if (status.isRunning()) {
                    taskParams[BS_MANUAL_ACTION] = params.status.toString()
                    taskParams[BS_MANUAL_ACTION_USERID] = userId
                    params.desc?.let { self -> taskParams[BS_MANUAL_ACTION_DESC] = self }
                    params.suggest?.let { self -> taskParams[BS_MANUAL_ACTION_SUGGEST] = self }
                    if (params.params.isNotEmpty()) {
                        taskParams[BS_MANUAL_ACTION_PARAMS] = JsonUtil.toJson(params.params, formatted = false)
                        buildVariableService.batchUpdateVariable(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            variables = params.params.associate { it.key to it.value.toString() }
                        )
                    }

                    pipelineTaskService.updateTaskParam(
                        projectId = projectId,
                        buildId = buildId,
                        taskId = taskId,
                        taskParam = JsonUtil.toJson(taskParams, formatted = false)
                    )

                    pipelineEventDispatcher.dispatch(
                        PipelineBuildAtomTaskEvent(
                            source = "manualDealBuildTask",
                            projectId = projectId,
                            pipelineId = pipelineId,
                            userId = starter,
                            buildId = buildId,
                            stageId = stageId,
                            containerId = containerId,
                            containerHashId = containerHashId,
                            containerType = containerType,
                            taskId = taskId,
                            taskParam = taskParams,
                            actionType = ActionType.REFRESH,
                            executeCount = executeCount ?: 1
                        )
                    )
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
        val buildTask = pipelineTaskService.getBuildTask(
            projectId = completeTask.projectId,
            buildId = completeTask.buildId,
            taskId = completeTask.taskId
        )
        if (buildTask != null) {
            pipelineTaskService.updateTaskStatus(
                task = buildTask,
                userId = completeTask.userId,
                buildStatus = completeTask.buildStatus,
                errorType = completeTask.errorType,
                errorCode = completeTask.errorCode,
                errorMsg = completeTask.errorMsg,
                platformCode = completeTask.platformCode,
                platformErrorCode = completeTask.platformErrorCode
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

    fun updateBuildNo(projectId: String, pipelineId: String, buildNo: Int) {
        pipelineBuildSummaryDao.updateBuildNo(dslContext, projectId, pipelineId, buildNo)
    }

    fun updateRecommendVersion(projectId: String, buildId: String, recommendVersion: String) {
        pipelineBuildDao.updateRecommendVersion(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            recommendVersion = recommendVersion
        )
    }

    /**
     * 开始最新一次构建
     */
    fun startLatestRunningBuild(latestRunningBuild: LatestRunningBuild, retry: Boolean) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val startTime = LocalDateTime.now()
            buildDetailDao.updateStatus(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                buildId = latestRunningBuild.buildId,
                buildStatus = BuildStatus.RUNNING,
                startTime = startTime
            )
            pipelineBuildDao.startBuild(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                buildId = latestRunningBuild.buildId,
                startTime = startTime,
                retry = retry
            )
            pipelineInfoDao.updateLatestStartTime(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                startTime = startTime
            )
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
            pipelineBuildSummaryDao.updateQueueCount(
                dslContext = dslContext,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                queueIncrement = -1
            )
        } else {
            pipelineBuildSummaryDao.finishLatestRunningBuild(
                dslContext = dslContext,
                latestRunningBuild = latestRunningBuild,
                isStageFinish = currentBuildStatus.name == BuildStatus.STAGE_SUCCESS.name
            )
        }
        with(latestRunningBuild) {
            val executeTime = try {
                getExecuteTime(latestRunningBuild.projectId, buildId)
            } catch (ignored: Throwable) {
                logger.error("[$pipelineId]|getExecuteTime-$buildId exception:", ignored)
                0L
            }
            logger.info("[$pipelineId]|getExecuteTime-$buildId executeTime: $executeTime")

            val buildParameters = getBuildParametersFromStartup(projectId, buildId)

            val recommendVersion = try {
                getRecommendVersion(buildParameters)
            } catch (ignored: Throwable) {
                logger.error("[$pipelineId]|getRecommendVersion-$buildId exception:", ignored)
                null
            }
            logger.info("[$pipelineId]|getRecommendVersion-$buildId recommendVersion: $recommendVersion")
            val remark = buildVariableService.getVariable(projectId, pipelineId, buildId, PIPELINE_BUILD_REMARK)
            pipelineBuildDao.finishBuild(
                dslContext = dslContext,
                projectId = projectId,
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

    fun getBuildParametersFromStartup(projectId: String, buildId: String): List<BuildParameters> {
        return try {
            val buildParameters = pipelineBuildDao.getBuildParameters(dslContext, projectId, buildId)
            return if (buildParameters.isNullOrEmpty()) {
                emptyList()
            } else {
                (JsonUtil.getObjectMapper().readValue(buildParameters) as List<BuildParameters>)
                    .filter { !it.key.startsWith(SkipElementUtils.prefix) }
            }
        } catch (ignore: Exception) {
            emptyList()
        }
    }

    fun getExecuteTime(projectId: String, buildId: String): Long {
        val filter = setOf(
            EnvControlTaskType.VM.name,
            EnvControlTaskType.NORMAL.name,
            QualityGateInElement.classType,
            QualityGateOutElement.classType,
            ManualReviewUserTaskElement.classType
        )
        val executeTask = pipelineTaskService.listByBuildId(projectId, buildId)
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
        buildMsg: String?,
        startUser: List<String>?
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
            buildMsg = buildMsg,
            startUser = startUser
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
        pipelineBuildDao.updateBuildStageStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageStatus = listOf(
                BuildStageStatus(
                    stageId = STATUS_STAGE,
                    name = STATUS_STAGE,
                    status = MessageCodeUtil.getCodeLanMessage(BUILD_QUEUE)
                )
            ),
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

    fun updateBuildHistoryStageState(projectId: String, buildId: String, allStageStatus: List<BuildStageStatus>) {
        pipelineBuildDao.updateBuildStageStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageStatus = allStageStatus
        )
    }
}
