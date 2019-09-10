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
import com.tencent.devops.artifactory.api.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.atom.vm.DispatchVMShutdownTaskAtom
import com.tencent.devops.process.engine.atom.vm.DispatchVMStartupTaskAtom
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildStartupParamService
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_ALIASNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_BRANCHNAME
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_COMMENT
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_ID
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_NEW_COMMIT_TIMES
import com.tencent.devops.process.utils.PIPELINE_MATERIAL_URL
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
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.process.websocket.ChangeType
import com.tencent.devops.process.websocket.PipelineStatusChangeEvent
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

/**
 * 流水线运行时相关的服务
 * @version 1.0
 */
@Service
class PipelineRuntimeService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildIdGenerator: BuildIdGenerator,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineBuildStageDao: PipelineBuildStageDao,
    private val pipelineBuildContainerDao: PipelineBuildContainerDao,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
    private val buildDetailDao: BuildDetailDao,
    private val client: Client,
    private val buildStartupParamService: BuildStartupParamService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
    }

    fun createPipelineBuildSummary(projectId: String, pipelineId: String, buildNo: BuildNo?) {
        pipelineBuildSummaryDao.create(dslContext, projectId, pipelineId, buildNo)
    }

    fun deletePipelineBuilds(projectId: String, pipelineId: String, userId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildSummaryDao.delete(transactionContext, projectId, pipelineId)
            val runningBuilds = pipelineBuildDao.getBuildTasksByStatus(
                transactionContext, projectId, pipelineId,
                setOf(
                    BuildStatus.RUNNING, BuildStatus.REVIEWING,
                    BuildStatus.QUEUE, BuildStatus.PREPARE_ENV,
                    BuildStatus.UNEXEC
                )
            )
            for (build in runningBuilds) {
                if (build != null) with(build) {
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildCancelEvent(
                            "deletePipelineBuilds",
                            projectId,
                            pipelineId,
                            userId,
                            buildId,
                            BuildStatus.TERMINATE
                        )
                    )
                    pipelineBuildVarDao.deleteBuildVar(transactionContext, buildId)
                }
            }
            pipelineBuildDao.deletePipelineBuilds(transactionContext, projectId, pipelineId)
            pipelineBuildStageDao.deletePipelineBuildStages(transactionContext, projectId, pipelineId)
            pipelineBuildContainerDao.deletePipelineBuildContainers(transactionContext, projectId, pipelineId)
            pipelineBuildTaskDao.deletePipelineBuildTasks(transactionContext, projectId, pipelineId)
        }
    }

    fun getBuildInfo(buildId: String): BuildInfo? {
        val t = pipelineBuildDao.getBuildInfo(dslContext, buildId)
        return pipelineBuildDao.convert(t)
    }

    /**
     * TODO 这个与下面的getBuildNoByByPair方法重复了，需要后面搞清楚前面接口是否不用了，重构一版
     * @see #com.tencent.devops.process.api.ServicePipelineResource#getBuildNoByBuildIds
     */
    fun listBuildInfoByBuildIds(buildIds: Set<String>): MutableMap<String, Int> {
        val result = mutableMapOf<String, Int>()
        val buildInfoList = pipelineBuildDao.listBuildInfoByBuildIds(dslContext, buildIds)
        buildInfoList.forEach {
            result[it.buildId] = it.buildNum
        }
        return result
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
        pipelineIds: Collection<String>? = null
    ): Result<out Record> {
        return pipelineBuildSummaryDao.listPipelineInfoBuildSummary(dslContext, projectId, channelCode, pipelineIds)
    }

    fun getVariable(buildId: String, varName: String): String? {
        val vars = pipelineBuildVarDao.getVars(dslContext, buildId, varName)
        return if (vars.isNotEmpty()) vars[varName] else null
    }

    fun getAllVariable(buildId: String): Map<String, String> {
        return pipelineBuildVarDao.getVars(dslContext, buildId)
    }

    fun getRunningTask(projectId: String, buildId: String): List<Pair<String, BuildStatus>> {
        val listByStatus = pipelineBuildTaskDao.listByStatus(
            dslContext,
            buildId,
            null,
            listOf(BuildStatus.RUNNING, BuildStatus.REVIEWING)
        )
        val list = mutableListOf<Pair<String, BuildStatus>>()
        val buildStatus = BuildStatus.values()
        listByStatus.forEach {
            list.add(Pair(it.taskId, buildStatus[it.status]))
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
            dslContext,
            buildId,
            containerId,
            if (buildStatus != null) setOf(buildStatus) else null
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
        logger.info("[$buildId]|updateContainerStatus |status=$buildStatus|containerId=$containerId|stageId=$stageId")
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

    fun updateStageStatus(buildId: String, stageId: String, buildStatus: BuildStatus) {
        pipelineBuildStageDao.updateStatus(dslContext, buildId, stageId, buildStatus)
    }

    fun updateStage(
        buildId: String,
        stageId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        buildStatus: BuildStatus
    ) {
        pipelineBuildStageDao.update(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            buildStatus = buildStatus,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun listStages(buildId: String): List<PipelineBuildStage> {
        val list = pipelineBuildStageDao.listByBuildId(dslContext, buildId)
        val result = mutableListOf<PipelineBuildStage>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildStageDao.convert(it)!!)
            }
        }
        return result
    }

    fun listPipelineBuildHistory(projectId: String, pipelineId: String, offset: Int, limit: Int): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext, projectId, pipelineId, offset, if (limit < 0) {
                1000
            } else limit
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
        remark: String?
    ): List<BuildHistory> {
        val currentTimestamp = System.currentTimeMillis()
        // 限制最大一次拉1000，防止攻击
        val list = pipelineBuildDao.listPipelineBuildInfo(
            dslContext,
            projectId,
            pipelineId,
            materialAlias,
            materialUrl,
            materialBranch,
            materialCommitId,
            materialCommitMessage,
            status,
            trigger,
            queueTimeStartTime,
            queueTimeEndTime,
            startTimeStartTime,
            startTimeEndTime,
            endTimeStartTime,
            endTimeEndTime,
            totalTimeMin,
            totalTimeMax,
            remark,
            offset,
            if (limit < 0) {
                1000
            } else limit
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
                deleteReason = "",
                currentTimestamp = currentTimestamp,
                material = if (material != null) {
                    JsonUtil.getObjectMapper().readValue(material) as List<PipelineBuildMaterial>
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
                startType = getStartType(trigger, webhookType),
                recommendVersion = recommendVersion
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
            PipelineBuildCancelEvent(javaClass.simpleName, projectId, pipelineId, userId, buildId, buildStatus)
        )

        return true
    }

    fun startBuild(pipelineInfo: PipelineInfo, fullModel: Model, params: Map<String, Any>): String {
        val startBuildStatus: BuildStatus = BuildStatus.QUEUE // 默认都是排队状态
        val buildId = // 如果不是从某个步骤重试的，则当成一个全新的, 后续看产品用户需求，是否需要仍然在当前build跑
            if (params[PIPELINE_RETRY_START_TASK_ID] == null) {
                buildIdGenerator.getNextId()
            } else {
                params[PIPELINE_RETRY_BUILD_ID].toString()
            }

        val startType = StartType.valueOf(params[PIPELINE_START_TYPE] as String)
        val parentBuildId = if (params[PIPELINE_START_PARENT_BUILD_ID] != null) {
            params[PIPELINE_START_PARENT_BUILD_ID].toString()
        } else null

        val parentTaskId = if (params[PIPELINE_START_PARENT_BUILD_TASK_ID] != null) {
            params[PIPELINE_START_PARENT_BUILD_TASK_ID].toString()
        } else null
        val channelCode = if (params[PIPELINE_START_CHANNEL] != null) {
            ChannelCode.valueOf(params[PIPELINE_START_CHANNEL].toString())
        } else ChannelCode.BS

        var taskCount = 0
        val userId = params[PIPELINE_START_USER_ID].toString()
        val triggerUser = params[PIPELINE_START_USER_NAME].toString()
        // 原子重试
        val retryStartTaskId: String? = if (params[PIPELINE_RETRY_START_TASK_ID] != null) {
            params[PIPELINE_RETRY_START_TASK_ID].toString()
        } else {
            null
        }
        val actionType: ActionType = if (params[PIPELINE_RETRY_COUNT] != null) {
            ActionType.RETRY
        } else {
            ActionType.START
        }

        var firstTaskId = if (params[PIPELINE_START_TASK_ID] != null) {
            params[PIPELINE_START_TASK_ID].toString()
        } else ""

        val updateExistsRecord: MutableList<TPipelineBuildTaskRecord> = mutableListOf()
        dslContext.transaction { configuration ->

            val transactionContext = DSL.using(configuration)

            val buildHistoryRecord = pipelineBuildDao.getBuildInfo(transactionContext, buildId)
            val sModel: Model = if (buildHistoryRecord != null) {
                val record = buildDetailDao.get(dslContext, buildId)
                if (record != null) {
                    JsonUtil.getObjectMapper().readValue(record.model, Model::class.java)
                } else {
                    fullModel
                }
            } else {
                fullModel
            }

            val lastTimeBuildTaskRecords = pipelineBuildTaskDao.getByBuildId(transactionContext, buildId)
            val lastTimeBuildContainerRecords = pipelineBuildContainerDao.listByBuildId(transactionContext, buildId)
            val lastTimeBuildStageRecords = pipelineBuildStageDao.listByBuildId(transactionContext, buildId)

            val buildTaskList = mutableListOf<PipelineBuildTask>()
            val buildContainers = mutableListOf<PipelineBuildContainer>()
            val buildStages = mutableListOf<PipelineBuildStage>()

            val updateStageExistsRecord: MutableList<TPipelineBuildStageRecord> = mutableListOf()
            val updateContainerExistsRecord: MutableList<TPipelineBuildContainerRecord> = mutableListOf()

            var containerSeq = 0
            sModel.stages.forEachIndexed s@{ index, stage ->
                val stageId = stage.id!!
                var needUpdateStage = false
                stage.containers.forEach c@{ container ->
                    var needUpdateContainer = false
                    var taskSeq = 0
                    // 构建机环境处理，需要先创建一个的启动构建机原子任务
                    val containerId = container.id!!
                    val containerType = container.getClassType()
                    // 构建机环境 或者 无构建环境
                    makeStartVMTask(
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = taskSeq,
                        lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                        updateExistsRecord = updateExistsRecord,
                        buildTaskList = buildTaskList,
                        pipelineInfo = pipelineInfo,
                        buildId = buildId,
                        stageId = stageId,
                        userId = userId,
                        retryStartTaskId = retryStartTaskId
                    )
                    container.elements.forEach nextElement@{ atomElement ->
                        taskCount++
                        taskSeq++
                        if (firstTaskId.isBlank() && container is TriggerContainer && atomElement.isElementEnable()) {
                            if (atomElement is RemoteTriggerElement) {
                                firstTaskId = atomElement.id!!
                            } else if (atomElement is ManualTriggerElement) {
                                if (startType == StartType.MANUAL ||
                                    startType == StartType.SERVICE ||
                                    startType == StartType.PIPELINE
                                ) {
                                    firstTaskId = atomElement.id!!
                                }
                            } else if (atomElement is TimerTriggerElement) {
                                if (startType == StartType.TIME_TRIGGER) {
                                    firstTaskId = atomElement.id!!
                                }
                            }
                        } else {

                            val skipKey = SkipElementUtils.getSkipElementVariableName(atomElement.id!!)
                            val status = if (params[skipKey] != null && params[skipKey] == "true") {
                                BuildStatus.SKIP // 跳过
                            } else if (atomElement.additionalOptions != null && !atomElement.additionalOptions!!.enable) {
                                BuildStatus.SKIP // 跳过
                            } else {
                                BuildStatus.QUEUE
                            }

                            if (lastTimeBuildTaskRecords.isNotEmpty()) {
                                if (!retryStartTaskId.isNullOrBlank()) {
                                    if (retryStartTaskId == atomElement.id) {
                                        // 重试判断是否存在原子重试，其他保持不变
                                        val taskRecord = retryTaskContainerStatus(
                                            lastTimeBuildTaskRecords, container, retryStartTaskId!!, atomElement
                                        )
                                        if (taskRecord != null) {
                                            updateExistsRecord.add(taskRecord)
                                            needUpdateContainer = true
                                        }
                                    } else { // 重试之外的其他任务
                                        val target =
                                            findTaskRecord(lastTimeBuildTaskRecords, container, atomElement.id!!)
                                        // 如果当前原子之前是完成状态，则跳过
                                        if (target == null || BuildStatus.isFinish(BuildStatus.values()[target.status])) {
                                            return@nextElement
                                        }

                                        val taskRecord = retryTaskContainerStatus(
                                            lastTimeBuildTaskRecords, container, atomElement.id!!, atomElement
                                        )
                                        if (taskRecord != null) {
                                            updateExistsRecord.add(taskRecord)
                                            needUpdateContainer = true
                                        }
                                    }
                                } else {
                                    // 如果当前原子之前是要求跳过的状态，则忽略不重试
                                    if (status == BuildStatus.SKIP) {
                                        return@nextElement
                                    }

                                    val taskRecord = retryTaskContainerStatus(
                                        lastTimeBuildTaskRecords, container, atomElement.id!!, atomElement
                                    )
                                    if (taskRecord != null) {
                                        updateExistsRecord.add(taskRecord)
                                    }
                                }
                            } else {

                                if (BuildStatus.isFinish(status)) {
                                    atomElement.status = status.name
                                }

                                val taskName =
                                    if (atomElement.name.length > 128) {
                                        atomElement.name.substring(0, 128)
                                    } else {
                                        atomElement.name
                                    }

                                buildTaskList.add(
                                    PipelineBuildTask(
                                        projectId = pipelineInfo.projectId,
                                        pipelineId = pipelineInfo.pipelineId,
                                        buildId = buildId,
                                        stageId = stageId,
                                        containerId = containerId,
                                        containerType = containerType,
                                        taskSeq = taskSeq,
                                        taskId = atomElement.id!!,
                                        taskName = taskName,
                                        taskType = atomElement.getClassType(),
                                        taskAtom = atomElement.getTaskAtom(),
                                        status = status,
                                        taskParams = atomElement.genTaskParams(),
                                        additionalOptions = atomElement.additionalOptions,
                                        executeCount = 1,
                                        starter = userId,
                                        approver = null,
                                        subBuildId = null
                                    )
                                )
                            }
                        }
                    }
                    // 构建机或原子市场原子的环境处理，需要一个的清理构建机原子任务
                    makeStopVMTask(
                        container = container,
                        containerSeq = containerSeq,
                        taskSeq = taskSeq,
                        lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                        updateExistsRecord = updateExistsRecord,
                        buildTaskList = buildTaskList,
                        pipelineInfo = pipelineInfo,
                        buildId = buildId,
                        stageId = stageId,
                        userId = userId,
                        retryStartTaskId = retryStartTaskId
                    )

                    if (lastTimeBuildContainerRecords.isNotEmpty()) {
                        if (needUpdateContainer) {
                            run findHistoryContainer@{
                                lastTimeBuildContainerRecords.forEach {
                                    if (it.containerId == containerId) {
                                        it.status = BuildStatus.QUEUE.ordinal
                                        it.executeCount += 1
                                        updateContainerExistsRecord.add(it)
                                        return@findHistoryContainer
                                    }
                                }
                            }
                        }
                    } else {
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
                                projectId = pipelineInfo.projectId, pipelineId = pipelineInfo.pipelineId,
                                buildId = buildId, stageId = stageId, containerId = containerId,
                                containerType = containerType, seq = containerSeq, status = BuildStatus.QUEUE,
                                controlOption = controlOption
                            )
                        )
                    }
                    if (needUpdateContainer) {
                        needUpdateStage = true
                    }
                    containerSeq++
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
                            projectId = pipelineInfo.projectId, pipelineId = pipelineInfo.pipelineId,
                            buildId = buildId, stageId = stageId, seq = index, status = BuildStatus.QUEUE
                        )
                    )
                }
            }

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
                    webhookType = params[PIPELINE_WEBHOOK_TYPE] as String?
                )
                // detail记录,未正式启动，先排队状态
                buildDetailDao.create(
                    dslContext = transactionContext,
                    buildId = buildId,
                    startType = startType,
                    buildNum = buildNum,
                    model = JsonUtil.toJson(sModel),
                    buildStatus = BuildStatus.QUEUE
                )
                // 写入版本号
                pipelineBuildVarDao.save(transactionContext, buildId, PIPELINE_BUILD_NUM, buildNum)
            }

            // 保存参数
            pipelineBuildVarDao.batchSave(transactionContext, buildId, params)

            // 上一次存在的需要重试的任务直接Update，否则就插入
            if (updateExistsRecord.isEmpty()) {
                // 保持要执行的任务
                logger.info("batch save to pipelineBuildTask, buildTaskList size: ${buildTaskList.size}")
                pipelineBuildTaskDao.batchSave(transactionContext, buildTaskList)
            } else {
                logger.info("batch store to pipelineBuildTask, updateExistsRecord size: ${updateExistsRecord.size}")
                transactionContext.batchStore(updateExistsRecord).execute()
            }

            if (updateContainerExistsRecord.isEmpty()) {
                pipelineBuildContainerDao.batchSave(transactionContext, buildContainers)
            } else {
                transactionContext.batchStore(updateContainerExistsRecord).execute()
            }

            if (updateStageExistsRecord.isEmpty()) {
                pipelineBuildStageDao.batchSave(transactionContext, buildStages)
            } else {
                transactionContext.batchStore(updateStageExistsRecord).execute()
            }
            // 排队计数+1
            pipelineBuildSummaryDao.updateQueueCount(transactionContext, pipelineInfo.pipelineId, 1)
        }

