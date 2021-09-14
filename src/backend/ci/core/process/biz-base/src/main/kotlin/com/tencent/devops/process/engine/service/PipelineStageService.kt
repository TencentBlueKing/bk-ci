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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.common.BS_STAGE_CANCELED_END_SOURCE
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.detail.StageBuildDetailService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * 流水线Stage相关的服务
 * @version 1.0
 */
@Service
@Suppress("TooManyFunctions", "LongParameterList")
class PipelineStageService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildStageDao: PipelineBuildStageDao,
    private val buildVariableService: BuildVariableService,
    private val stageBuildDetailService: StageBuildDetailService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineStageService::class.java)
    }

    fun getStage(buildId: String, stageId: String?): PipelineBuildStage? {
        val result = pipelineBuildStageDao.get(dslContext, buildId, stageId)
        if (result != null) {
            return pipelineBuildStageDao.convert(result)
        }
        return null
    }

    /**
     * 取构建[buildId]当前序号为[currentStageSeq]的下一个[PipelineBuildStage]
     * 如果不存在则返回null
     */
    fun getNextStage(buildId: String, currentStageSeq: Int): PipelineBuildStage? {
        val result = pipelineBuildStageDao.getNextStage(dslContext, buildId, currentStageSeq)
        if (result != null) {
            return pipelineBuildStageDao.convert(result)
        }
        return null
    }

    fun updateStageStatus(
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ) {
        logger.info("[$buildId]|updateStageStatus|status=$buildStatus|stageId=$stageId")
        pipelineBuildStageDao.updateStatus(
            dslContext = dslContext, buildId = buildId,
            stageId = stageId, buildStatus = buildStatus,
            checkIn = checkIn, checkOut = checkOut
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

    fun skipStage(userId: String, buildStage: PipelineBuildStage) {
        with(buildStage) {
            val allStageStatus = stageBuildDetailService.stageSkip(buildId = buildId, stageId = stageId)
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineBuildStageDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    stageId = stageId, buildStatus = BuildStatus.SKIP,
                    controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                )

                pipelineBuildDao.updateBuildStageStatus(
                    dslContext = context, buildId = buildId, stageStatus = allStageStatus
                )
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "skipStage", projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                )
            )
        }
    }

    fun checkQualityFailStage(userId: String, buildStage: PipelineBuildStage) {
        with(buildStage) {
            val allStageStatus = stageBuildDetailService.stageCheckQuality(
                buildId = buildId, stageId = stageId,
                controlOption = controlOption!!,
                buildStatus = BuildStatus.FAILED,
                checkIn = checkIn, checkOut = checkOut
            )
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineBuildStageDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    stageId = stageId, controlOption = controlOption!!,
                    buildStatus = BuildStatus.QUALITY_CHECK_FAIL,
                    checkIn = checkIn, checkOut = checkOut
                )
                pipelineBuildDao.updateBuildStageStatus(
                    dslContext = context, buildId = buildId, stageStatus = allStageStatus
                )
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "checkQualityFailStage", projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                )
            )
        }
    }

    fun checkQualityPassStage(userId: String, buildStage: PipelineBuildStage) {
        with(buildStage) {
            val allStageStatus = stageBuildDetailService.stageCheckQuality(
                buildId = buildId, stageId = stageId,
                controlOption = controlOption!!,
                buildStatus = BuildStatus.RUNNING,
                checkIn = checkIn, checkOut = checkOut
            )
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineBuildStageDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    stageId = stageId, controlOption = controlOption!!,
                    buildStatus = BuildStatus.QUALITY_CHECK_PASS,
                    checkIn = checkIn, checkOut = checkOut
                )
                pipelineBuildDao.updateBuildStageStatus(
                    dslContext = context, buildId = buildId, stageStatus = allStageStatus
                )
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "checkQualityPassStage", projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                )
            )
        }
    }

    fun pauseStage(buildStage: PipelineBuildStage) {
        with(buildStage) {
            checkIn?.status = BuildStatus.REVIEWING.name
            val allStageStatus = stageBuildDetailService.stagePause(
                buildId = buildId,
                stageId = stageId,
                controlOption = controlOption!!,
                checkIn = checkIn,
                checkOut = checkOut
            )
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineBuildStageDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    stageId = stageId, buildStatus = BuildStatus.PAUSE,
                    controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                )
                pipelineBuildDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    oldBuildStatus = BuildStatus.RUNNING, newBuildStatus = BuildStatus.STAGE_SUCCESS
                )
                pipelineBuildDao.updateBuildStageStatus(
                    dslContext = context, buildId = buildId, stageStatus = allStageStatus
                )
                // 被暂停的流水线不占构建队列，在执行数-1
                pipelineBuildSummaryDao.updateRunningCount(
                    dslContext = context, pipelineId = pipelineId, buildId = buildId, runningIncrement = -1
                )
            }

            // #3400 点Stage启动时处于DETAIL界面，以操作人视角，没有刷历史列表的必要
        }
    }

    fun startStage(
        userId: String,
        buildStage: PipelineBuildStage,
        reviewRequest: StageReviewRequest?
    ): Boolean {
        with(buildStage) {
            val success = checkIn?.reviewGroup(
                userId = userId, groupId = reviewRequest?.id,
                action = ManualReviewAction.PROCESS, params = reviewRequest?.reviewParams,
                suggest = reviewRequest?.suggest
            )
            if (success != true) return false
            stageBuildDetailService.stageReview(
                buildId = buildId, stageId = stageId,
                controlOption = controlOption!!,
                checkIn = checkIn, checkOut = checkOut
            )
            // #4531 stage先保持暂停，如果没有其他需要审核的审核组则可以启动stage，否则直接返回
            pipelineBuildStageDao.updateStatus(
                dslContext = dslContext, buildId = buildId,
                stageId = stageId, buildStatus = BuildStatus.PAUSE,
                controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
            )
            // 如果还有待审核的审核组，则直接通知并返回
            if (checkIn?.groupToReview() != null) {
                val variables = buildVariableService.getAllVariable(buildId)
                pauseStageNotify(
                    userId = userId,
                    stage = buildStage,
                    pipelineName = variables[PIPELINE_NAME] ?: pipelineId,
                    buildNum = variables[PIPELINE_BUILD_NUM] ?: "1"
                )
            } else {
                val allStageStatus = stageBuildDetailService.stageStart(
                    buildId = buildId, stageId = stageId,
                    controlOption = controlOption!!,
                    checkIn = checkIn, checkOut = checkOut
                )
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    pipelineBuildStageDao.updateStatus(
                        dslContext = context, buildId = buildId, stageId = stageId, buildStatus = BuildStatus.QUEUE,
                        controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                    )
                    pipelineBuildDao.updateStatus(
                        dslContext = context, buildId = buildId,
                        oldBuildStatus = BuildStatus.STAGE_SUCCESS, newBuildStatus = BuildStatus.RUNNING
                    )
                    pipelineBuildDao.updateBuildStageStatus(
                        dslContext = context, buildId = buildId, stageStatus = allStageStatus
                    )
                    pipelineBuildSummaryDao.updateRunningCount(
                        dslContext = context, pipelineId = pipelineId, buildId = buildId, runningIncrement = 1
                    )
                }
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStageEvent(
                        source = BS_MANUAL_START_STAGE, projectId = projectId, pipelineId = pipelineId,
                        userId = userId, buildId = buildId, stageId = stageId, actionType = ActionType.REFRESH
                    )
                    // #3400 点Stage启动时处于DETAIL界面，以操作人视角，没有刷历史列表的必要
                )
            }
            return true
        }
    }

    fun cancelStage(
        userId: String,
        buildStage: PipelineBuildStage,
        reviewRequest: StageReviewRequest?,
        timeout: Boolean? = false
    ): Boolean {
        with(buildStage) {
            checkIn?.reviewGroup(
                userId = if (timeout == true) "SYSTEM" else userId,
                groupId = reviewRequest?.id,
                action = ManualReviewAction.ABORT,
                suggest = if (timeout == true) "TIMEOUT" else reviewRequest?.suggest
            )
            // 5019 暂时只有准入有审核逻辑，准出待产品规划
            checkIn?.status = BuildStatus.REVIEW_ABORT.name
            stageBuildDetailService.stageCancel(
                buildId = buildId, stageId = stageId, controlOption = controlOption!!,
                checkIn = checkIn, checkOut = checkOut
            )

            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                pipelineBuildStageDao.updateStatus(
                    dslContext = context,
                    buildId = buildId, stageId = stageId, buildStatus = BuildStatus.STAGE_SUCCESS,
                    checkIn = checkIn, checkOut = checkOut
                )
                pipelineBuildDao.updateStatus(
                    dslContext = context, buildId = buildId,
                    oldBuildStatus = BuildStatus.STAGE_SUCCESS, newBuildStatus = BuildStatus.RUNNING
                )
                // #4255 stage审核超时恢复运行状态需要将运行状态+1，即使直接结束也会在finish阶段减回来
                pipelineBuildSummaryDao.updateRunningCount(
                    dslContext = context, pipelineId = pipelineId,
                    buildId = buildId, runningIncrement = 1
                )
            }
            // #3138 Stage Cancel 需要走finally Stage流程
            pipelineEventDispatcher.dispatch(
                PipelineBuildStageEvent(
                    source = BS_STAGE_CANCELED_END_SOURCE, projectId = projectId,
                    pipelineId = pipelineId, userId = userId,
                    buildId = buildId, stageId = stageId,
                    actionType = ActionType.END
                )
                // #3400 FinishEvent会刷新HISTORY列表的Stage状态
            )
            return true
        }
    }

    fun getLastStage(buildId: String): PipelineBuildStage? {
        val result = pipelineBuildStageDao.getMaxStage(dslContext, buildId)
        if (result != null) {
            return pipelineBuildStageDao.convert(result)
        }
        return null
    }

    fun getPendingStage(buildId: String): PipelineBuildStage? {
        var pendingStage = pipelineBuildStageDao.getByStatus(dslContext, buildId, BuildStatus.RUNNING)
        if (pendingStage == null) {
            pendingStage = pipelineBuildStageDao.getByStatus(dslContext, buildId, BuildStatus.QUEUE)
        }
        return pendingStage
    }

    fun pauseStageNotify(
        userId: String,
        stage: PipelineBuildStage,
        pipelineName: String,
        buildNum: String
    ) {
        val checkIn = stage.checkIn ?: return
        val group = stage.checkIn?.groupToReview() ?: return

        pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE.name,
                source = "s(${stage.stageId}) waiting for REVIEW",
                projectId = stage.projectId, pipelineId = stage.pipelineId,
                userId = userId, buildId = stage.buildId,
                receivers = group.reviewers,
                titleParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "buildNum" to buildNum
                ),
                bodyParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewDesc" to (checkIn.reviewDesc ?: "")
                )
            )
        )
    }

    /**
     * 流水线引擎Stage事件 [event]
     * 该Stage的当前配置属性 [stage]
     * 上下文中的环境变量 [variables]
     * 控制当前检查是准入还是准出使用 [inOrOut] 准入为true，准出为false
     */
    fun checkQualityPassed(
        event: PipelineBuildStageEvent,
        stage: PipelineBuildStage,
        variables: Map<String, String>,
        inOrOut: Boolean
    ): Boolean {
        val (check, position) = if (inOrOut) {
            Pair(stage.checkIn, ControlPointPosition.BEFORE_POSITION)
        } else {
            Pair(stage.checkOut, ControlPointPosition.AFTER_POSITION)
        }
        return try {
            val request = BuildCheckParamsV3(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                position = position,
                templateId = null,
                interceptName = null,
                ruleBuildIds = check?.ruleIds!!.toSet(),
                runtimeVariable = variables
            )
            logger.info("ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_REQUEST|${event.stageId}|" +
                "inOrOut=$inOrOut|request=$request|ruleIds=${check.ruleIds}")
            val result = client.get(ServiceQualityRuleResource::class).check(request).data!!
            logger.info("ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_RESPONSE|${event.stageId}|" +
                "inOrOut=$inOrOut|response=$result|ruleIds=${check.ruleIds}")
            check.checkTimes = result.checkTimes
            result.success
        } catch (ignore: Throwable) {
            logger.error("ENGINE|${event.buildId}|${event.source}|inOrOut=$inOrOut|" +
                "STAGE_QUALITY_CHECK_ERROR|${event.stageId}", ignore)
            false
        }
    }

    fun retryRefreshStage(model: Model) {
        model.stages.forEach { stage ->
            stage.checkIn?.retryRefresh()
            stage.checkOut?.retryRefresh()
        }
    }
}
