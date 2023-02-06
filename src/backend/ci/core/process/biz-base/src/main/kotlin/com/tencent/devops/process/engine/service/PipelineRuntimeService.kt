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
import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCancelBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
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
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ISSUE_IID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_NUMBER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_NOTE_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_REVIEW_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_TAG_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_ALIAS_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_AUTH_USER
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_TYPE
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
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_DESC
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.context.StartBuildContext
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.control.lock.PipelineVersionLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
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
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.code.WebhookInfo
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
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
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
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
import java.util.Date

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
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildDetailDao: BuildDetailDao,
    private val recordModelDao: BuildRecordModelDao,
    private val buildVariableService: BuildVariableService,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineRuleService: PipelineRuleService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
        private const val TRIGGER_STAGE = "stage-1"
        private const val TAG = "startVM-0"
        private const val JOB_ID = "0"
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

    /** 根据状态信息获取并发组构建列表
     * @return Pair( PIPELINE_ID , BUILD_ID )
     */
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

    fun getBuildInfoListByConcurrencyGroupNull(
        projectId: String,
        pipelineId: String,
        status: List<BuildStatus>
    ): List<Pair<String, String>> {
        return pipelineBuildDao.getBuildTasksByConcurrencyGroupNull(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
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
            pageSize = pageSize,
            userId = null
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
                retry = executeCount?.let { it > 1 } == true,
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
                concurrencyGroup = concurrencyGroup,
                executeCount = executeCount
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
        // 记录该构建取消人信息
        pipelineBuildDetailService.updateBuildCancelUser(
            projectId = projectId,
            buildId = buildId,
            cancelUserId = userId
        )
        pipelineBuildRecordService.updateBuildCancelUser(
            projectId = projectId,
            buildId = buildId,
            cancelUserId = userId
        )
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
        // #7983 审核插件在被取消时作为审核结束处理，增加时间戳
        val tasks = pipelineTaskService.listContainerBuildTasks(
            projectId = projectId,
            buildId = buildId,
            containerSeqId = null,
            buildStatusSet = setOf(BuildStatus.REVIEWING)
        )
        tasks.forEach { task ->
            taskBuildRecordService.updateTaskRecord(
                projectId = task.projectId, pipelineId = pipelineId, buildId = buildId,
                taskId = task.taskId, executeCount = task.executeCount ?: 1, buildStatus = null,
                taskVar = mapOf(), timestamps = mapOf(
                    BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                        BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
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
        buildId: String,
        buildNo: Int? = null,
        buildNumRule: String? = null,
        acquire: Boolean? = false,
        triggerReviewers: List<String>? = null
    ): String {
        val now = LocalDateTime.now()
        val startParamMap = pipelineParamMap.values.associate { it.key to it.value.toString() }
        // 2019-12-16 产品 rerun 需求
        val projectId = pipelineInfo.projectId
        val pipelineId = pipelineInfo.pipelineId
        val version = pipelineInfo.version
        val startBuildStatus: BuildStatus = if (triggerReviewers.isNullOrEmpty()) {
            // 默认都是排队状态
            BuildStatus.QUEUE
        } else {
            BuildStatus.TRIGGER_REVIEWING
        }
        val detailUrl = pipelineUrlBean.genBuildDetailUrl(
            projectId, pipelineId, buildId, null, null, false
        )
        val context = StartBuildContext.init(projectId, pipelineId, buildId, startParamMap)
        buildLogPrinter.startLog(buildId, null, null, context.executeCount)

        val defaultStageTagId by lazy { stageTagService.getDefaultStageTag().data?.id }

        val lastTimeBuildTasks = pipelineTaskService.listByBuildId(projectId, buildId)
        val lastTimeBuildContainers = pipelineContainerService.listByBuildId(projectId, buildId)
        val lastTimeBuildStages = pipelineStageService.listStages(projectId, buildId)

        val buildHistoryRecord = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId)

        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val buildContainers = mutableListOf<PipelineBuildContainer>()
        val buildStages = ArrayList<PipelineBuildStage>(fullModel.stages.size)

        val stageBuildRecords = mutableListOf<BuildRecordStage>()
        val containerBuildRecords = mutableListOf<BuildRecordContainer>()
        val taskBuildRecords = mutableListOf<BuildRecordTask>()

        val updateExistsTask: MutableList<PipelineBuildTask> = mutableListOf()
        val updateExistsStage: MutableList<PipelineBuildStage> = ArrayList(fullModel.stages.size)
        val updateExistsContainer: MutableList<PipelineBuildContainer> = mutableListOf()

        context.currentBuildNo = buildNo
//        var buildNoType: BuildNoType? = null
        // --- 第1层循环：Stage遍历处理 ---
        var afterRetryStage = false
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
                            ContainerUtils.setQueuingWaitName(it, startBuildStatus)
                        }
                    }
                    stage.executeCount?.let { count -> stage.executeCount = count + 1 }
                }
                // record表需要记录被跳过的记录
                if (stage.stageControlOption?.enable == false) {
                    stageBuildRecords.add(
                        BuildRecordStage(
                            projectId = projectId, pipelineId = pipelineId, resourceVersion = version,
                            buildId = buildId, stageId = stage.id!!, executeCount = context.executeCount,
                            stageSeq = index, stageVar = mutableMapOf(), status = BuildStatus.SKIP.name,
                            timestamps = mapOf()
                        )
                    )
                    stage.containers.forEach { container ->
                        containerBuildRecords.add(
                            BuildRecordContainer(
                                projectId = projectId, pipelineId = pipelineId, resourceVersion = version,
                                buildId = buildId, stageId = stage.id!!, containerId = container.containerId!!,
                                containerType = container.getClassType(), executeCount = context.executeCount,
                                matrixGroupFlag = container.matrixGroupFlag, matrixGroupId = null,
                                status = BuildStatus.SKIP.name, timestamps = mapOf(),
                                containerVar = mutableMapOf(
                                    Container::containerHashId.name to  container.containerHashId!!
                                )
                            )
                        )
                        container.elements.forEachIndexed { index, element ->
                            taskBuildRecords.add(
                                BuildRecordTask(
                                    projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                                    stageId = stage.id!!, containerId = container.containerId!!,
                                    taskId = element.id!!, classType = element.getClassType(),
                                    atomCode = element.getTaskAtom(), executeCount = context.executeCount,
                                    originClassType = null, resourceVersion = version, taskSeq = index,
                                    status = BuildStatus.SKIP.name, timestamps = mapOf(), taskVar = mutableMapOf()
                                )
                            )
                        }
                    }
                }
                return@nextStage
            }

            DependOnUtils.initDependOn(stage = stage, params = startParamMap)
            // --- 第2层循环：Container遍历处理 ---
            stage.containers.forEach nextContainer@{ container ->
                if (container is TriggerContainer) { // 寻找触发点
                    pipelineContainerService.setUpTriggerContainer(
                        version, stage, container, context, startBuildStatus,
                        stageBuildRecords, containerBuildRecords, taskBuildRecords
                    )
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
                    lastTimeBuildContainers.isNotEmpty()
                ) {
                    if (null == pipelineContainerService.findLastTimeBuildTask(
                            lastTimeBuildTasks = lastTimeBuildTasks,
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
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        matrixGroupId = container.id!!
                    )
                }
                // --- 第3层循环：Element遍历处理 ---
                pipelineContainerService.prepareBuildContainerTasks(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    container = container,
                    startParamMap = startParamMap,
                    context = context,
                    stage = stage,
                    buildContainers = buildContainers,
                    buildTaskList = buildTaskList,
                    updateExistsContainer = updateExistsContainer,
                    updateExistsTask = updateExistsTask,
                    lastTimeBuildTasks = lastTimeBuildTasks,
                    lastTimeBuildContainers = lastTimeBuildContainers
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
                    afterRetryStage = true
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
                                updateExistsStage.add(it)
                                return@findHistoryStage
                            }
                        }
                    }
                }
                if (afterRetryStage) {
                    stage.executeCount?.let { count -> stage.executeCount = count + 1 }
                }
            } else {
                stage.resetBuildOption(true)
                buildStages.add(
                    PipelineBuildStage(
                        projectId = projectId,
                        pipelineId = pipelineId,
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

                val buildNum: Int
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
                        projectId = projectId,
                        buildId = buildId,
                        model = JsonUtil.toJson(fullModel, formatted = false),
                        buildStatus = startBuildStatus,
                        cancelUser = ""
                    )
                    buildNum = buildHistoryRecord.buildNum
                    pipelineParamMap[PIPELINE_BUILD_NUM] = BuildParameters(
                        key = PIPELINE_BUILD_NUM, value = buildNum.toString(), readOnly = true
                    )
                } else { // 创建构建记录
                    val buildNumAlias = if (!buildNumRule.isNullOrBlank()) {
                        val parsedValue = pipelineRuleService.parsePipelineRule(
                            projectId = projectId,
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
                    buildNum = pipelineBuildSummaryDao.updateBuildNum(
                        dslContext = transactionContext,
                        projectId = projectId,
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
                    val pipelineVersionLock = PipelineVersionLock(redisOperation, pipelineId, version)
                    try {
                        pipelineVersionLock.lock()
                        pipelineBuildDao.create(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
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
                        // 查询流水线版本记录
                        val pipelineVersionInfo = pipelineResVersionDao.getPipelineVersionSimple(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = version
                        )
                        val referFlag = pipelineVersionInfo?.referFlag ?: true
                        var referCount = pipelineVersionInfo?.referCount
                        referCount = if (referCount == null) {
                            // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
                            pipelineBuildDao.countBuildNumByVersion(
                                dslContext = transactionContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                version = version
                            )
                        } else {
                            referCount + 1
                        }
                        // 更新流水线版本关联构建记录信息
                        pipelineResVersionDao.updatePipelineVersionReferInfo(
                            dslContext = transactionContext,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = version,
                            referCount = referCount,
                            referFlag = referFlag
                        )
                    } finally {
                        pipelineVersionLock.unlock()
                    }

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

                saveBuildRuntimeRecord(
                    transactionContext = transactionContext,
                    context = context,
                    startBuildStatus = startBuildStatus,
                    buildNum = buildNum,
                    resourceVersion = version,
                    updateExistsStage = updateExistsStage,
                    updateExistsContainer = updateExistsContainer,
                    updateExistsTask = updateExistsTask,
                    buildStages = buildStages,
                    buildContainers = buildContainers,
                    buildTaskList = buildTaskList,
                    stageBuildRecords = stageBuildRecords,
                    containerBuildRecords = containerBuildRecords,
                    taskBuildRecords = taskBuildRecords
                )
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
        // 如果不需要触发审核则直接开始发送开始事件
        if (startBuildStatus.isReadyToRun()) {
            sendBuildStartEvent(
                buildId = buildId,
                pipelineId = pipelineInfo.pipelineId,
                projectId = pipelineInfo.projectId,
                context = context,
                startBuildStatus = startBuildStatus
            )
        } else if (triggerReviewers?.isNotEmpty() == true) {
            prepareTriggerReview(
                userId = startParamMap[PIPELINE_START_USER_ID] ?: pipelineInfo.lastModifyUser,
                triggerUser = startParamMap[PIPELINE_START_USER_NAME] ?: pipelineInfo.lastModifyUser,
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                triggerReviewers = triggerReviewers,
                pipelineName = pipelineParamMap[PIPELINE_NAME]?.value?.toString() ?: pipelineId,
                buildNum = pipelineParamMap[PIPELINE_BUILD_NUM]?.value?.toString() ?: "1"
            )
            buildLogPrinter.addYellowLine(
                buildId = buildId, message = "Waiting for the review of $triggerReviewers",
                tag = TAG, jobId = JOB_ID, executeCount = 1
            )
        }
        return buildId
    }

    private fun saveBuildRuntimeRecord(
        transactionContext: DSLContext,
        context: StartBuildContext,
        startBuildStatus: BuildStatus,
        buildNum: Int,
        resourceVersion: Int,
        updateExistsStage: MutableList<PipelineBuildStage>,
        updateExistsContainer: MutableList<PipelineBuildContainer>,
        updateExistsTask: MutableList<PipelineBuildTask>,
        buildStages: ArrayList<PipelineBuildStage>,
        buildContainers: MutableList<PipelineBuildContainer>,
        buildTaskList: MutableList<PipelineBuildTask>,
        stageBuildRecords: MutableList<BuildRecordStage>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        taskBuildRecords: MutableList<BuildRecordTask>
    ) {
        val modelRecord = BuildRecordModel(
            resourceVersion = resourceVersion, startUser = context.triggerUser,
            startType = context.startType.name, buildNum = buildNum,
            projectId = context.projectId, pipelineId = context.pipelineId,
            buildId = context.buildId, executeCount = context.executeCount,
            cancelUser = null, modelVar = mutableMapOf(),
            status = startBuildStatus.name, timestamps = mapOf(),
            queueTime = LocalDateTime.now().timestampmilli(), startTime = null, endTime = null
        )

        if (updateExistsTask.isNotEmpty()) {
            pipelineTaskService.batchUpdate(transactionContext, updateExistsTask)
            saveTaskRecords(updateExistsTask, taskBuildRecords, resourceVersion)
        }
        if (buildTaskList.isNotEmpty()) {
            pipelineTaskService.batchSave(transactionContext, buildTaskList)
            saveTaskRecords(buildTaskList, taskBuildRecords, resourceVersion)
        }
        if (updateExistsContainer.isNotEmpty()) {
            pipelineContainerService.batchUpdate(transactionContext, updateExistsContainer)
            saveContainerRecords(updateExistsContainer, containerBuildRecords, resourceVersion)
        }
        if (buildContainers.isNotEmpty()) {
            pipelineContainerService.batchSave(transactionContext, buildContainers)
            saveContainerRecords(buildContainers, containerBuildRecords, resourceVersion)
        }

        if (updateExistsStage.isNotEmpty()) {
            pipelineStageService.batchUpdate(transactionContext, updateExistsStage)
            saveStageRecords(updateExistsStage, stageBuildRecords, resourceVersion)
        }
        if (buildStages.isNotEmpty()) {
            pipelineStageService.batchSave(transactionContext, buildStages)
            saveStageRecords(buildStages, stageBuildRecords, resourceVersion)
        }
        pipelineBuildRecordService.batchSave(
            dslContext, modelRecord, stageBuildRecords,
            containerBuildRecords, taskBuildRecords
        )
    }

    private fun saveTaskRecords(
        buildTaskList: MutableList<PipelineBuildTask>,
        taskBuildRecords: MutableList<BuildRecordTask>,
        resourceVersion: Int
    ) {
        buildTaskList.forEach {
            // 自动填充的构建机控制插件不需要存入Record
            if (it.taskType == EnvControlTaskType.VM.name) return@forEach
            taskBuildRecords.add(
                BuildRecordTask(
                    projectId = it.projectId, pipelineId = it.pipelineId, buildId = it.buildId,
                    stageId = it.stageId, containerId = it.containerId, taskSeq = it.taskSeq,
                    taskId = it.taskId, classType = it.taskType, atomCode = it.atomCode ?: it.taskAtom,
                    executeCount = it.executeCount ?: 1, originClassType = null,
                    resourceVersion = resourceVersion, status = null,
                    timestamps = mapOf(), taskVar = mutableMapOf()
                )
            )
        }
    }

    private fun saveContainerRecords(
        buildContainers: MutableList<PipelineBuildContainer>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        resourceVersion: Int
    ) {
        buildContainers.forEach {
            val containerVar = mutableMapOf<String, Any>()
            it.containerHashId?.let { hashId ->
                containerVar[Container::containerHashId.name] = hashId
            }
            containerBuildRecords.add(
                BuildRecordContainer(
                    projectId = it.projectId, pipelineId = it.pipelineId, resourceVersion = resourceVersion,
                    buildId = it.buildId, stageId = it.stageId, containerId = it.containerId,
                    containerType = it.containerType, executeCount = it.executeCount,
                    matrixGroupFlag = it.matrixGroupFlag, matrixGroupId = it.matrixGroupId,
                    status = null, timestamps = mapOf(),
                    containerVar = containerVar
                )
            )
        }
    }

    private fun saveStageRecords(
        updateStageExistsRecord: MutableList<PipelineBuildStage>,
        stageBuildRecords: MutableList<BuildRecordStage>,
        resourceVersion: Int
    ) {
        updateStageExistsRecord.forEach {
            stageBuildRecords.add(
                BuildRecordStage(
                    projectId = it.projectId, pipelineId = it.pipelineId, resourceVersion = resourceVersion,
                    buildId = it.buildId, stageId = it.stageId, stageSeq = it.seq,
                    executeCount = it.executeCount, stageVar = mutableMapOf(),
                    status = null, timestamps = mapOf()
                )
            )
        }
    }

    fun approveTriggerReview(
        userId: String,
        buildId: String,
        pipelineId: String,
        projectId: String,
        executeCount: Int
    ) {
        val newBuildStatus = BuildStatus.QUEUE
        logger.info("[$buildId|APPROVE_BUILD|userId($userId)|newBuildStatus=$newBuildStatus")
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val now = LocalDateTime.now()
            pipelineBuildDao.updateStatus(
                dslContext = transactionContext,
                projectId = projectId,
                buildId = buildId,
                oldBuildStatus = BuildStatus.TRIGGER_REVIEWING,
                newBuildStatus = newBuildStatus,
                startTime = now
            )
            recordModelDao.updateStatus(
                dslContext = transactionContext,
                projectId = projectId,
                buildId = buildId,
                buildStatus = newBuildStatus,
                executeCount = executeCount
            )
            buildDetailDao.updateStatus(
                dslContext = transactionContext,
                projectId = projectId,
                buildId = buildId,
                buildStatus = newBuildStatus,
                startTime = now
            )
            val variables = buildVariableService.getAllVariable(projectId, projectId, buildId)
            buildLogPrinter.addYellowLine(
                buildId = buildId, message = "Approved by user($userId)",
                tag = TAG, jobId = JOB_ID, executeCount = 1
            )
            sendBuildStartEvent(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = projectId,
                context = StartBuildContext.init(projectId, pipelineId, buildId, variables),
                startBuildStatus = newBuildStatus
            )
        }
    }

    fun disapproveTriggerReview(
        userId: String,
        buildId: String,
        pipelineId: String,
        projectId: String,
        executeCount: Int
    ) {
        val newBuildStatus = BuildStatus.FAILED
        logger.info("[$buildId|DISAPPROVE_BUILD|userId($userId)|pipelineId=$pipelineId")
        val (_, allStageStatus) = pipelineBuildDetailService.buildEnd(
            projectId = projectId,
            buildId = buildId,
            buildStatus = newBuildStatus,
            errorMsg = "Rejected by $userId"
        )

        pipelineBuildRecordService.buildEnd(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            buildStatus = newBuildStatus,
            errorMsg = "Rejected by $userId"
        )
        pipelineBuildDao.updateBuildStageStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageStatus = allStageStatus,
            oldBuildStatus = BuildStatus.TRIGGER_REVIEWING,
            newBuildStatus = newBuildStatus,
            errorInfoList = listOf(
                ErrorInfo(
                    stageId = "", containerId = "",
                    taskId = "", taskName = "", atomCode = "",
                    errorType = ErrorType.USER.num, errorMsg = "Rejected by $userId in trigger review.",
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_REVIEW_ABORT.toInt()
                )
            )
        )
        buildLogPrinter.addYellowLine(
            buildId = buildId, message = "Disapproved by user($userId)",
            tag = TAG, jobId = JOB_ID, executeCount = 1
        )
    }

    fun checkTriggerReviewer(
        userId: String,
        buildId: String,
        pipelineId: String,
        projectId: String
    ) = pipelineTriggerReviewDao.getTriggerReviewers(dslContext, projectId, pipelineId, buildId)
        ?.contains(userId) == true

    private fun sendBuildStartEvent(
        buildId: String,
        pipelineId: String,
        projectId: String,
        context: StartBuildContext,
        startBuildStatus: BuildStatus
    ) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = context.userId,
                buildId = buildId,
                taskId = context.firstTaskId,
                status = startBuildStatus,
                actionType = context.actionType,
                buildNoType = context.buildNoType
            ), // 监控事件
            PipelineBuildMonitorEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = context.userId,
                buildId = buildId,
                buildStatus = startBuildStatus,
                executeCount = context.executeCount
            ), // #3400 点启动处于DETAIL界面，以操作人视角，没有刷历史列表的必要，在buildStart真正启动时也会有HISTORY，减少负载
            PipelineBuildWebSocketPushEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = context.userId,
                buildId = buildId,
                // 刷新历史列表和详情页面
                refreshTypes = RefreshType.DETAIL.binary
            ), // 广播构建排队事件
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = context.userId,
                buildId = buildId,
                actionType = context.actionType,
                triggerType = context.startType.name
            )
        )
    }

    private fun prepareTriggerReview(
        userId: String,
        triggerUser: String,
        buildId: String,
        pipelineId: String,
        projectId: String,
        pipelineName: String,
        buildNum: String,
        triggerReviewers: List<String>
    ) {
        // #7565 如果是需要启动审核的则进入审核状态
        pipelineTriggerReviewDao.createReviewRecord(
            dslContext = dslContext,
            buildId = buildId,
            pipelineId = pipelineId,
            projectId = projectId,
            reviewers = triggerReviewers
        )
        pipelineEventDispatcher.dispatch(
            PipelineBuildReviewBroadCastEvent(
                source = "build waiting for REVIEW",
                projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, userId = userId,
                reviewType = BuildReviewType.TRIGGER_REVIEW,
                status = BuildStatus.REVIEWING.name,
                stageId = null, taskId = null
            ),
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_TRIGGER_REVIEW_NOTIFY_TEMPLATE.name,
                source = "build waiting for REVIEW",
                projectId = projectId, pipelineId = pipelineId,
                userId = userId, buildId = buildId,
                receivers = if (triggerReviewers.contains(triggerUser)) {
                    triggerReviewers
                } else {
                    triggerReviewers.plus(triggerUser)
                },
                titleParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "buildNum" to buildNum
                ),
                bodyParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewers" to triggerReviewers.joinToString(),
                    "triggerUser" to triggerUser
                ),
                position = null,
                stageId = null
            )
        )
    }

    private fun getWebhookInfo(params: Map<String, Any>): String? {
        if (params[PIPELINE_START_TYPE] != StartType.WEB_HOOK.name) {
            return null
        }
        return JsonUtil.toJson(
            bean = WebhookInfo(
                codeType = params[BK_REPO_WEBHOOK_REPO_TYPE]?.toString(),
                nameWithNamespace = params[BK_REPO_WEBHOOK_REPO_NAME]?.toString(),
                webhookMessage = params[PIPELINE_WEBHOOK_COMMIT_MESSAGE]?.toString(),
                webhookRepoUrl = params[BK_REPO_WEBHOOK_REPO_URL]?.toString(),
                webhookType = params[PIPELINE_WEBHOOK_TYPE]?.toString(),
                webhookBranch = params[PIPELINE_WEBHOOK_BRANCH]?.toString(),
                webhookAliasName = params[BK_REPO_WEBHOOK_REPO_ALIAS_NAME]?.toString(),
                // GIT事件分为MR和MR accept,但是PIPELINE_WEBHOOK_EVENT_TYPE值只有MR
                webhookEventType = if (params[PIPELINE_WEBHOOK_TYPE] == CodeType.GIT.name) {
                    params[BK_REPO_GIT_WEBHOOK_EVENT_TYPE]?.toString()
                } else {
                    params[PIPELINE_WEBHOOK_EVENT_TYPE]?.toString()
                },
                refId = params[PIPELINE_WEBHOOK_REVISION]?.toString(),
                webhookCommitId = params[PIPELINE_WEBHOOK_REVISION] as String?,
                webhookMergeCommitSha = params[BK_REPO_GIT_WEBHOOK_MR_MERGE_COMMIT_SHA]?.toString(),
                webhookSourceBranch = params[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH]?.toString(),
                mrId = params[BK_REPO_GIT_WEBHOOK_MR_ID]?.toString(),
                mrIid = params[BK_REPO_GIT_WEBHOOK_MR_NUMBER]?.toString(),
                mrUrl = params[BK_REPO_GIT_WEBHOOK_MR_URL]?.toString(),
                repoAuthUser = params[BK_REPO_WEBHOOK_REPO_AUTH_USER]?.toString(),
                tagName = params[BK_REPO_GIT_WEBHOOK_TAG_NAME]?.toString(),
                issueIid = params[BK_REPO_GIT_WEBHOOK_ISSUE_IID]?.toString(),
                noteId = params[BK_REPO_GIT_WEBHOOK_NOTE_ID]?.toString(),
                reviewId = params[BK_REPO_GIT_WEBHOOK_REVIEW_ID]?.toString()
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

                    taskBuildRecordService.updateTaskRecord(
                        projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                        taskId = taskId, executeCount = executeCount ?: 1, buildStatus = null,
                        taskVar = mapOf(
                            ManualReviewUserTaskElement::desc.name to (params.desc ?: ""),
                            ManualReviewUserTaskElement::suggest.name to (params.suggest ?: ""),
                            ManualReviewUserTaskElement::params.name to params.params
                        ),
                        timestamps = mapOf(
                            BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                                BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
                        )
                    )

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
    fun startLatestRunningBuild(latestRunningBuild: LatestRunningBuild, executeCount: Int) {
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
            recordModelDao.updateStatus(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                buildId = latestRunningBuild.buildId,
                executeCount = executeCount,
                buildStatus = BuildStatus.RUNNING
            )
            pipelineBuildDao.startBuild(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                buildId = latestRunningBuild.buildId,
                startTime = startTime,
                executeCount = executeCount
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

    fun getBuilds(
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?
    ): List<String> {
        return pipelineBuildDao.getBuilds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildStatus = buildStatus
        )
    }

    fun getPipelineBuildHistoryCount(
        projectId: String,
        pipelineId: String,
        materialAlias: List<String>? = null,
        materialUrl: String? = null,
        materialBranch: List<String>? = null,
        materialCommitId: String? = null,
        materialCommitMessage: String? = null,
        status: List<BuildStatus>?,
        trigger: List<StartType>? = null,
        queueTimeStartTime: Long? = null,
        queueTimeEndTime: Long? = null,
        startTimeStartTime: Long? = null,
        startTimeEndTime: Long? = null,
        endTimeStartTime: Long? = null,
        endTimeEndTime: Long? = null,
        totalTimeMin: Long? = null,
        totalTimeMax: Long? = null,
        remark: String? = null,
        buildNoStart: Int? = null,
        buildNoEnd: Int? = null,
        buildMsg: String? = null,
        startUser: List<String>? = null
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
                    stageId = TRIGGER_STAGE,
                    name = TRIGGER_STAGE,
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