// 发送开始事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                "startBuild",
                pipelineInfo.projectId, pipelineInfo.pipelineId, userId,
                buildId, firstTaskId, startBuildStatus, actionType
            ), // 监控事件
            PipelineBuildMonitorEvent(
                "startBuild",
                pipelineInfo.projectId, pipelineInfo.pipelineId, userId,
                buildId, startBuildStatus
            ),
            PipelineStatusChangeEvent(
                source = "pipelineHistoryChangeEvent",
                pipelineId = pipelineInfo.pipelineId,
                changeType = ChangeType.HISTORY,
                buildId = buildId,
                projectId = pipelineInfo.projectId,
                userId = userId
            ),
            PipelineStatusChangeEvent(
                source = "pipelineDetailChangeEvent",
                pipelineId = pipelineInfo.pipelineId,
                changeType = ChangeType.DETAIL,
                buildId = buildId,
                projectId = pipelineInfo.projectId,
                userId = userId
            )
        )
        return buildId
    }

    private fun makeStartVMTask(
        container: Container,
        containerSeq: Int,
        taskSeq: Int,
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        updateExistsRecord: MutableList<TPipelineBuildTaskRecord>,
        buildTaskList: MutableList<PipelineBuildTask>,
        pipelineInfo: PipelineInfo,
        buildId: String,
        stageId: String,
        userId: String,
        retryStartTaskId: String?
    ) {

        if (container !is VMBuildContainer) {
            return
        }

        // 是任务重试的则做下重试标志，如果有找到的话
        if (lastTimeBuildTaskRecords.isNotEmpty()) {
            val needStartVM =
                if (!retryStartTaskId.isNullOrBlank()) { // 在当前Job中找到要重试的原子，则当前Job需要重启
                    findTaskRecord(lastTimeBuildTaskRecords, container, retryStartTaskId) != null
                } else {
                    true
                }
            if (!needStartVM) {
                logger.info("[${pipelineInfo.pipelineId}]|RETRY| $retryStartTaskId not in container(${container.name}")
                return
            }
            val taskId = VMUtils.genStartVMTaskId(containerSeq, taskSeq)
            val taskRecord =
                retryTaskContainerStatus(lastTimeBuildTaskRecords, container, taskId)
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
            } else {
                logger.info("[${pipelineInfo.pipelineId}]|RETRY| do not need vm start (${container.name})")
            }
            return
        }

        buildTaskList.add(
            DispatchVMStartupTaskAtom.makePipelineBuildTask(
                pipelineInfo.projectId, pipelineInfo.pipelineId, buildId, stageId,
                container, containerSeq, taskSeq, userId
            )
        )
    }

    private fun haveMarketAtom(container: Container): Boolean {
        container.elements.forEach {
            if (it is MarketBuildAtomElement || it is MarketBuildLessAtomElement) {
                return true
            }
        }
        return false
    }

    private fun makeStopVMTask(
        container: Container,
        containerSeq: Int,
        taskSeq: Int,
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        updateExistsRecord: MutableList<TPipelineBuildTaskRecord>,
        buildTaskList: MutableList<PipelineBuildTask>,
        pipelineInfo: PipelineInfo,
        buildId: String,
        stageId: String,
        userId: String,
        retryStartTaskId: String?
    ) {
        if (container !is VMBuildContainer) {
            return
        }
        if (lastTimeBuildTaskRecords.isNotEmpty()) {
            val needStopVM =
                if (!retryStartTaskId.isNullOrBlank()) { // 在当前Job中找到要重试的原子，则当前Job需要关闭构建机
                    findTaskRecord(lastTimeBuildTaskRecords, container, retryStartTaskId) != null
                } else {
                    true
                }
            if (!needStopVM) {
                logger.info("[${pipelineInfo.pipelineId}]|RETRY| $retryStartTaskId not in container(${container.name}")
                return
            }

            val endPointTaskId = VMUtils.genEndPointTaskId(VMUtils.genVMSeq(containerSeq, taskSeq - 1))
            var taskRecord =
                retryTaskContainerStatus(lastTimeBuildTaskRecords, container, endPointTaskId)
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
                val stopVmTaskId = VMUtils.genStopVMTaskId(VMUtils.genVMSeq(containerSeq, taskSeq))
                taskRecord =
                    retryTaskContainerStatus(lastTimeBuildTaskRecords, container, stopVmTaskId)
                if (taskRecord != null) {
                    updateExistsRecord.add(taskRecord)
                } else {
                    logger.warn("[${pipelineInfo.pipelineId}]|RETRY| no found $stopVmTaskId")
                }
            } else {
                logger.info("[${pipelineInfo.pipelineId}]|RETRY| do not need vm start (${container.name})")
            }
        } else {

            buildTaskList.addAll(
                DispatchVMShutdownTaskAtom.makePipelineBuildTasks(
                    pipelineInfo.projectId, pipelineInfo.pipelineId, buildId, stageId,
                    container, containerSeq, taskSeq, userId
                )
            )
        }
    }

    /**
     * 刷新要重试的任务，如果任务是在当前容器，需要将当前容器的状态一并刷新
     * @param lastTimeBuildTaskRecords 之前重试任务记录列表
     * @param container 当前任务所在构建容器
     * @param retryStartTaskId 要重试的任务i
     * @param atomElement 需要重置状态的任务原子Element，可以为空。
     */
    private fun retryTaskContainerStatus(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        container: Container,
        retryStartTaskId: String,
        atomElement: Element? = null
    ): TPipelineBuildTaskRecord? {
        val target: TPipelineBuildTaskRecord? = findTaskRecord(lastTimeBuildTaskRecords, container, retryStartTaskId)

        if (target != null) {
            container.status = null // 重置状态为空
            container.startEpoch = null
            container.elementElapsed = null
            container.systemElapsed = null
            target.executeCount += 1 // 执行次数增1
            target.status = BuildStatus.QUEUE.ordinal // 进入排队状态
            if (atomElement != null) { // 将原子状态重置
                atomElement.status = null // BuildStatus.QUEUE.name
                atomElement.executeCount++
                atomElement.elapsed = null
                atomElement.startEpoch = null
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
    fun completeClaimBuildTask(buildId: String, taskId: String, userId: String, buildStatus: BuildStatus) {
        val buildTask = getBuildTask(buildId, taskId)
        if (buildTask != null) {
            updateTaskStatus(buildId, taskId, userId, buildStatus)
            // 刷新容器，下发后面的任务
            with(buildTask) {
                pipelineEventDispatcher.dispatch(
                    PipelineBuildContainerEvent(
                        javaClass.simpleName,
                        projectId,
                        pipelineId,
                        userId,
                        buildId,
                        stageId,
                        containerId,
                        containerType, ActionType.REFRESH
                    )
                )
            }
        }
    }

    fun setVariable(buildId: String, varName: String, varValue: Any) {
        pipelineBuildVarDao.save(dslContext, buildId, varName, varValue)
    }

    fun batchSetVariable(buildId: String, variables: Map<String, Any>) {
        pipelineBuildVarDao.batchSave(dslContext, buildId, variables)
    }

    fun updateBuildNo(pipelineId: String, buildNo: Int) {
        pipelineBuildSummaryDao.updateBuildNo(dslContext, pipelineId, buildNo)
    }

//    fun resetBuildNum(pipelineId: String, buildNum: Int) {
//        pipelineBuildSummaryDao.updateBuildNum(dslContext, pipelineId, buildNum)
//    }

    fun getBuildNo(pipelineId: String): Int {
        return pipelineBuildSummaryDao.getBuildNo(dslContext, pipelineId)
    }

//    fun removeVariable(buildId: String, varName: String) {
//        pipelineBuildVarDao.deleteBuildVar(dslContext, buildId, varName)
//    }

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
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, latestRunningBuild.buildId) ?: return
        pipelineEventDispatcher.dispatch(
            PipelineStatusChangeEvent(
                source = "pipelineStatusChangeEvent",
                pipelineId = latestRunningBuild.pipelineId,
                changeType = ChangeType.STATUS,
                buildId = latestRunningBuild.buildId,
                projectId = pipelineBuildInfo.projectId,
                userId = latestRunningBuild.userId
            ),
            PipelineStatusChangeEvent(
                source = "pipelineHistoryChangeEvent",
                pipelineId = latestRunningBuild.pipelineId,
                changeType = ChangeType.HISTORY,
                buildId = latestRunningBuild.buildId,
                projectId = pipelineBuildInfo.projectId,
                userId = latestRunningBuild.userId
            ),
            PipelineStatusChangeEvent(
                source = "pipelineDetailChangeEvent",
                pipelineId = pipelineBuildInfo.pipelineId,
                changeType = ChangeType.DETAIL,
                buildId = pipelineBuildInfo.buildId,
                projectId = pipelineBuildInfo.projectId,
                userId = pipelineBuildInfo.startUser
            )
        )

        logger.info("[${latestRunningBuild.pipelineId}]|startLatestRunningBuild-${latestRunningBuild.buildId}")
    }

    /**
     * 结束构建
     * @param latestRunningBuild 最一次构建的要更新的状态信息
     * @param currentBuildStatus 当前一次构建的当前状态
     */
    fun finishLatestRunningBuild(latestRunningBuild: LatestRunningBuild, currentBuildStatus: BuildStatus) {
        if (BuildStatus.isReadyToRun(currentBuildStatus)) {
            // 减1,当作没执行过
            pipelineBuildSummaryDao.updateQueueCount(dslContext, latestRunningBuild.pipelineId, -1)
        } else {
            pipelineBuildSummaryDao.finishLatestRunningBuild(dslContext, latestRunningBuild)
        }
        with(latestRunningBuild) {
            val materials: List<PipelineBuildMaterial> = try {
                getPipelineBuildMaterial(buildId)
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|getPipelineBuildMaterial-$buildId exception:", e)
                mutableListOf()
            }
            logger.info("[$pipelineId]|getPipelineBuildMaterial-$buildId material: ${JsonUtil.toJson(materials)}")

            val artifactList: List<FileInfo> = try {
                getArtifactList(userId = userId, projectId = projectId, pipelineId = pipelineId, buildId = buildId)
            } catch (e: Throwable) {
                logger.error("[$pipelineId]|getArtifactList-$buildId exception:", e)
                mutableListOf()
            }
            logger.info("[$pipelineId]|getArtifactList-$buildId artifact: ${JsonUtil.toJson(artifactList)}")

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

            pipelineBuildDao.finishBuild(
                dslContext,
                buildId,
                if (BuildStatus.isFinish(status)) status else BuildStatus.FAILED,
                JsonUtil.toJson(materials),
                JsonUtil.toJson(artifactList),
                executeTime,
                JsonUtil.toJson(buildParameters),
                recommendVersion
            )
            val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, latestRunningBuild.buildId) ?: return
            pipelineEventDispatcher.dispatch(
                PipelineStatusChangeEvent(
                    source = "pipelineStatusChangeEvent",
                    pipelineId = latestRunningBuild.pipelineId,
                    changeType = ChangeType.STATUS,
                    buildId = latestRunningBuild.buildId,
                    projectId = pipelineBuildInfo.projectId,
                    userId = latestRunningBuild.userId
                ),
                PipelineStatusChangeEvent(
                    source = "pipelineHistoryChangeEvent",
                    pipelineId = latestRunningBuild.pipelineId,
                    changeType = ChangeType.HISTORY,
                    buildId = latestRunningBuild.buildId,
                    projectId = pipelineBuildInfo.projectId,
                    userId = latestRunningBuild.userId
                ),
                PipelineStatusChangeEvent(
                    source = "pipelineDetailChangeEvent",
                    pipelineId = latestRunningBuild.pipelineId,
                    changeType = ChangeType.DETAIL,
                    buildId = latestRunningBuild.buildId,
                    projectId = pipelineBuildInfo.projectId,
                    userId = latestRunningBuild.userId
                )
            )
            logger.info("[$pipelineId]|finishLatestRunningBuild-$buildId|status=$status")
        }
    }

    private fun getRecommendVersion(buildParameters: List<BuildParameters>): String? {
        val majorVersion = if (!buildParameters.none { it.key == MAJORVERSION }) {
            buildParameters.filter { it.key == MAJORVERSION }[0].value.toString()
        } else return null

        val minorVersion = if (!buildParameters.none { it.key == MINORVERSION }) {
            buildParameters.filter { it.key == MINORVERSION }[0].value.toString()
        } else return null

        val fixVersion = if (!buildParameters.none { it.key == FIXVERSION }) {
            buildParameters.filter { it.key == FIXVERSION }[0].value.toString()
        } else return null

        val buildNo = if (!buildParameters.none { it.key == BUILD_NO }) {
            buildParameters.filter { it.key == BUILD_NO }[0].value.toString()
        } else return null

        return "$majorVersion.$minorVersion.$fixVersion.$buildNo"
    }

    private fun getBuildParameters(
        buildId: String
    ): List<BuildParameters> {
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

    private fun getExecuteTime(pipelineId: String, buildId: String): Long {
        val executeTask = pipelineBuildTaskDao.getByBuildId(dslContext, buildId)
            .filter { it.taskType != ManualReviewUserTaskElement.classType }
        var executeTime = 0L
        executeTask.forEach {
            executeTime += it.totalTime ?: 0
        }
        return executeTime
    }

    private fun getArtifactList(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<FileInfo> {
        val fileInfoList = mutableListOf<FileInfo>()
        val propertyList = mutableListOf<Property>()
        propertyList.add(Property("pipelineId", pipelineId))
        propertyList.add(Property("buildId", buildId))
        fileInfoList.addAll(
            client.get(ServiceArtifactoryResource::class).searchFile(
                userId = userId,
                projectCode = projectId,
                page = 1,
                pageSize = null,
                searchProps = SearchProps(props = mapOf("pipelineId" to pipelineId, "buildId" to buildId))
            ).data!!.records
        )
        logger.info("ArtifactFileList size: ${fileInfoList.size}")
        return fileInfoList
    }

    private fun getPipelineBuildMaterial(buildId: String): List<PipelineBuildMaterial> {
        val urlMap = pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_URL)
        logger.info("urlMap: $urlMap")
        val branchNameMap =
            pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_BRANCHNAME)
        logger.info("branchNameMap: $branchNameMap")
        val aliasNameMap =
            pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_ALIASNAME)
        logger.info("aliasNameMap: $aliasNameMap")
        val commitIdMap =
            pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_NEW_COMMIT_ID)
        logger.info("commitIdMap: $commitIdMap")
        val commitCommentMap =
            pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_NEW_COMMIT_COMMENT)
        logger.info("commitCommentMap: $commitCommentMap")
        val commitTimesMap =
            pipelineBuildVarDao.getVarRecordsByKeyPrefix(dslContext, buildId, PIPELINE_MATERIAL_NEW_COMMIT_TIMES)
        logger.info("commitTimesMap: $commitTimesMap")

        val repoIds = mutableListOf<String>()
        urlMap.map {
            val repoId = it.key.substringAfter(PIPELINE_MATERIAL_URL)
            repoIds.add(repoId)
        }
        logger.info("repoIds: $repoIds")

        val materialList = mutableListOf<PipelineBuildMaterial>()
        repoIds.forEach {
            materialList.add(
                PipelineBuildMaterial(
                    aliasNameMap["$PIPELINE_MATERIAL_ALIASNAME.$it"]?.value,
                    urlMap["$PIPELINE_MATERIAL_URL.$it"]!!.value,
                    branchNameMap["$PIPELINE_MATERIAL_BRANCHNAME.$it"]?.value,
                    commitIdMap["$PIPELINE_MATERIAL_NEW_COMMIT_ID.$it"]?.value,
                    commitCommentMap["$PIPELINE_MATERIAL_NEW_COMMIT_COMMENT.$it"]?.value,
                    commitTimesMap["$PIPELINE_MATERIAL_NEW_COMMIT_TIMES.$it"]?.value?.toInt()

                )
            )
        }

        return materialList
    }

    fun getNextQueueBuildInfo(pipelineId: String): BuildInfo? {
        return pipelineBuildDao.convert(pipelineBuildDao.getOneQueueBuild(dslContext, pipelineId))
    }

    fun getLastTimeBuild(pipelineId: String): BuildInfo? {
        return pipelineBuildDao.convert(pipelineBuildDao.getLatestBuild(dslContext, pipelineId))
    }

    fun updateTaskSubBuildId(buildId: String, taskId: String, subBuildId: String) {
        pipelineBuildTaskDao.updateSubBuildId(dslContext, buildId, taskId, subBuildId)
    }

    fun updateTaskStatus(buildId: String, taskId: String, userId: String, buildStatus: BuildStatus) {
        val task = getBuildTask(buildId, taskId)
        if (task != null) {
            updateTaskStatus(buildId, task, userId, buildStatus)
        }
    }

    private fun updateTaskStatus(buildId: String, task: PipelineBuildTask, userId: String, buildStatus: BuildStatus) {
        val buildInfo = getBuildInfo(buildId) ?: return
        val latestRunningBuild = LatestRunningBuild(
            projectId = buildInfo.projectId,
            pipelineId = task.pipelineId,
            buildId = buildId,
            userId = userId,
            status = buildStatus,
            taskCount = 0,
            currentTaskId = task.taskId,
            currentTaskName = task.taskName,
            buildNum = buildInfo.buildNum
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            logger.info("$buildId|${task.taskName} update status, status: ${buildStatus.name}, userId: $userId")
            pipelineBuildTaskDao.updateStatus(transactionContext, buildId, task.taskId, userId, buildStatus)
            pipelineBuildSummaryDao.updateCurrentBuildTask(
                transactionContext,
                latestRunningBuild
            )
        }
        pipelineEventDispatcher.dispatch(
            PipelineStatusChangeEvent(
                source = "pipelineStatusChangeEvent",
                pipelineId = latestRunningBuild.pipelineId,
                changeType = ChangeType.STATUS,
                buildId = latestRunningBuild.buildId,
                projectId = task.projectId,
                userId = latestRunningBuild.userId
            )
        )
    }

    fun getPipelineBuildHistoryCount(projectId: String, pipelineId: String): Int {
        return pipelineBuildDao.count(dslContext, projectId, pipelineId)
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
        remark: String?
    ): Int {
        return pipelineBuildDao.count(
            dslContext,
            projectId,
            pipelineId,
            materialAlias,
            materialUrl,
            materialBranch,
            materialCommitId,
            materialCommitMessage,
            status,
            trigger,
            queueTimeStartTime,
            queueTimeEndTime,
            startTimeStartTime,
            startTimeEndTime,
            endTimeStartTime,
            endTimeEndTime,
            totalTimeMin,
            totalTimeMax,
            remark
        )
    }

    fun getAllBuildNum(projectId: String, pipelineId: String): Collection<Int> {
        return pipelineBuildDao.listPipelineBuildNum(dslContext, projectId, pipelineId, 0, Int.MAX_VALUE)
    }

    // 性能点
    fun totalRunningBuildCount(): Int {
        return pipelineBuildDao.countAllByStatus(dslContext, BuildStatus.RUNNING)
    }

    // 获取流水线最后的构建号
    fun getLatestFinishedBuildId(pipelineId: String): String? {
        val buildHistory = pipelineBuildDao.getLatestFinishedBuild(dslContext, pipelineId)
        return when (buildHistory) {
            null -> null
            else -> buildHistory.buildId
        }
    }

    fun getBuildIdbyBuildNo(projectId: String, pipelineId: String, buildNo: Int): String? {
        return pipelineBuildDao.getBuildByBuildNo(dslContext, projectId, pipelineId, buildNo)?.buildId
    }
}
