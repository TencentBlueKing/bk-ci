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

package com.tencent.devops.process.engine.service.record

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildEndInfo
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.EndPosition
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TapdWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EVENT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_WAREHOUSE_EVENTS
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateBuildTimeCost
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceDraftVersionDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.service.PipelineArtifactQualityService
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.task.PipelineFailTaskDetail
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Suppress(
    "LongParameterList",
    "ComplexMethod",
    "ReturnCount",
    "NestedBlockDepth",
    "LongMethod"
)
@Service
class PipelineBuildRecordService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    private val dslContext: DSLContext,
    private val recordModelDao: BuildRecordModelDao,
    private val recordStageDao: BuildRecordStageDao,
    private val recordContainerDao: BuildRecordContainerDao,
    private val recordTaskDao: BuildRecordTaskDao,
    private val buildDetailDao: BuildDetailDao,
    private val client: Client,
    private val pipelineInfoService: PipelineInfoService,
    private val pipelineArtifactQualityService: PipelineArtifactQualityService,
    recordModelService: PipelineRecordModelService,
    pipelineResourceDao: PipelineResourceDao,
    pipelineBuildDao: PipelineBuildDao,
    pipelineResourceVersionDao: PipelineResourceVersionDao,
    redisOperation: RedisOperation,
    stageTagService: StageTagService,
    pipelineEventDispatcher: PipelineEventDispatcher,
    pipelineResourceDraftVersionDao: PipelineResourceDraftVersionDao
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = recordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation,
    recordModelService = recordModelService,
    pipelineResourceDao = pipelineResourceDao,
    pipelineBuildDao = pipelineBuildDao,
    pipelineResourceVersionDao = pipelineResourceVersionDao,
    pipelineResourceDraftVersionDao = pipelineResourceDraftVersionDao
) {

    @Value("\${pipeline.build.retry.limit_days:21}")
    private var retryLimitDays: Int = 0

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildRecordService::class.java)!!
    }

    fun batchSave(
        transactionContext: DSLContext?,
        model: BuildRecordModel?,
        stageList: List<BuildRecordStage>?,
        containerList: List<BuildRecordContainer>?,
        taskList: List<BuildRecordTask>?
    ) {
        val dsl = transactionContext ?: dslContext
        model?.let { recordModelDao.createRecord(dsl, model) }
        stageList?.let { recordStageDao.batchSave(dsl, stageList) }
        containerList?.let { recordContainerDao.batchSave(dsl, containerList) }
        taskList?.let { recordTaskDao.batchSave(dsl, taskList) }
    }

    private fun checkPassDays(startTime: Long?): Boolean {
        if (retryLimitDays < 0 || startTime == null) {
            return true
        }
        return (System.currentTimeMillis() - startTime) < TimeUnit.DAYS.toMillis(retryLimitDays.toLong())
    }

    /**
     * 查询ModelRecord
     * @param projectId: 项目Id
     * @param pipelineId: 流水线Id
     * @param buildId: 构建Id
     * @param refreshStatus: 是否刷新状态
     * @param executeCount: 执行次数
     * @param archiveFlag: 归档标识
     */
    fun getBuildRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        refreshStatus: Boolean = true,
        executeCount: Int? = null,
        encryptedFlag: Boolean? = false,
        archiveFlag: Boolean? = false
    ): ModelRecord? {
        val queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        val buildInfo = pipelineBuildDao.getBuildInfo(
            dslContext = queryDslContext,
            projectId = projectId,
            buildId = buildId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        if (projectId != buildInfo.projectId || pipelineId != buildInfo.pipelineId) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                params = arrayOf(pipelineId)
            )
        }
        return getBuildRecord(
            buildInfo = buildInfo,
            executeCount = executeCount,
            refreshStatus = refreshStatus,
            encryptedFlag = encryptedFlag,
            archiveFlag = archiveFlag
        )
    }

    /**
     * 查询ModelRecord
     * @param buildInfo: 构建信息
     * @param executeCount: 查询的执行次数
     * @param refreshStatus: 是否刷新状态
     */
    fun getBuildRecord(
        userId: String? = null,
        buildInfo: BuildInfo,
        executeCount: Int?,
        refreshStatus: Boolean = true,
        encryptedFlag: Boolean? = false,
        archiveFlag: Boolean? = false
    ): ModelRecord? {
        // 直接取构建记录数据，防止接口传错
        val projectId = buildInfo.projectId
        val pipelineId = buildInfo.pipelineId
        val buildId = buildInfo.buildId
        logger.info("[$$buildId|$projectId|QUERY_BUILD_RECORD|$refreshStatus|executeCount=$executeCount")
        val watcher = Watcher(id = "getBuildRecord#$buildId")

        // 如果请求的次数为空或者为负数则填补为最新的次数，旧数据直接按第一次查询
        var fixedExecuteCount = fixedExecuteCount(
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            buildInfo = buildInfo
        )
        watcher.start("buildRecordModel")
        val queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        val buildRecordModel = recordModelDao.getRecord(
            dslContext = queryDslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = fixedExecuteCount
        )
        watcher.start("genRecordModel")
        val version = buildInfo.version
        val model = if (buildRecordModel != null) {
            val record = getRecordModel(
                projectId = projectId, pipelineId = pipelineId,
                version = version, buildId = buildId,
                fixedExecuteCount = fixedExecuteCount,
                buildRecordModel = buildRecordModel,
                queryDslContext = queryDslContext,
                debug = buildInfo.debug
            )
            if (record == null) fixedExecuteCount = buildInfo.executeCount
            record
        } else {
            null
        } ?: run {
            logger.warn(
                "RECORD|turn to detail($buildId)|executeCount=$executeCount|" +
                    "fixedExecuteCount=$fixedExecuteCount"
            )
            watcher.start("getDetailModel")
            val record = buildDetailDao.get(queryDslContext, projectId, buildId) ?: return null
            val detail = JsonUtil.to(record.model, Model::class.java)
            fixDetailTimeCost(buildInfo, detail)
            detail
        }
        watcher.start("getPipelineInfo")
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId, pipelineId = buildInfo.pipelineId, queryDslContext = queryDslContext
        ) ?: return null

        val buildSummaryRecord = pipelineBuildSummaryDao.get(
            dslContext = queryDslContext,
            projectId = projectId,
            pipelineId = buildInfo.pipelineId
        )
        val elementSensitiveParamInfos = if (encryptedFlag == true) {
            AtomUtils.getModelElementSensitiveParamInfos(projectId, model, client)
        } else {
            null
        }
        // 判断需要刷新状态，目前只会改变canRetry & canSkip 状态
        // #7983 仅当查看最新一次执行记录时可以选择重试
        if (refreshStatus && fixedExecuteCount == buildInfo.executeCount && archiveFlag != true) {
            // #4245 仅当在有限时间内并已经失败或者取消(终态)的构建上可尝试重试或跳过
            // #6400 无需流水线是终态就可以进行task重试
            if (checkPassDays(buildInfo.startTime)) {
                ModelUtils.refreshCanRetry(model)
            }
        }
        watcher.start("fixModel")
        val triggerContainer = model.getTriggerContainer()
        triggerContainer.buildNo?.apply {
            currentBuildNo = if (buildInfo.debug) {
                buildSummaryRecord?.debugBuildNo
            } else {
                buildSummaryRecord?.buildNo
            } ?: buildNo
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
                fixContainerDetail(container)
                container.fetchGroupContainers()?.forEach { groupContainer ->
                    fixContainerDetail(groupContainer)
                }
                elementSensitiveParamInfos?.let {
                    container.elements.forEach { e ->
                        pipelineInfoService.transferSensitiveParam(e, elementSensitiveParamInfos)
                    }
                }
            }
            stage.elapsed = stage.elapsed ?: stage.timeCost?.totalCost
        }
        val triggerReviewers = pipelineTriggerReviewDao.getTriggerReviewers(
            dslContext = queryDslContext,
            projectId = projectId,
            pipelineId = pipelineInfo.pipelineId,
            buildId = buildId
        )
        watcher.start("startUserList")

        val recordList = getRecordInfo(
            pipelineId = pipelineInfo.pipelineId,
            projectId = projectId,
            buildId = buildId,
            queryDslContext = queryDslContext
        )
        watcher.start("parseTriggerInfo")
        // TODO 临时解析旧触发器获取实际触发信息，后续触发器完善需要改回
        val triggerInfo = if (buildInfo.trigger == StartType.WEB_HOOK.name) {
            triggerContainer.elements.find { it.status == BuildStatus.SUCCEED.name }?.let {
                when (it) {
                    is CodeGitWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("Git")
                        )
                    }
                    is CodeTGitWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("Git")
                        )
                    }
                    is CodeGithubWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("GitHub")
                        )
                    }
                    is CodeGitlabWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("Gitlab")
                        )
                    }
                    is CodeP4WebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("P4")
                        )
                    }
                    is CodeSVNWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("SVN")
                        )
                    }
                    is TapdWebHookTriggerElement -> {
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_EVENT,
                            params = arrayOf("TAPD")
                        )
                    }
                    else -> null
                }
            } ?: I18nUtil.getCodeLanMessage(messageCode = BK_WAREHOUSE_EVENTS)
        } else {
            StartType.toReadableString(
                buildInfo.trigger,
                buildInfo.channelCode,
                I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val queueTime = buildRecordModel?.queueTime?.timestampmilli() ?: buildInfo.queueTime
        val startTime = buildRecordModel?.startTime?.timestampmilli()
        val endTime = buildRecordModel?.endTime?.timestampmilli()
        val queueTimeCost = startTime?.let { it - queueTime } ?: endTime?.let { it - queueTime }
        val versionChange = run {
            if (buildInfo.buildNum == 1 || buildInfo.versionChange == false || buildInfo.debug) {
                return@run false // 返回 run 块的结果
            }

            if (buildInfo.versionChange == true) {
                return@run true
            }

            val prevBuildInfo = pipelineBuildDao.getBuildByBuildNum(
                dslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT),
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = buildInfo.buildNum - 1,
                debugVersion = null
            )

            prevBuildInfo != null && prevBuildInfo.version != buildInfo.version
        }
        LogUtils.printCostTimeWE(watcher)
        // 构建运行总时长：从构建开始到结束，未结束时按当前时刻计算；排队中未启动的构建为空
        // 需在apply块外计算，避免块内endTime被BuildEndInfo.endTime属性遮蔽
        val buildRunCostTime = startTime?.let { (endTime ?: System.currentTimeMillis()) - it }
        // 合成终态必须与页面状态标签同源取记录表状态：详情记录先落终态、构建历史表随后才更新，
        // 若用滞后的 buildInfo.status，构建结束瞬间推送的详情会因状态还是运行中而合成不出来（#13477）
        val recordStatus = buildRecordModel?.status?.let { BuildStatus.parse(it) } ?: buildInfo.status
        val buildEndInfo = (
            buildRecordModel?.modelVar?.get(BuildEndInfo.MODEL_VAR_KEY)?.let {
                JsonUtil.anyToOrNull(it, object : TypeReference<BuildEndInfo>() {})
            } ?: synthesizeSuccessEndInfo(recordStatus, model, endTime)
            )?.apply {
            totalCostTime = buildRunCostTime
            // 与 endType 恒定同类；写入侧已保证 endType 的大类与构建最终状态同类
            endCategory = endType.category
            reasonCode?.let { code ->
                reason = I18nUtil.getCodeLanMessage(
                    messageCode = code,
                    params = reasonParams?.toTypedArray(),
                    defaultMessage = reason
                )
            }
            endTypeDesc = translateEndType(endType)
            positions?.forEach { pos ->
                val status = BuildStatus.parse(pos.statusAtEnd)
                pos.statusAtEndDesc = I18nUtil.getCodeLanMessage(
                    messageCode = "buildStatus.${status.statusName}",
                    defaultMessage = pos.statusAtEnd
                )
                pos.endTypeDesc = pos.endType?.let { translateEndType(it) }
                pos.reasonCode?.let { code ->
                    pos.reason = I18nUtil.getCodeLanMessage(
                        messageCode = code,
                        params = pos.reasonParams?.toTypedArray(),
                        defaultMessage = pos.reason
                    )
                }
            }
        }
        return ModelRecord(
            id = buildInfo.buildId,
            pipelineId = buildInfo.pipelineId,
            pipelineName = model.name,
            userId = buildInfo.startUser,
            triggerUser = buildInfo.triggerUser,
            trigger = triggerInfo,
            queueTime = queueTime,
            startTime = startTime,
            queueTimeCost = queueTimeCost,
            endTime = endTime,
            status = buildRecordModel?.status ?: buildInfo.status.name,
            model = model,
            currentTimestamp = System.currentTimeMillis(),
            buildNum = buildInfo.buildNum,
            cancelUserId = buildRecordModel?.cancelUser,
            curVersion = buildInfo.version,
            curVersionName = buildInfo.versionName,
            latestVersion = pipelineInfo.version,
            latestBuildNum = if (buildInfo.debug) {
                buildSummaryRecord?.debugBuildNum
            } else {
                buildSummaryRecord?.buildNum
            } ?: -1,
            lastModifyUser = pipelineInfo.lastModifyUser,
            executeTime = buildInfo.executeTime, // 只为兼容接口，该字段不准确
            errorInfoList = buildRecordModel?.errorInfoList,
            stageStatus = buildInfo.stageStatus,
            triggerReviewers = triggerReviewers,
            executeCount = fixedExecuteCount,
            startUserList = recordList.map { it.startUser },
            buildMsg = BuildMsgUtils.getBuildMsg(
                buildMsg = buildInfo.buildMsg,
                startType = StartType.toStartType(buildInfo.trigger),
                channelCode = buildInfo.channelCode
            ),
            material = buildInfo.material,
            remark = buildInfo.remark,
            debug = buildInfo.debug,
            webhookInfo = buildInfo.webhookInfo,
            templateInfo = pipelineInfo.templateInfo,
            recordList = recordList,
            artifactQuality = pipelineArtifactQualityService.buildArtifactQuality(
                userId = userId,
                projectId = projectId,
                artifactQualityList = buildInfo.artifactQualityList
            ),
            versionChange = versionChange,
            buildEndInfo = buildEndInfo
        )
    }

    /**
     * 普通成功的构建不在结束时落库终态详情，避免每次构建成功都额外写一次记录表；
     * 读取时按构建状态合成，保证前端对所有终态都能拿到统一结构。
     * 阶段准入被驳回等有额外信息的成功场景已在构建结束时落库，不会走到这里。
     */
    private fun synthesizeSuccessEndInfo(status: BuildStatus, model: Model, buildEndTime: Long?): BuildEndInfo? {
        // 阶段准入等待审核时构建并未结束，只是挂起为阶段成功，需与真正的阶段成功区分开。
        // 挂起是先写审核记录、后改构建状态（见 PipelineStageService.pauseStage），推送恰好赶在
        // 状态改写前时状态还是运行中，因此只要构建未结束就以模型里的阶段审核态为准，不依赖状态判定
        if (!status.isFinish()) {
            synthesizeStageReviewingEndInfo(model)?.let { return it }
        }
        return if (status == BuildStatus.STAGE_SUCCESS || status.isSuccess()) {
            successEndInfo(buildEndTime)
        } else {
            null
        }
    }

    /**
     * 合成而非落库，因此终态时间必须取构建自身的结束时间，
     * 不能用工厂方法里的当前时刻——否则每次读取都会算出一个不同的「结束时间」。
     */
    private fun successEndInfo(buildEndTime: Long?) = BuildEndInfo(
        endType = BuildEndType.SUCCESS,
        endTime = buildEndTime
    )

    /**
     * 合成「阶段准入审核中」终态详情：构建挂起在审核环节时不会走构建结束流程，
     * 无从在写入侧落库，因此读取时按阶段的审核状态还原出审核位置与已等待时长。
     *
     * 详情页不展示触发器阶段（Model 中的第一个 Stage），位置编码从 1 开始，与失败位置口径一致。
     */
    private fun synthesizeStageReviewingEndInfo(model: Model): BuildEndInfo? {
        model.stages.forEachIndexed { stageIndex, stage ->
            if (stageIndex == 0) return@forEachIndexed
            val stageId = stage.id ?: return@forEachIndexed
            val check = listOfNotNull(stage.checkIn, stage.checkOut)
                .firstOrNull { it.status == BuildStatus.REVIEWING.name } ?: return@forEachIndexed
            val group = check.groupToReview() ?: return@forEachIndexed
            val groupSeq = check.reviewGroups?.indexOf(group)?.takeIf { it >= 0 }?.plus(1)
            val position = EndPosition(
                position = "$stageIndex",
                componentPath = stage.name.orEmpty(),
                statusAtEnd = BuildStatus.REVIEWING.name,
                endType = BuildEndType.SUCCESS_STAGE_REVIEWING,
                stageId = stageId,
                containerId = "",
                reviewGroupSeq = groupSeq,
                reviewGroupName = group.name,
                reviewers = group.reviewers.takeIf { it.isNotEmpty() }
            )
            return BuildEndInfo(
                endType = BuildEndType.SUCCESS_STAGE_REVIEWING,
                // 构建挂起在审核环节，尚无结束时间；已等待时长自阶段暂停进入审核（stagePause 写入 startEpoch）起算
                waitCostTime = stage.startEpoch?.let { System.currentTimeMillis() - it }
            ).withPositions(listOf(position))
        }
        return null
    }

    private fun translateEndType(endType: BuildEndType): String = I18nUtil.getCodeLanMessage(
        messageCode = "buildEndType.${endType.displayName}",
        defaultMessage = endType.displayName
    )

    fun getRecordInfo(pipelineId: String, projectId: String, buildId: String, queryDslContext: DSLContext? = null) =
        recordModelDao.getRecordInfoList(
            dslContext = queryDslContext ?: dslContext,
            pipelineId = pipelineId,
            projectId = projectId,
            buildId = buildId
        )

    private fun fixContainerDetail(container: Container) {
        container.containerHashId = container.containerHashId ?: container.containerId
        container.containerId = container.id
        var elementElapsed = 0L
        container.elements.forEach { element ->
            element.timeCost?.executeCost?.let {
                element.elapsed = it
                elementElapsed += it
            }
            element.additionalOptions?.let {
                if (it.timeoutVar.isNullOrBlank()) it.timeoutVar = it.timeout.toString()
            }
        }
        if (container is NormalContainer) {
            container.jobControlOption?.let {
                if (it.timeoutVar.isNullOrBlank()) it.timeoutVar = it.timeout.toString()
            }
            container.mutexGroup?.let {
                if (it.timeoutVar.isNullOrBlank()) it.timeoutVar = it.timeout.toString()
            }
        } else if (container is VMBuildContainer) {
            container.jobControlOption?.let {
                if (it.timeoutVar.isNullOrBlank()) it.timeoutVar = it.timeout.toString()
            }
            container.mutexGroup?.let {
                if (it.timeoutVar.isNullOrBlank()) it.timeoutVar = it.timeout.toString()
            }
        }
        container.elementElapsed = container.elementElapsed ?: elementElapsed
        container.systemElapsed = container.systemElapsed ?: container.timeCost?.systemCost
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
        detail.stages.forEach nextStage@{ stage ->
            if (!hasTimeCost(stage.status)) return@nextStage
            stage.containers.forEach nextContainer@{ container ->
                if (!hasTimeCost(container.status)) return@nextContainer
                container.timeCost = BuildRecordTimeCost(
                    systemCost = container.systemElapsed ?: 0,
                    executeCost = container.elementElapsed ?: 0,
                    totalCost = (container.systemElapsed ?: 0) + (container.elementElapsed ?: 0)
                )
                container.elements.forEach nextElement@{ element ->
                    if (!hasTimeCost(element.status)) return@nextElement
                    element.timeCost = BuildRecordTimeCost(
                        executeCost = element.elapsed ?: 0,
                        totalCost = element.elapsed ?: 0
                    )
                }
            }
        }
    }

    private fun hasTimeCost(status: String?) =
        BuildStatus.parse(status).isFinish() &&
            !BuildStatus.parse(status).isSkip()

    fun buildCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: BuildStatus,
        cancelUser: String,
        executeCount: Int,
        buildEndInfo: BuildEndInfo? = null
    ) {
        logger.info("[$buildId]|BUILD_CANCEL|cancelUser=$cancelUser|buildStatus=$buildStatus")
        var startUser: String? = null
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
            val runningStatusSet = enumValues<BuildStatus>().filter { it.isRunning() }.toSet()
            val recordStages = recordStageDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount
            )
            // 第1层循环：刷新运行中stage状态
            recordStages.forEach nextStage@{ stage ->
                if (!BuildStatus.parse(stage.status).isRunning()) return@nextStage
                stage.status = buildStatus.name
                // 第2层循环：刷新stage下运行中的container状态（包括矩阵）
                val recordContainers = recordContainerDao.getRecords(
                    dslContext = context, projectId = projectId,
                    pipelineId = pipelineId, buildId = buildId,
                    executeCount = executeCount, stageId = stage.stageId,
                    matrixGroupId = null, buildStatusSet = runningStatusSet
                )
                recordContainers.forEach nextContainer@{ container ->
                    val status = BuildStatus.parse(container.status)
                    val recordTasks = recordTaskDao.getRecords(
                        dslContext = context, projectId = projectId, pipelineId = pipelineId,
                        buildId = buildId, executeCount = executeCount, containerId = container.containerId
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
                    }
                    container.status = buildStatus.name
                    // 第3层循环：刷新container下运行中的task状态
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

            val modelVar = mutableMapOf<String, Any>()
            modelVar[Model::timeCost.name] = recordModel.generateBuildTimeCost(recordStages)
            buildEndInfo?.let { modelVar[BuildEndInfo.MODEL_VAR_KEY] = it }
            recordModelDao.updateRecord(
                context, projectId, pipelineId, buildId, executeCount, buildStatus,
                recordModel.modelVar.plus(modelVar), null, LocalDateTime.now(),
                null, cancelUser, null
            )
            startUser = recordModel.startUser
        }
        // 事务提交后再推送，否则推送侧回查可能读到尚未提交的运行中状态
        startUser?.let {
            pipelineRecordChangeEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                startUser = it,
                executeCount = executeCount
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
    ): Triple<Model, List<BuildStageStatus>, BuildRecordTimeCost?> {
        logger.info("[$buildId]|BUILD_END|buildStatus=$buildStatus")
        var timeCost: BuildRecordTimeCost? = null
        val recordModel = recordModelDao.getRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        ) ?: run {
            logger.warn(
                "ENGINE|$buildId|buildEnd| get model($buildId) record failed."
            )
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        }
        var allStageStatus: List<BuildStageStatus> = emptyList()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val now = LocalDateTime.now()
            val runningStatusSet = enumValues<BuildStatus>().filter { it.isRunning() }.toSet()
            // 刷新运行中stage状态，取出所有stage记录还需用于耗时计算
            val recordStages = recordStageDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount
            )
            recordStages.forEach nextStage@{ stage ->
                if (!BuildStatus.parse(stage.status).isRunning()) return@nextStage
                stage.status = buildStatus.name
                if (stage.endTime == null) { stage.endTime = now }
            }
            // 刷新运行中的container状态
            val recordContainers = recordContainerDao.getRecords(
                dslContext = context, projectId = projectId,
                pipelineId = pipelineId, buildId = buildId,
                executeCount = executeCount, stageId = null,
                matrixGroupId = null, buildStatusSet = runningStatusSet
            )
            recordContainers.forEach nextContainer@{ container ->
                container.status = buildStatus.name
                if (container.endTime == null) { container.endTime = now }
                val containerName = container.containerVar[Container::name.name] as String?
                if (!containerName.isNullOrBlank()) {
                    container.containerVar[Container::name.name] =
                        ContainerUtils.getClearedQueueContainerName(containerName)
                }
            }
            // 刷新运行中的task状态
            val recordTasks = recordTaskDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount, null, runningStatusSet
            )
            recordTasks.forEach nextTask@{ task ->
                task.status = buildStatus.name
                if (task.endTime == null) { task.endTime = now }
            }
            recordTaskDao.batchSave(context, recordTasks)
            recordContainerDao.batchSave(context, recordContainers)
            recordStageDao.batchSave(context, recordStages)

            val modelVar = mutableMapOf<String, Any>()
            timeCost = recordModel.generateBuildTimeCost(recordStages)
            timeCost?.let { modelVar[Model::timeCost.name] = it }
            recordModelDao.updateRecord(
                context, projectId, pipelineId, buildId, executeCount, buildStatus,
                recordModel.modelVar.plus(modelVar), null, LocalDateTime.now(),
                errorInfoList, null, null
            )
            val allRecordStages = recordStageDao.getLatestRecords(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount
            )
            allStageStatus = fetchHistoryStageStatus(
                recordStages = allRecordStages, buildStatus = buildStatus, errorMsg = errorMsg
            )
        }
        // 事务提交后再推送，否则推送侧回查可能读到尚未提交的运行中状态
        pipelineRecordChangeEvent(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            startUser = recordModel.startUser,
            executeCount = executeCount
        )
        val model = getRecordModel(
            projectId = projectId,
            pipelineId = pipelineId,
            version = recordModel.resourceVersion,
            buildId = buildId,
            executeCount = executeCount
        ) ?: throw ErrorCodeException(
            statusCode = jakarta.ws.rs.core.Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        return Triple(model, allStageStatus, timeCost)
    }

    fun updateBuildCancelUser(
        projectId: String,
        buildId: String,
        executeCount: Int,
        cancelUserId: String
    ) {
        recordModelDao.updateBuildCancelUser(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            cancelUser = cancelUserId
        )
    }

    fun saveBuildEndInfo(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildEndInfo: BuildEndInfo
    ) {
        var startUser: String? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount
            ) ?: run {
                logger.warn("ENGINE|$buildId|saveBuildEndInfo| get record failed.")
                return@transaction
            }
            val modelVar = recordModel.modelVar.toMutableMap()
            modelVar[BuildEndInfo.MODEL_VAR_KEY] = buildEndInfo
            recordModelDao.updateRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount, cancelUser = null,
                modelVar = modelVar, buildStatus = null,
                startTime = null, endTime = null, errorInfoList = null,
                timestamps = null
            )
            startUser = recordModel.startUser
        }
        notifyBuildEndInfoSaved(projectId, pipelineId, buildId, executeCount, startUser)
    }

    /**
     * 清除已落库的终态详情，供运行中重试复用同一执行次数的记录行时调用。
     * 不派发记录变更推送——调用方在重试流程中随后就会刷新记录并推送。
     */
    fun clearBuildEndInfo(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ) {
        val context = transactionContext ?: dslContext
        val recordModel = recordModelDao.getRecord(
            dslContext = context, projectId = projectId, pipelineId = pipelineId,
            buildId = buildId, executeCount = executeCount
        ) ?: return
        if (!recordModel.modelVar.containsKey(BuildEndInfo.MODEL_VAR_KEY)) return
        recordModelDao.updateRecord(
            dslContext = context, projectId = projectId, pipelineId = pipelineId,
            buildId = buildId, executeCount = executeCount, cancelUser = null,
            modelVar = recordModel.modelVar.minus(BuildEndInfo.MODEL_VAR_KEY), buildStatus = null,
            startTime = null, endTime = null, errorInfoList = null,
            timestamps = null
        )
    }

    /**
     * 仅在buildEndInfo尚未存在时保存，避免覆盖用户主动取消等高优先级信息。
     * 适用于心跳超时、Job执行超时等系统级场景。
     */
    fun saveBuildEndInfoIfAbsent(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildEndInfo: BuildEndInfo
    ) {
        var startUser: String? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount
            ) ?: run {
                logger.warn("ENGINE|$buildId|saveBuildEndInfoIfAbsent| get record failed.")
                return@transaction
            }
            if (recordModel.modelVar.containsKey(BuildEndInfo.MODEL_VAR_KEY)) {
                return@transaction
            }
            val modelVar = recordModel.modelVar.toMutableMap()
            modelVar[BuildEndInfo.MODEL_VAR_KEY] = buildEndInfo
            recordModelDao.updateRecord(
                dslContext = context, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount, cancelUser = null,
                modelVar = modelVar, buildStatus = null,
                startTime = null, endTime = null, errorInfoList = null,
                timestamps = null
            )
            startUser = recordModel.startUser
        }
        notifyBuildEndInfoSaved(projectId, pipelineId, buildId, executeCount, startUser)
    }

    /**
     * 终态详情是在构建结束的记录更新之后才落库的，此时前端已收到上一次记录变更推送并拉走了
     * 不含终态详情的数据。因此保存成功后需再推送一次，否则页面要等用户手动刷新才能看到详情。
     * 事务提交后再派发，避免推送侧回查到旧数据。
     */
    private fun notifyBuildEndInfoSaved(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        startUser: String?
    ) {
        if (startUser == null) return
        pipelineRecordChangeEvent(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            startUser = startUser,
            executeCount = executeCount
        )
    }

    fun getBuildCancelUser(
        projectId: String,
        buildId: String,
        executeCount: Int
    ): String? {
        return recordModelDao.getBuildCancelUser(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount
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

    fun getPipelineIdByBuildId(projectId: String, buildId: String): String? {
        return pipelineBuildDao.getBuildInfo(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId
        )?.pipelineId
    }

    fun getBuildFailedTasks(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?
    ): List<PipelineFailTaskDetail> {
        val buildRecord = getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount
        )
        val errorInfoList = buildRecord?.errorInfoList?.takeIf { it.isNotEmpty() } ?: return listOf()
        val stageIds = errorInfoList.mapNotNull { it.stageId }.toSet()
        val containerIds = errorInfoList.mapNotNull { it.containerId }.toSet()
        val stageNameMap = mutableMapOf<String, String>()
        val containerNameMap = mutableMapOf<String, String>()
        buildRecord.model.stages.forEach stageForeach@{ stage ->
            val stageId = stage.id?.takeIf { it in stageIds } ?: return@stageForeach
            stageNameMap[stageId] = stage.name ?: ""
            stage.containers.forEach containerForeach@{ container ->
                val containerId = container.id?.takeIf { it in containerIds } ?: return@containerForeach
                containerNameMap[containerId] = container.name ?: ""
            }
        }
        return errorInfoList.map {
            PipelineFailTaskDetail(
                stepId = it.stageId ?: "",
                taskId = it.taskId,
                taskName = it.taskName,
                jobId = it.containerId ?: "",
                jobName = containerNameMap[it.containerId],
                stageName = stageNameMap[it.stageId],
                errorMsg = it.errorMsg,
                matrixFlag = it.matrixFlag ?: false
            )
        }
    }
}
