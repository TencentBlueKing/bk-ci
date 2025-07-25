/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.constant.BUILD_QUEUE
import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
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
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.ElementUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
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
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildNumAliasLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
import com.tencent.devops.process.engine.pojo.AgentReuseMutexTree
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.BuildRetryInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineFilterParam
import com.tencent.devops.process.engine.pojo.builds.CompleteTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.enums.HistorySearchType
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer.Companion.addRecords
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage.Companion.addRecords
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask.Companion.addRecords
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.DependOnUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM_ALIAS
import com.tencent.devops.process.utils.PIPELINE_BUILD_REMARK
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import java.time.LocalDateTime
import java.util.Date
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildDetailDao: BuildDetailDao,
    private val recordModelDao: BuildRecordModelDao,
    private val buildVariableService: BuildVariableService,
    private val pipelineSettingService: PipelineSettingService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineRuleService: PipelineRuleService,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val repositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineArtifactQualityService: PipelineArtifactQualityService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
        private const val TRIGGER_STAGE = "stage-1"
        private const val TAG = "startVM-0"
        private const val JOB_ID = "0"
        private const val BUILD_REMARK_MAX_LENGTH = 4096
    }

    fun cancelPendingTask(projectId: String, pipelineId: String, userId: String) {
        val runningBuilds = pipelineBuildDao.getBuildTasksByStatus(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
            statusSet = setOf(
                BuildStatus.RUNNING, BuildStatus.REVIEWING,
                BuildStatus.QUEUE, BuildStatus.PREPARE_ENV,
                BuildStatus.UNEXEC, BuildStatus.QUEUE_CACHE,
                BuildStatus.STAGE_SUCCESS
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
                        status = BuildStatus.TERMINATE,
                        executeCount = build.executeCount
                    )
                )
            }
        }
    }

    fun getBuildInfo(projectId: String, buildId: String, queryDslContext: DSLContext? = null): BuildInfo? {
        return pipelineBuildDao.getBuildInfo(queryDslContext ?: dslContext, projectId, buildId)
    }

    fun getBuildInfo(projectId: String, pipelineId: String, buildId: String): BuildInfo? {
        return pipelineBuildDao.getBuildInfo(dslContext, projectId, pipelineId, buildId)
    }

    fun getRunningBuildCount(
        projectId: String,
        pipelineId: String
    ): Int {
        return pipelineBuildDao.countAllBuildWithStatus(dslContext, projectId, pipelineId, setOf(BuildStatus.RUNNING))
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
        list.forEach {
            result.add(genBuildHistory(it, currentTimestamp))
        }
        return result
    }

    fun listPipelineBuildHistory(
        userId: String? = null,
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
        updateTimeDesc: Boolean? = null,
        queryDslContext: DSLContext? = null,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext = queryDslContext ?: dslContext,
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
            updateTimeDesc = updateTimeDesc,
            debug = debug,
            triggerAlias = triggerAlias,
            triggerBranch = triggerBranch,
            triggerUser = triggerUser
        )
        val result = mutableListOf<BuildHistory>()
        list.forEach { buildInfo ->
            val artifactQuality = pipelineArtifactQualityService.buildArtifactQuality(
                userId = userId,
                projectId = projectId,
                artifactQualityList = buildInfo.artifactQualityList
            )
            result.add(genBuildHistory(buildInfo, currentTimestamp, artifactQuality))
        }
        return result
    }

    fun updateBuildRemark(projectId: String, pipelineId: String, buildId: String, remark: String?) {
        if (!remark.isNullOrEmpty() && remark.length >= BUILD_REMARK_MAX_LENGTH) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_BUILD_REMARK_MAX_LENGTH,
                params = arrayOf(BUILD_REMARK_MAX_LENGTH.toString())
            )
        }
        pipelineBuildDao.updateBuildRemark(dslContext, projectId, pipelineId, buildId, remark)
    }

    fun getHistoryConditionRepo(
        projectId: String,
        pipelineId: String,
        debugVersion: Int?,
        search: String?,
        type: HistorySearchType? = HistorySearchType.MATERIAL
    ): List<String> {
        val aliasNames = when (type) {
            HistorySearchType.MATERIAL -> {
                val history = pipelineBuildDao.listHistorySearchOptions(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    debugVersion = debugVersion,
                    type = type
                )
                val materialObjList = mutableListOf<PipelineBuildMaterial>()
                history.forEach {
                    if (!it.material.isNullOrEmpty()) {
                        materialObjList.addAll(it.material!!)
                    }
                }
                materialObjList.filter { !it.aliasName.isNullOrBlank() }
                    .map { it.aliasName!! }
                    .distinct()
            }

            HistorySearchType.TRIGGER -> {
                val history = pipelineBuildDao.listHistorySearchOptions(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    debugVersion = debugVersion,
                    type = type
                )
                history.filter { it.webhookInfo != null && !it.webhookInfo!!.webhookAliasName.isNullOrBlank() }
                    .map { it.webhookInfo!!.webhookAliasName!! }
                    .distinct()
            }

            else -> emptyList()
        }
        return if (search.isNullOrBlank()) {
            aliasNames
        } else {
            aliasNames.filter { it.contains(search) }
        }
    }

    fun getHistoryConditionBranch(
        projectId: String,
        pipelineId: String,
        aliasList: List<String>?,
        debugVersion: Int? = null,
        search: String?,
        type: HistorySearchType? = HistorySearchType.MATERIAL
    ): List<String> {
        val branchNames = when (type) {
            HistorySearchType.MATERIAL -> {
                val history = pipelineBuildDao.listHistorySearchOptions(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    debugVersion = debugVersion,
                    type = type
                )
                val materialObjList = mutableListOf<PipelineBuildMaterial>()
                history.forEach {
                    if (!it.material.isNullOrEmpty()) {
                        materialObjList.addAll(it.material!!)
                    }
                }
                val aliasNames = if (!aliasList.isNullOrEmpty() && aliasList.first().isNotBlank()) {
                    aliasList
                } else {
                    materialObjList.map { it.aliasName }
                }

                val result = mutableListOf<String>()
                aliasNames.distinct().forEach { alias ->
                    val branchNames = materialObjList.filter {
                        it.aliasName == alias && !it.branchName.isNullOrBlank()
                    }.map { it.branchName!! }.distinct()
                    result.addAll(branchNames)
                }
                result.distinct()
            }

            HistorySearchType.TRIGGER -> {
                val history = pipelineBuildDao.listHistorySearchOptions(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    debugVersion = debugVersion,
                    type = type
                )
                val webhookInfoList = history.filter { it.webhookInfo != null }.map { it.webhookInfo!! }
                val aliasNames = if (!aliasList.isNullOrEmpty() && aliasList.first().isNotBlank()) {
                    aliasList
                } else {
                    webhookInfoList.map { it.webhookAliasName }
                }
                val result = mutableListOf<String>()
                aliasNames.distinct().forEach { alias ->
                    val branchNames = webhookInfoList.filter {
                        it.webhookAliasName == alias && !it.webhookBranch.isNullOrBlank()
                    }.map { it.webhookBranch!! }.distinct()
                    result.addAll(branchNames)
                }
                result.distinct()
            }

            else -> emptyList()
        }
        return if (search.isNullOrBlank()) {
            branchNames
        } else {
            branchNames.filter { it.contains(search) }
        }
    }

    private fun genBuildHistory(
        buildInfo: BuildInfo,
        currentTimestamp: Long,
        artifactQuality: Map<String, List<ArtifactQualityMetadataAnalytics>>? = null
    ): BuildHistory {
        return with(buildInfo) {
            val startType = StartType.toStartType(trigger)
            BuildHistory(
                id = buildId,
                userId = triggerUser ?: startUser,
                trigger = StartType.toReadableString(
                    trigger,
                    channelCode,
                    I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ),
                buildNum = buildNum,
                pipelineVersion = version,
                pipelineVersionName = versionName,
                startTime = startTime ?: 0L,
                endTime = endTime,
                status = status.name,
                stageStatus = stageStatus,
                currentTimestamp = currentTimestamp,
                material = material,
                queueTime = queueTime,
                artifactList = artifactList,
                artifactQuality = artifactQuality,
                remark = remark,
                totalTime = if (startTime != null && endTime != null) {
                    (endTime!! - startTime!!).takeIf { it > 0 }
                } else null,
                executeTime = executeTime,
                buildParameters = buildParameters,
                webHookType = webhookType,
                webhookInfo = webhookInfo,
                startType = StartType.transform(trigger, webhookType),
                recommendVersion = recommendVersion,
                retry = executeCount?.let { it > 1 } == true,
                errorInfoList = errorInfoList,
                buildMsg = BuildMsgUtils.getBuildMsg(buildMsg, startType = startType, channelCode = channelCode),
                buildNumAlias = buildNumAlias,
                updateTime = updateTime ?: endTime ?: 0L, // 防止空异常
                concurrencyGroup = concurrencyGroup,
                executeCount = executeCount
            )
        }
    }

    fun getBuildHistoryByBuildNum(
        projectId: String,
        pipelineId: String,
        buildNum: Int?,
        statusSet: Set<BuildStatus>?,
        debug: Boolean? = false
    ): BuildHistory? {
        val record = pipelineBuildDao.getBuildInfoByBuildNum(
            dslContext, projectId, pipelineId, buildNum, statusSet, debug ?: false
        )
        return if (record != null) {
            genBuildHistory(record, System.currentTimeMillis())
        } else {
            null
        }
    }

    fun getBuildBasicInfoByIds(buildIds: Set<String>): Map<String, BuildBasicInfo> {
        val records = pipelineBuildDao.listBuildInfoByBuildIdsOnly(dslContext = dslContext, buildIds = buildIds)
        val result = mutableMapOf<String, BuildBasicInfo>()
        if (records.isEmpty()) {
            return result
        }

        buildIds.forEach { buildId ->
            result[buildId] = BuildBasicInfo(buildId, "", "", 0, null)
        }
        records.forEach {
            with(it) {
                result[it.buildId] = BuildBasicInfo(buildId, projectId, pipelineId, version, status)
            }
        }
        return result
    }

    fun getBuildHistoryById(projectId: String, buildId: String): BuildHistory? {
        val record = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId) ?: return null
        val currentTimestamp = System.currentTimeMillis()
        return genBuildHistory(record, currentTimestamp)
    }

    fun getStartUser(projectId: String, buildId: String): String? {
        return pipelineBuildDao.getStartUser(dslContext, projectId, buildId)
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
        val currentTimestamp = System.currentTimeMillis()
        val historyBuildIds = mutableListOf<String>()
        records.forEach {
            val buildId = it.buildId
            if (historyBuildIds.contains(buildId)) {
                return@forEach
            }
            historyBuildIds.add(buildId)
            result.add(genBuildHistory(it, currentTimestamp))
        }
        return result
    }

    fun cancelBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        terminateFlag: Boolean = false
    ): Boolean {
        logger.info("[$buildId]|SHUTDOWN_BUILD|userId=$userId|status=$buildStatus|terminateFlag=$terminateFlag")
        // 记录该构建取消人信息
        pipelineBuildRecordService.updateBuildCancelUser(
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
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
                actionType = actionType,
                executeCount = executeCount
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

    fun startBuild(fullModel: Model, context: StartBuildContext): BuildId {
        buildLogPrinter.startLog(
            buildId = context.buildId,
            tag = null,
            containerHashId = null,
            executeCount = context.executeCount
        )

        val defaultStageTagId by lazy { stageTagService.getDefaultStageTag().data?.id }
        context.watcher.start("read_old_data")
        val lastTimeBuildTasks = pipelineTaskService.listByBuildId(context.projectId, context.buildId)
        val lastTimeBuildContainers = pipelineContainerService.listByBuildId(context.projectId, context.buildId)
        val lastTimeBuildStages = pipelineStageService.listStages(context.projectId, context.buildId)

        val buildInfo = pipelineBuildDao.getBuildInfo(dslContext, context.projectId, context.buildId)
        context.watcher.stop()
        // # 7983 由于container需要使用名称动态展示状态，Record需要特殊保存
        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val buildContainersWithDetail = mutableListOf<Pair<PipelineBuildContainer, Container>>()
        val buildStages = ArrayList<PipelineBuildStage>(fullModel.stages.size)

        val stageBuildRecords = mutableListOf<BuildRecordStage>()
        val containerBuildRecords = mutableListOf<BuildRecordContainer>()
        val taskBuildRecords = mutableListOf<BuildRecordTask>()

        val updateExistsTask: MutableList<PipelineBuildTask> = mutableListOf()
        val updateExistsStage: MutableList<PipelineBuildStage> = ArrayList(fullModel.stages.size)
        val updateExistsContainerWithDetail: MutableList<Pair<PipelineBuildContainer, Container>> = mutableListOf()

//        var buildNoType: BuildNoType? = null
        // --- 第1层循环：Stage遍历处理 ---
        var afterRetryStage = false
        // #10082 针对构建容器的第三方构建机组装复用互斥信息
        val agentReuseMutexTree = AgentReuseMutexTree(context.executeCount, mutableListOf())
        fullModel.stages.forEachIndexed nextStage@{ index, stage ->
            // 运行中重试,如果不是重试插件的stage，则不处理
            if (context.shouldSkipRefreshWhenRetryRunning(stage)) {
                logger.info("${context.buildId}|EXECUTE|#${stage.id!!}|${stage.status}|NOT_RUNNING_STAGE")
                context.containerSeq += stage.containers.size // Job跳过计数也需要增加
                return@nextStage
            }
            context.needUpdateStage = stage.finally // final stage 每次重试都会参与执行检查

            // #2318 如果是stage重试不是当前stage且当前stage已经是完成状态，或者该stage被禁用，则直接跳过
            if (context.needSkipWhenStageFailRetry(stage) || stage.stageControlOption?.enable == false) {
                logger.info("[${context.buildId}|EXECUTE|#${stage.id!!}|${stage.status}|NOT_EXECUTE_STAGE")
                context.containerSeq += stage.containers.size // Job跳过计数也需要增加
                if (index == 0) {
                    stage.containers.forEach {
                        if (it is TriggerContainer) {
                            it.status = BuildStatus.RUNNING.name
                            it.name = ContainerUtils.getQueuingWaitName(it.name, context.startBuildStatus)
                        }
                    }
                    stage.executeCount?.let { count -> stage.executeCount = count + 1 }
                }
                // record表需要记录被跳过的记录
                if (stage.stageControlOption?.enable == false) {
                    stageBuildRecords.addRecords(
                        stage = stage,
                        context = context,
                        stageIndex = index,
                        buildStatus = BuildStatus.SKIP,
                        containerBuildRecords = containerBuildRecords,
                        taskBuildRecords = taskBuildRecords
                    )
                }
                return@nextStage
            }

            DependOnUtils.initDependOn(stage = stage, params = context.variables)
            // --- 第2层循环：Container遍历处理 ---
            stage.containers.forEach nextContainer@{ container ->
                // 运行中重试,如果不是重试插件的container或者依赖重试插件的container,则不处理
                if (context.shouldSkipRefreshWhenRetryRunning(container)) {
                    logger.info(
                        "${context.buildId}|EXECUTE|#${container.id!!}|${container.status}|NOT_RUNNING_CONTAINER"
                    )
                    context.containerSeq++
                    return@nextContainer
                }
                if (container is TriggerContainer) { // 寻找触发点
                    pipelineContainerService.setUpTriggerContainer(
                        stage = stage,
                        container = container,
                        context = context,
                        stageBuildRecords = stageBuildRecords,
                        containerBuildRecords = containerBuildRecords,
                        taskBuildRecords = taskBuildRecords
                    )
                    context.containerSeq++
                    containerBuildRecords.addRecords(
                        stageId = stage.id!!,
                        stageEnableFlag = stage.stageEnabled(),
                        container = container,
                        context = context,
                        buildStatus = null,
                        taskBuildRecords = taskBuildRecords
                    )
                    // 清理options变量
                    container.params = PipelineUtils.cleanOptions(container.params)
                    return@nextContainer
                } else if (container is NormalContainer) {
                    if (!ContainerUtils.isNormalContainerEnable(container)) {
                        context.containerSeq++
                        containerBuildRecords.addRecords(
                            stageId = stage.id!!,
                            stageEnableFlag = stage.stageEnabled(),
                            container = container,
                            context = context,
                            buildStatus = BuildStatus.SKIP,
                            taskBuildRecords = taskBuildRecords
                        )
                        return@nextContainer
                    }
                } else if (container is VMBuildContainer) {
                    if (!ContainerUtils.isVMBuildContainerEnable(container)) {
                        context.containerSeq++
                        containerBuildRecords.addRecords(
                            stageId = stage.id!!,
                            stageEnableFlag = stage.stageEnabled(),
                            container = container,
                            context = context,
                            buildStatus = BuildStatus.SKIP,
                            taskBuildRecords = taskBuildRecords
                        )
                        return@nextContainer
                    }

                    // #10082 针对构建容器的第三方构建机组装复用互斥信息
                    agentReuseMutexTree.addNode(container, index, context.variables)
                }

                modelCheckPlugin.checkJobCondition(container, stage.finally, context.variables)
                modelCheckPlugin.checkMutexGroup(container, context.variables)

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

                        logger.info("[${context.buildId}|RETRY_SKIP_JOB|j(${container.id!!})|${container.name}")
                        context.containerSeq++
                        return@nextContainer
                    }
                }

                /*
                    #3138 整合重试Stage下所有失败Job的功能，并对finallyStage做了特殊处理：
                    finallyStage如果不是属于重试的Stage，则需要将所有状态重置，不允许跳过
                */
                if (context.isRetryFailedContainer(container = container, stage = stage)) {
                    logger.info("[${context.buildId}|RETRY_SKIP_SUC_JOB|j(${container.containerId})|${container.name}")
                    context.containerSeq++
                    return@nextContainer
                }

                // --- 第3层循环：Element遍历处理 ---
                /*
                    #4518 整合组装Task和刷新已有Container的逻辑
                */
                pipelineContainerService.prepareBuildContainerTasks(
                    container = container,
                    context = context,
                    stage = stage,
                    buildContainers = buildContainersWithDetail,
                    buildTaskList = buildTaskList,
                    updateExistsContainer = updateExistsContainerWithDetail,
                    updateExistsTask = updateExistsTask,
                    containerBuildRecords = containerBuildRecords,
                    taskBuildRecords = taskBuildRecords,
                    lastTimeBuildTasks = lastTimeBuildTasks,
                    lastTimeBuildContainers = lastTimeBuildContainers
                )
                // 运行中重试,stage不需要更新
                if (context.retryOnRunningBuild) {
                    context.needUpdateStage = false
                }
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
                stageStartTime = context.now
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
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        stageId = stage.id!!,
                        seq = index,
                        status = stageStatus,
                        startTime = stageStartTime,
                        controlOption = stageOption,
                        checkIn = stage.checkIn,
                        checkOut = stage.checkOut,
                        stageIdForUser = stage.stageIdForUser
                    )
                )
            }

            // #10082 针对构建容器的第三方构建机组装复用互斥信息
            agentReuseMutexTree.checkVirtualRootAndResetJobType()
        }

        // #10082 使用互斥树节点重新回写Control和Container
        agentReuseMutexTree.rewriteModel(context, buildContainersWithDetail, fullModel, buildTaskList)

        context.pipelineParamMap[PIPELINE_START_TASK_ID] =
            BuildParameters(PIPELINE_START_TASK_ID, context.firstTaskId, readOnly = true)

        val modelJson = JsonUtil.toJson(fullModel, formatted = false)

        val retryInfo = if (buildInfo != null) {
            context.buildNum = buildInfo.buildNum
            BuildRetryInfo(
                status = context.startBuildStatus,
                rebuild = context.retryStartTaskId.isNullOrBlank(),
                nowTime = context.now,
                executeCount = context.executeCount,
                buildParameters = buildInfo.buildParameters?.let { self ->
                    val newList = self.toMutableList()
                    val retryCount = context.executeCount - 1
                    newList.find { it.key == PIPELINE_RETRY_COUNT }?.let { param ->
                        param.value = retryCount
                    } ?: run {
                        newList.add(BuildParameters(key = PIPELINE_RETRY_COUNT, value = retryCount)) // 不加readOnly，历史原因
                    }
                    newList
                },
                concurrencyGroup = context.concurrencyGroup
            )
        } else {
            // 自定义构建号生成, 如果是自定义构建号会有锁，放到事务外面防止影响整体事务性能
            context.genBuildNumAlias()
            null
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            if (buildInfo != null) {
                // 运行时的重试不需要刷新重试信息
                if (!context.retryOnRunningBuild) {
                    pipelineBuildDao.updateBuildRetryInfo(
                        dslContext = transactionContext,
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        retryInfo = retryInfo!!
                    )
                    // 重置状态和人
                    buildDetailDao.update(
                        dslContext = transactionContext,
                        projectId = context.projectId,
                        buildId = context.buildId,
                        model = modelJson,
                        buildStatus = context.startBuildStatus,
                        cancelUser = ""
                    )
                }
            } else {
                context.watcher.start("updateBuildNum")
                // 构建号递增
                context.buildNum = pipelineBuildSummaryDao.updateBuildNum(
                    dslContext = transactionContext,
                    projectId = context.projectId,
                    pipelineId = context.pipelineId,
                    buildId = context.buildId,
                    buildNumAlias = context.buildNumAlias,
                    debug = context.debug
                )
                context.watcher.stop()
                // 创建构建记录
                pipelineBuildDao.create(dslContext = transactionContext, startBuildContext = context)

                // detail记录,未正式启动，先排队状态
                buildDetailDao.create(
                    dslContext = transactionContext,
                    projectId = context.projectId,
                    buildId = context.buildId,
                    startUser = context.userId,
                    startType = context.startType,
                    buildNum = context.buildNum,
                    model = modelJson,
                    buildStatus = context.startBuildStatus
                )
            }

            context.pipelineParamMap[PIPELINE_BUILD_NUM] = BuildParameters(
                key = PIPELINE_BUILD_NUM, value = context.buildNum.toString(), readOnly = true
            )
            if (buildInfo != null) {
                if (!context.retryOnRunningBuild) {
                    // 重试构建需要增加锁保护更新VAR表
                    context.watcher.start("startBuildBatchSetVariable")
                    buildVariableService.batchSetVariable(
                        dslContext = transactionContext,
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        variables = context.pipelineParamMap
                    )
                }
            } else {
                // 全新构建不需要锁保护更新VAR表
                context.watcher.start("startBuildBatchSaveWithoutThreadSafety")
                buildVariableService.startBuildBatchSaveWithoutThreadSafety(
                    dslContext = transactionContext,
                    projectId = context.projectId,
                    pipelineId = context.pipelineId,
                    buildId = context.buildId,
                    variables = context.pipelineParamMap
                )
            }
            context.watcher.start("saveBuildRuntimeRecord")
            saveBuildRuntimeRecord(
                transactionContext = transactionContext,
                context = context,
                updateExistsStage = updateExistsStage,
                updateExistsContainer = updateExistsContainerWithDetail,
                updateExistsTask = updateExistsTask,
                buildStages = buildStages,
                buildContainers = buildContainersWithDetail,
                buildTaskList = buildTaskList,
                stageBuildRecords = stageBuildRecords,
                containerBuildRecords = containerBuildRecords,
                taskBuildRecords = taskBuildRecords
            )
            if (context.debug) repositoryVersionService.saveDebugBuildInfo(
                transactionContext = transactionContext,
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                version = context.resourceVersion,
                buildId = context.buildId
            )
            context.watcher.stop()
        }

        when {
            context.retryOnRunningBuild -> {
                context.sendBuildStageEvent()
            }

            context.startBuildStatus.isReadyToRun() -> {
                context.sendBuildStartEvent()
            }

            context.triggerReviewers?.isNotEmpty() == true -> {
                prepareTriggerReview(
                    userId = context.userId,
                    triggerUser = context.triggerUser,
                    buildId = context.buildId,
                    pipelineId = context.pipelineId,
                    projectId = context.projectId,
                    triggerReviewers = context.triggerReviewers!!,
                    pipelineName = context.pipelineParamMap[PIPELINE_NAME]?.value?.toString() ?: context.pipelineId,
                    buildNum = context.buildNum.toString()
                )
                buildLogPrinter.addYellowLine(
                    buildId = context.buildId, message = "Waiting for the review of ${context.triggerReviewers}",
                    tag = TAG, containerHashId = JOB_ID, executeCount = 1,
                    jobId = null, stepId = TAG
                )
            }
        }

        LogUtils.printCostTimeWE(context.watcher, warnThreshold = 4000, errorThreshold = 8000)
        return BuildId(
            id = context.buildId,
            executeCount = context.executeCount,
            projectId = context.projectId,
            pipelineId = context.pipelineId,
            num = context.buildNum
        )
    }

    private fun StartBuildContext.genBuildNumAlias() {

        (if (!pipelineSetting?.buildNumRule.isNullOrBlank())
            PipelineBuildNumAliasLock(redisOperation = redisOperation, pipelineId = pipelineId)
        else null
            )?.use { pipelineBuildNumAliasLock ->
                watcher.start("genBuildNumAlias_lock")
                pipelineBuildNumAliasLock.lock()
                watcher.start("parsePipelineRule")
                buildNumAlias = pipelineRuleService.parsePipelineRule(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    busCode = PipelineRuleBusCodeEnum.BUILD_NUM.name,
                    ruleStr = pipelineSetting!!.buildNumRule!!
                )

                // 写自定义构建号信息
                if (!buildNumAlias.isNullOrBlank()) {
                    pipelineParamMap[PIPELINE_BUILD_NUM_ALIAS] =
                        BuildParameters(PIPELINE_BUILD_NUM_ALIAS, value = buildNumAlias!!, readOnly = true)
                }
                watcher.start("setCurrentDayBuildCount")
                // 设置流水线每日构建次数
                pipelineSettingService.setCurrentDayBuildCount(
                    transactionContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                )
                watcher.stop()
            }
    }

    private fun saveBuildRuntimeRecord(
        transactionContext: DSLContext,
        context: StartBuildContext,
        updateExistsStage: MutableList<PipelineBuildStage>,
        updateExistsContainer: MutableList<Pair<PipelineBuildContainer, Container>>,
        updateExistsTask: MutableList<PipelineBuildTask>,
        buildStages: ArrayList<PipelineBuildStage>,
        buildContainers: MutableList<Pair<PipelineBuildContainer, Container>>,
        buildTaskList: MutableList<PipelineBuildTask>,
        stageBuildRecords: MutableList<BuildRecordStage>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        taskBuildRecords: MutableList<BuildRecordTask>
    ) {
        val modelRecord = if (context.retryOnRunningBuild) {
            null
        } else {
            BuildRecordModel(
                resourceVersion = context.resourceVersion, startUser = context.triggerUser,
                startType = context.startType.name, buildNum = context.buildNum,
                projectId = context.projectId, pipelineId = context.pipelineId,
                buildId = context.buildId, executeCount = context.executeCount,
                modelVar = mutableMapOf(), status = context.startBuildStatus.name,
                timestamps = mapOf(
                    BuildTimestampType.BUILD_CONCURRENCY_QUEUE to
                        BuildRecordTimeStamp(context.now.timestampmilli(), null)
                ), queueTime = context.now
            )
        }
        // #8955 针对单独写入的插件记录可以覆盖根据build数据生成的记录
        val taskBuildRecordResult = mutableListOf<BuildRecordTask>()
        if (updateExistsTask.isNotEmpty()) {
            pipelineTaskService.batchUpdate(transactionContext, updateExistsTask)
            taskBuildRecordResult.addRecords(updateExistsTask, context.resourceVersion)
        }
        if (buildTaskList.isNotEmpty()) {
            pipelineTaskService.batchSave(transactionContext, buildTaskList)
            taskBuildRecordResult.addRecords(buildTaskList, context.resourceVersion)
        }
        taskBuildRecordResult.addAll(taskBuildRecords)
        if (updateExistsContainer.isNotEmpty()) {
            pipelineContainerService.batchUpdate(
                transactionContext, updateExistsContainer.map { it.first }
            )
            saveContainerRecords(updateExistsContainer, containerBuildRecords, context.resourceVersion)
        }
        if (buildContainers.isNotEmpty()) {
            pipelineContainerService.batchSave(
                transactionContext, buildContainers.map { it.first }
            )
            saveContainerRecords(buildContainers, containerBuildRecords, context.resourceVersion)
        }

        if (updateExistsStage.isNotEmpty()) {
            pipelineStageService.batchUpdate(transactionContext, updateExistsStage)
            saveStageRecords(updateExistsStage, stageBuildRecords, context.resourceVersion)
        }
        if (buildStages.isNotEmpty()) {
            pipelineStageService.batchSave(transactionContext, buildStages)
            saveStageRecords(buildStages, stageBuildRecords, context.resourceVersion)
        }
        pipelineBuildRecordService.batchSave(
            transactionContext, modelRecord, stageBuildRecords,
            containerBuildRecords, taskBuildRecordResult
        )
    }

    private fun saveContainerRecords(
        buildContainers: MutableList<Pair<PipelineBuildContainer, Container>>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        resourceVersion: Int
    ) {
        buildContainers.forEach { (build, detail) ->
            val containerVar = mutableMapOf<String, Any>()
            containerVar[Container::name.name] = detail.name
            containerVar[Container::startVMTaskSeq.name] = detail.startVMTaskSeq ?: 1
            build.containerHashId?.let { hashId ->
                containerVar[Container::containerHashId.name] = hashId
            }
            if (detail is VMBuildContainer) {
                detail.showBuildResource?.let {
                    containerVar[VMBuildContainer::showBuildResource.name] = it
                }
                detail.mutexGroup?.let {
                    containerVar[VMBuildContainer::mutexGroup.name] = it
                }
            } else if (detail is NormalContainer) {
                detail.mutexGroup?.let {
                    containerVar[VMBuildContainer::mutexGroup.name] = it
                }
            }
            if (detail.matrixGroupFlag == true) {
                containerVar[Container::elements.name] = detail.elements.map {
                    JsonUtil.toMutableMap(it)
                }
            }
            containerBuildRecords.add(
                BuildRecordContainer(
                    projectId = build.projectId, pipelineId = build.pipelineId,
                    resourceVersion = resourceVersion, buildId = build.buildId,
                    stageId = build.stageId, containerId = build.containerId,
                    containerType = build.containerType, executeCount = build.executeCount,
                    matrixGroupFlag = build.matrixGroupFlag, matrixGroupId = build.matrixGroupId,
                    status = null, startTime = build.startTime,
                    endTime = build.endTime, timestamps = mapOf(), containerVar = containerVar
                )
            )
        }
    }

    private fun saveStageRecords(
        updateStageExistsRecord: MutableList<PipelineBuildStage>,
        stageBuildRecords: MutableList<BuildRecordStage>,
        resourceVersion: Int
    ) {
        updateStageExistsRecord.forEach { build ->
            stageBuildRecords.add(
                BuildRecordStage(
                    projectId = build.projectId, pipelineId = build.pipelineId,
                    resourceVersion = resourceVersion, buildId = build.buildId,
                    stageId = build.stageId, stageSeq = build.seq,
                    executeCount = build.executeCount, stageVar = mutableMapOf(),
                    timestamps = mapOf(), startTime = build.startTime, endTime = build.endTime
                )
            )
        }
    }

    fun approveTriggerReview(userId: String, buildInfo: BuildInfo) {
        val newBuildStatus = BuildStatus.QUEUE

        logger.info("[${buildInfo.buildId}|APPROVE_BUILD|userId($userId)|newBuildStatus=$newBuildStatus")
        val now = LocalDateTime.now()
        val executeCount = buildInfo.executeCount ?: 1
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildDao.updateStatus(
                dslContext = transactionContext,
                projectId = buildInfo.projectId,
                buildId = buildInfo.buildId,
                oldBuildStatus = BuildStatus.TRIGGER_REVIEWING,
                newBuildStatus = newBuildStatus,
                startTime = now
            )
            recordModelDao.updateStatus(
                dslContext = transactionContext,
                projectId = buildInfo.projectId,
                buildId = buildInfo.buildId,
                buildStatus = newBuildStatus,
                executeCount = executeCount
            )
            buildDetailDao.updateStatus(
                dslContext = transactionContext,
                projectId = buildInfo.projectId,
                buildId = buildInfo.buildId,
                buildStatus = newBuildStatus,
                startTime = now
            )
            buildLogPrinter.addYellowLine(
                buildId = buildInfo.buildId, message = "Approved by user($userId)",
                tag = TAG, containerHashId = JOB_ID, executeCount = 1,
                jobId = null, stepId = TAG
            )
            StartBuildContext.init4SendBuildStartEvent(
                userId = userId,
                buildId = buildInfo.buildId,
                pipelineId = buildInfo.pipelineId,
                projectId = buildInfo.projectId,
                resourceVersion = buildInfo.version,
                versionName = buildInfo.versionName,
                executeCount = executeCount,
                firstTaskId = buildInfo.firstTaskId,
                actionType = ActionType.START,
                startBuildStatus = BuildStatus.QUEUE,
                startType = StartType.toStartType(buildInfo.trigger),
                debug = buildInfo.debug
            ).apply {
                buildNoType = null // 该字段是需要遍历Model获得，不过在审核阶段为null，目前不影响功能逻辑。
            }.sendBuildStartEvent()
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
        val (_, allStageStatus) = pipelineBuildRecordService.buildEnd(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            buildStatus = newBuildStatus,
            errorInfoList = null,
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
            tag = TAG, containerHashId = JOB_ID, executeCount = 1,
            jobId = null, stepId = TAG
        )
    }

    fun checkTriggerReviewer(
        userId: String,
        buildId: String,
        pipelineId: String,
        projectId: String
    ) = pipelineTriggerReviewDao.getTriggerReviewers(dslContext, projectId, pipelineId, buildId)
        ?.contains(userId) == true

    private fun StartBuildContext.sendBuildStartEvent() {
        // #8275 在发送运行或排队的开始事件时，进行排队计数+1
        pipelineBuildSummaryDao.updateQueueCount(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            queueIncrement = 1
        )
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                taskId = firstTaskId,
                status = startBuildStatus,
                actionType = actionType,
                executeCount = executeCount,
                buildNoType = buildNoType // 该字段是需要遍历Model‘获得，不过在审核阶段为null，不影响功能逻辑。
            ), // 监控事件
            PipelineBuildMonitorEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                executeCount = executeCount
            ), // #3400 点启动处于DETAIL界面，以操作人视角，没有刷历史列表的必要，在buildStart真正启动时也会有HISTORY，减少负载
            PipelineBuildWebSocketPushEvent(
                source = "startBuild",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                // 刷新历史列表和详情页面
                refreshTypes = RefreshType.DETAIL.binary or RefreshType.RECORD.binary
            ), // 广播构建排队事件
            PipelineBuildQueueBroadCastEvent(
                source = "startQueue",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = actionType,
                triggerType = startType.name
            )
        )
    }

    private fun StartBuildContext.sendBuildStageEvent() {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = "runningBuildRetry|$buildId|$retryStartTaskId",
                projectId = projectId, pipelineId = pipelineId, userId = userId,
                buildId = buildId, stageId = retryTaskInStageId!!, actionType = actionType
            )
        )
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = if (skipFailedTask) {
                "$userId skip the fail task"
            } else {
                "$userId retry fail task"
            },
            tag = retryStartTaskId!!,
            jobId = retryTaskInContainerId,
            executeCount = executeCount,
            stepId = null
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

    private fun reviewParamsCheck(
        projectId: String,
        buildId: String,
        params: List<ManualReviewParam>,
        pipelineId: String,
        taskName: String,
        taskId: String
    ) {
        // feat: 检测stage审核参数与入参之间的不规范写法 #11853
        val variables = buildVariableService.getAllVariableWithType(projectId, buildId).associateBy { it.key }
        params.forEach {
            val prefix = "[$projectId][$pipelineId][$buildId][$taskId][$taskName]"
            variables[it.key]?.let { check ->
                logger.info("$prefix|11853_CHECK|TASK|reviewParams|key=${it.key}|")
                if (check.readOnly == true) {
                    logger.info("$prefix|11853_CHECK|TASK|READ_ONLY|key=${it.key}|")
                }
            }
            variables["variables.${it.key}"]?.let { check ->
                logger.info("$prefix|11853_CHECK|TASK|HAS_VARIABLES|key=${it.key}|")
                if (check.readOnly == true) {
                    logger.info("$prefix|11853_CHECK|TASK|VARIABLES_READ_ONLY|key=${it.key}|")
                }
            }
        }
    }

    /**
     * 手动审批
     */
    fun manualDealReview(taskId: String, userId: String, params: ReviewParam): Boolean {
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
                        reviewParamsCheck(
                            projectId = projectId,
                            buildId = buildId,
                            params = params.params,
                            pipelineId = pipelineId,
                            taskName = taskName,
                            taskId = taskId
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
                        operation = "manualDealReview#$taskId",
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
                    return true
                }
            }
        return false
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
    fun completeClaimBuildTask(
        completeTask: CompleteTask,
        endBuild: Boolean = false,
        endBuildMsg: String? = null
    ): PipelineBuildTask? {
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
                    actionType = if (endBuild) ActionType.END else ActionType.REFRESH,
                    errorCode = completeTask.errorCode ?: 0,
                    errorTypeName = completeTask.errorType?.getI18n(I18nUtil.getDefaultLocaleLanguage()),
                    executeCount = buildTask.executeCount,
                    reason = endBuildMsg ?: completeTask.errorMsg
                )
            )
        }
        return buildTask
    }

    fun updateBuildNo(
        projectId: String,
        pipelineId: String,
        buildNo: Int,
        debug: Boolean
    ) {
        pipelineBuildSummaryDao.updateBuildNo(dslContext, projectId, pipelineId, buildNo, debug)
    }

    fun updateExecuteCount(
        projectId: String,
        buildId: String,
        executeCount: Int
    ) {
        pipelineBuildDao.updateExecuteCount(dslContext, projectId, buildId, executeCount)
    }

    /**
     * 开始最新一次构建
     */
    fun startLatestRunningBuild(latestRunningBuild: LatestRunningBuild) {
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
                executeCount = latestRunningBuild.executeCount,
                buildStatus = BuildStatus.RUNNING
            )
            pipelineBuildDao.startBuild(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                buildId = latestRunningBuild.buildId,
                startTime = if (latestRunningBuild.executeCount == 1) startTime else null,
                debug = latestRunningBuild.debug
            )
            pipelineInfoDao.updateLatestStartTime(
                dslContext = transactionContext,
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                startTime = startTime
            )
            pipelineBuildSummaryDao.startLatestRunningBuild(
                dslContext = transactionContext,
                latestRunningBuild = latestRunningBuild,
                executeCount = latestRunningBuild.executeCount,
                debug = latestRunningBuild.debug
            )
        }
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "buildStart",
                projectId = latestRunningBuild.projectId,
                pipelineId = latestRunningBuild.pipelineId,
                userId = latestRunningBuild.userId,
                buildId = latestRunningBuild.buildId,
                // 刷新历史列表、详情、状态页面
                refreshTypes = RefreshType.HISTORY.binary or
                    RefreshType.DETAIL.binary or
                    RefreshType.STATUS.binary or
                    RefreshType.RECORD.binary
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
        errorInfoList: List<ErrorInfo>?,
        timeCost: BuildRecordTimeCost?
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
                isStageFinish = currentBuildStatus.name == BuildStatus.STAGE_SUCCESS.name,
                debug = latestRunningBuild.debug
            )
        }
        with(latestRunningBuild) {
            val executeTime = try {
                timeCost?.executeCost ?: getExecuteTime(latestRunningBuild.projectId, buildId)
            } catch (ignored: Throwable) {
                logger.warn("[$pipelineId]|getExecuteTime-$buildId exception:", ignored)
                0L
            }
            logger.info("[$pipelineId]|getExecuteTime-$buildId executeTime: $executeTime")

            val buildParameters = getBuildParametersFromStartup(projectId, buildId)
            // 修正推荐版本号过长和流水号重复更新导致的问题
            val recommendVersion = PipelineVarUtil.getRecommendVersion(buildParameters)
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
                errorInfoList = errorInfoList,
                debug = debug
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
                    refreshTypes = RefreshType.DETAIL.binary or
                        RefreshType.STATUS.binary or
                        RefreshType.RECORD.binary
                )
            )
            logger.info("[$pipelineId]|finishLatestRunningBuild-$buildId|status=$status")
        }
    }

    fun getBuildParametersFromStartup(
        projectId: String,
        buildId: String,
        queryDslContext: DSLContext? = null
    ): List<BuildParameters> {
        return try {
            val buildParameters = pipelineBuildDao.getBuildParameters(
                dslContext = queryDslContext ?: dslContext,
                projectId = projectId,
                buildId = buildId
            )
            return if (buildParameters.isNullOrEmpty()) {
                emptyList()
            } else {
                (JsonUtil.getObjectMapper().readValue(buildParameters) as List<BuildParameters>)
                    .filter { !it.key.startsWith(ElementUtils.skipPrefix) }
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

    fun getLastTimeBuild(projectId: String, pipelineId: String, debug: Boolean): BuildInfo? {
        return pipelineBuildDao.getLatestBuild(dslContext, projectId, pipelineId, debug)
    }

    fun getPipelineBuildHistoryCount(
        projectId: String,
        pipelineId: String,
        debugVersion: Int?
    ): Int {
        return pipelineBuildDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            debugVersion = debugVersion
        )
    }

    fun getBuilds(
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?,
        debugVersion: Int?
    ): List<String> {
        return pipelineBuildDao.getBuilds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildStatus = buildStatus,
            debugVersion = debugVersion
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
        startUser: List<String>? = null,
        queryDslContext: DSLContext? = null,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): Int {
        return pipelineBuildDao.count(
            dslContext = queryDslContext ?: dslContext,
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
            startUser = startUser,
            debug = debug,
            triggerAlias = triggerAlias,
            triggerBranch = triggerBranch,
            triggerUser = triggerUser
        )
    }

    fun getTotalBuildHistoryCount(
        projectId: String,
        pipelineId: String,
        status: List<BuildStatus>?,
        startTimeEndTime: Long? = null
    ): Int {
        val normal = pipelineBuildDao.countByStatus(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            status = status,
            startTimeEndTime = startTimeEndTime,
            onlyDebug = false
        )
        val debug = pipelineBuildDao.countByStatus(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            status = status,
            startTimeEndTime = startTimeEndTime,
            onlyDebug = true
        )
        return normal + debug
    }

    fun getAllBuildNum(projectId: String, pipelineId: String, debugVersion: Int? = null): Collection<Int> {
        return pipelineBuildDao.listPipelineBuildNum(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            debugVersion = debugVersion,
            offset = 0,
            limit = Int.MAX_VALUE
        )
    }

    // 获取流水线最后的构建号
    fun getLatestBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestBuild(dslContext, projectId, pipelineId, false)?.buildId
    }

    // 获取流水线最后完成的构建号
    fun getLatestFinishedBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestFinishedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    // 获取流水线最后成功的构建号
    fun getLatestSucceededBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestSucceedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    // 获取流水线最后失败的构建号
    fun getLatestFailedBuildId(projectId: String, pipelineId: String): String? {
        return pipelineBuildDao.getLatestFailedBuild(dslContext, projectId, pipelineId)?.buildId
    }

    fun getBuildIdByBuildNum(
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        debugVersion: Int? = null,
        archiveFlag: Boolean? = false
    ): String? {
        return pipelineBuildDao.getBuildByBuildNum(
            dslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT),
            projectId = projectId,
            pipelineId = pipelineId,
            buildNum = buildNum,
            debugVersion = debugVersion
        )?.buildId
    }

    fun updateBuildInfoStatus2Queue(projectId: String, buildId: String, oldStatus: BuildStatus, showMsg: String) {
        pipelineBuildDao.updateBuildStageStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageStatus = listOf(
                BuildStageStatus(
                    stageId = TRIGGER_STAGE,
                    name = TRIGGER_STAGE,
                    status = I18nUtil.getCodeLanMessage(BUILD_QUEUE),
                    showMsg = showMsg
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
        artifactListJsonString: String,
        artifactQualityList: String
    ): Boolean {
        return pipelineBuildDao.updateArtifactList(
            dslContext = dslContext,
            artifactList = artifactListJsonString,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactQualityList = artifactQualityList
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

    fun updateRecommendVersion(projectId: String, buildId: String, recommendVersion: String) {
        pipelineBuildDao.updateRecommendVersion(dslContext, projectId, buildId, recommendVersion)
    }

    fun updateBuildParameters(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildParameters: Collection<BuildParameters>,
        debug: Boolean
    ): Boolean {
        return pipelineBuildDao.updateBuildParameters(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildParameters = buildParameters,
            debug = debug
        )
    }

    fun concurrencyCancelBuildPipeline(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        groupName: String,
        detailUrl: String
    ) {
        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        try {
            redisLock.lock()
            val buildInfo = getBuildInfo(projectId, pipelineId, buildId)
            val tasks = pipelineTaskService.getRunningTask(projectId, buildId)
            tasks.forEach { task ->
                val taskId = task["taskId"]?.toString() ?: ""
                val stepId = task["stepId"]?.toString() ?: ""
                logger.info("build($buildId) shutdown by $userId, taskId: $taskId, status: ${task["status"] ?: ""}")
                val containerId = task["containerId"]?.toString() ?: ""
                // #7599 兼容短时间取消状态异常优化
                val cancelTaskSetKey = TaskUtils.getCancelTaskIdRedisKey(buildId, containerId, false)
                redisOperation.addSetValue(cancelTaskSetKey, taskId)
                redisOperation.expire(cancelTaskSetKey, TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS))
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "[concurrency] Canceling since <a target='_blank' href='$detailUrl'>" +
                        "a higher priority waiting request</a> for group($groupName) exists",
                    tag = taskId,
                    containerHashId = task["containerId"]?.toString() ?: "",
                    executeCount = task["executeCount"] as? Int ?: 1,
                    jobId = null, stepId = stepId
                )
            }
            if (tasks.isEmpty()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "[concurrency] Canceling all since <a target='_blank' href='$detailUrl'>" +
                        "a higher priority waiting request</a> for group($groupName) exists",
                    tag = "QueueInterceptor",
                    containerHashId = "",
                    executeCount = 1,
                    jobId = null, stepId = "QueueInterceptor"
                )
            }
            try {
                cancelBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = userId,
                    executeCount = buildInfo?.executeCount ?: 1,
                    buildStatus = BuildStatus.CANCELED
                )
                logger.info("Cancel the pipeline($pipelineId) of instance($buildId) by the user($userId)")
            } catch (t: Throwable) {
                logger.warn("Fail to shutdown the build($buildId) of pipeline($pipelineId)", t)
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun updateAsyncStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        asyncStatus: String
    ) {
        taskBuildRecordService.updateAsyncStatus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            asyncStatus = asyncStatus
        )
    }

    fun getBuildVariableService(
        projectId: String,
        pipelineId: String,
        buildId: String,
        keys: Set<String>
    ): Map<String, String> {
        return buildVariableService.getAllVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            keys = keys
        )
    }
}
