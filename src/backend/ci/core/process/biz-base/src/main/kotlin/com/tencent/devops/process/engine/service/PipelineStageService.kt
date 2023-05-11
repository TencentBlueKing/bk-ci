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

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityCheckBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.common.BS_QUALITY_ABORT_STAGE
import com.tencent.devops.process.engine.common.BS_QUALITY_PASS_STAGE
import com.tencent.devops.process.engine.common.BS_STAGE_CANCELED_END_SOURCE
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.detail.StageBuildDetailService
import com.tencent.devops.process.engine.service.record.StageBuildRecordService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PipelineVarUtil
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
@Suppress("TooManyFunctions", "LongParameterList", "LongMethod")
class PipelineStageService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildStageDao: PipelineBuildStageDao,
    private val buildVariableService: BuildVariableService,
    private val stageBuildDetailService: StageBuildDetailService,
    private val stageBuildRecordService: StageBuildRecordService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineStageService::class.java)
    }

    fun getStage(projectId: String, buildId: String, stageId: String?): PipelineBuildStage? {
        return pipelineBuildStageDao.get(dslContext, projectId, buildId, stageId)
    }

    fun getAllBuildStage(projectId: String, buildId: String): Collection<PipelineBuildStage> {
        return pipelineBuildStageDao.getByBuildId(dslContext, projectId, buildId)
    }

    fun updateStageStatus(
        projectId: String,
        buildId: String,
        stageId: String,
        buildStatus: BuildStatus,
        checkIn: StagePauseCheck?,
        checkOut: StagePauseCheck?
    ) {
        logger.info("[$buildId]|updateStageStatus|status=$buildStatus|stageId=$stageId")
        JooqUtils.retryWhenDeadLock {
            pipelineBuildStageDao.updateStatus(
                dslContext = dslContext, projectId = projectId, buildId = buildId,
                stageId = stageId, buildStatus = buildStatus,
                checkIn = checkIn, checkOut = checkOut
            )
        }
    }

    fun listStages(projectId: String, buildId: String): List<PipelineBuildStage> {
        return pipelineBuildStageDao.listBuildStages(dslContext, projectId, buildId)
    }

    fun batchSave(transactionContext: DSLContext?, stageList: Collection<PipelineBuildStage>) {
        return JooqUtils.retryWhenDeadLock {
            pipelineBuildStageDao.batchSave(transactionContext ?: dslContext, stageList)
        }
    }

    fun batchUpdate(transactionContext: DSLContext?, stageList: Collection<PipelineBuildStage>) {
        return JooqUtils.retryWhenDeadLock {
            pipelineBuildStageDao.batchUpdate(transactionContext ?: dslContext, stageList)
        }
    }

    fun deletePipelineBuildStages(transactionContext: DSLContext?, projectId: String, pipelineId: String) {
        pipelineBuildStageDao.deletePipelineBuildStages(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun skipStage(userId: String, buildStage: PipelineBuildStage) {
        with(buildStage) {
            val allStageStatus = stageBuildRecordService.stageSkip(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount
            )
            JooqUtils.retryWhenDeadLock {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    pipelineBuildStageDao.updateStatus(
                        dslContext = context, projectId = projectId, buildId = buildId,
                        stageId = stageId, buildStatus = BuildStatus.SKIP,
                        controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                    )

                    pipelineBuildDao.updateBuildStageStatus(
                        dslContext = context, projectId = projectId, buildId = buildId, stageStatus = allStageStatus
                    )
                }
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "skipStage", projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                )
            )
        }
    }

    fun refreshCheckStageStatus(userId: String, buildStage: PipelineBuildStage, inOrOut: Boolean) {
        with(buildStage) {
            val allStageStatus = stageBuildRecordService.stageCheckQuality(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                stageId = stageId, executeCount = executeCount,
                controlOption = controlOption!!, inOrOut = inOrOut,
                checkIn = checkIn, checkOut = checkOut
            )
            JooqUtils.retryWhenDeadLock {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    pipelineBuildStageDao.updateStatus(
                        dslContext = context, projectId = projectId, buildId = buildId,
                        stageId = stageId, controlOption = controlOption!!,
                        // #5246 所有质量红线检查都不影响stage原构建状态
                        buildStatus = null, initStartTime = true,
                        checkIn = checkIn, checkOut = checkOut
                    )
                    pipelineBuildDao.updateBuildStageStatus(
                        dslContext = context,
                        projectId = projectId,
                        buildId = buildId,
                        stageStatus = allStageStatus
                    )
                }
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "refreshCheckStageStatus", projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, refreshTypes = RefreshType.HISTORY.binary
                )
            )
        }
    }

    fun pauseStage(buildStage: PipelineBuildStage) {
        with(buildStage) {
            // 兜底保护，若已经被审核过则直接忽略
            if (checkIn?.status == BuildStatus.REVIEW_ABORT.name ||
                checkIn?.status == BuildStatus.REVIEW_PROCESSED.name) {
                return@with
            }
            checkIn?.status = BuildStatus.REVIEWING.name
            val allStageStatus = stageBuildRecordService.stagePause(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                executeCount = executeCount,
                controlOption = controlOption!!,
                checkIn = checkIn,
                checkOut = checkOut
            )

            JooqUtils.retryWhenDeadLock {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    pipelineBuildStageDao.updateStatus(
                        dslContext = context, projectId = projectId, buildId = buildId,
                        stageId = stageId, buildStatus = BuildStatus.PAUSE,
                        controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                    )
                    pipelineBuildDao.updateStatus(
                        dslContext = context, buildId = buildId, projectId = projectId,
                        oldBuildStatus = BuildStatus.RUNNING, newBuildStatus = BuildStatus.STAGE_SUCCESS
                    )
                    pipelineBuildDao.updateBuildStageStatus(
                        dslContext = context, projectId = projectId, buildId = buildId, stageStatus = allStageStatus
                    )
                    // 被暂停的流水线不占构建队列，在执行数-1
                    pipelineBuildSummaryDao.updateRunningCount(
                        dslContext = context, projectId = projectId, pipelineId = pipelineId,
                        buildId = buildId, runningIncrement = -1
                    )
                }
            }

            // #3400 点Stage启动时处于DETAIL界面，以操作人视角，没有刷历史列表的必要
        }
    }

    fun stageManualStart(
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
            stageBuildRecordService.stageReview(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                stageId = stageId, executeCount = executeCount,
                controlOption = controlOption!!,
                checkIn = checkIn, checkOut = checkOut
            )
            JooqUtils.retryWhenDeadLock {
                // #4531 stage先保持暂停，如果没有其他需要审核的审核组则可以启动stage，否则直接返回
                pipelineBuildStageDao.updateStatus(
                    dslContext = dslContext, projectId = projectId, buildId = buildId,
                    stageId = stageId, buildStatus = BuildStatus.PAUSE,
                    controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                )
            }
            // 如果还有待审核的审核组，则直接通知并返回
            if (checkIn?.groupToReview() != null) {
                val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
                pauseStageNotify(
                    userId = userId,
                    triggerUserId = variables[PIPELINE_START_USER_NAME] ?: userId,
                    stage = buildStage,
                    pipelineName = variables[PIPELINE_NAME] ?: pipelineId,
                    buildNum = variables[PIPELINE_BUILD_NUM] ?: "1"
                )
            } else {
                val allStageStatus = stageBuildRecordService.stageManualStart(
                    projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                    stageId = stageId, executeCount = executeCount,
                    controlOption = controlOption!!, checkIn = checkIn, checkOut = checkOut
                )
                JooqUtils.retryWhenDeadLock {
                    dslContext.transaction { configuration ->
                        val context = DSL.using(configuration)
                        pipelineBuildStageDao.updateStatus(
                            dslContext = context, projectId = projectId, buildId = buildId, stageId = stageId,
                            buildStatus = BuildStatus.QUEUE,
                            controlOption = controlOption, checkIn = checkIn, checkOut = checkOut
                        )
                        pipelineBuildDao.updateStatus(
                            dslContext = context, buildId = buildId, projectId = projectId,
                            oldBuildStatus = BuildStatus.STAGE_SUCCESS, newBuildStatus = BuildStatus.RUNNING
                        )
                        pipelineBuildDao.updateBuildStageStatus(
                            dslContext = context, projectId = projectId, buildId = buildId, stageStatus = allStageStatus
                        )
                        pipelineBuildSummaryDao.updateRunningCount(
                            dslContext = context, projectId = projectId, pipelineId = pipelineId,
                            buildId = buildId, runningIncrement = 1
                        )
                    }
                }
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStageEvent(
                        source = BS_MANUAL_START_STAGE, projectId = projectId, pipelineId = pipelineId,
                        userId = userId, buildId = buildId, stageId = stageId, actionType = ActionType.REFRESH
                    ),
                    PipelineBuildReviewBroadCastEvent(
                        source = "stage($stageId) reviewed with PROCESSED", projectId = projectId,
                        pipelineId = pipelineId, buildId = buildId, userId = userId,
                        stageId = stageId, taskId = null, reviewType = BuildReviewType.QUALITY_CHECK_IN,
                        status = BuildStatus.REVIEW_PROCESSED.name
                    )
                    // #3400 点Stage启动时处于DETAIL界面，以操作人视角，没有刷历史列表的必要
                )
            }
            return true
        }
    }

    fun cancelStageBySystem(
        userId: String,
        buildInfo: BuildInfo,
        buildStage: PipelineBuildStage,
        timeout: Boolean? = false
    ) {

        val checkMap: Map<Boolean, StagePauseCheck?> = mapOf(
            true to buildStage.checkIn,
            false to buildStage.checkOut
        )

        checkMap.forEach { (inOrOut, pauseCheck) ->
            // #5654 如果是红线待审核状态则取消红线审核
            if (pauseCheck?.status == BuildStatus.QUALITY_CHECK_WAIT.name) {
                qualityTriggerStage(
                    userId = userId,
                    buildStage = buildStage,
                    qualityRequest = StageQualityRequest(
                        position = ControlPointPosition.BEFORE_POSITION,
                        pass = false,
                        checkTimes = buildStage.executeCount
                    ),
                    inOrOut = inOrOut,
                    check = pauseCheck,
                    timeout = timeout
                )
            }
            // #5654 如果是待人工审核则取消人工审核
            else if (pauseCheck?.groupToReview() != null) {
                val pipelineInfo =
                    pipelineRepositoryService.getPipelineInfo(buildStage.projectId, buildStage.pipelineId)
                cancelStage(
                    userId = userId,
                    triggerUserId = buildInfo.triggerUser,
                    pipelineName = pipelineInfo?.pipelineName,
                    buildNum = buildInfo.buildNum,
                    buildStage = buildStage,
                    reviewRequest = StageReviewRequest(
                        reviewParams = listOf(),
                        id = pauseCheck.groupToReview()?.id,
                        suggest = "CANCEL"
                    ),
                    timeout = timeout
                )
            }
        }
    }

    fun cancelStage(
        userId: String,
        pipelineName: String?,
        buildNum: Int,
        triggerUserId: String,
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
            stageBuildRecordService.stageCancel(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId, stageId = stageId,
                executeCount = executeCount, controlOption = controlOption!!,
                checkIn = checkIn, checkOut = checkOut
            )

            JooqUtils.retryWhenDeadLock {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    pipelineBuildStageDao.updateStatus(
                        dslContext = context,
                        projectId = projectId,
                        buildId = buildId, stageId = stageId, buildStatus = BuildStatus.STAGE_SUCCESS,
                        checkIn = checkIn, checkOut = checkOut
                    )
                    pipelineBuildDao.updateStatus(
                        dslContext = context, buildId = buildId, projectId = projectId,
                        oldBuildStatus = BuildStatus.STAGE_SUCCESS, newBuildStatus = BuildStatus.RUNNING
                    )
                    // #4255 stage审核超时恢复运行状态需要将运行状态+1，即使直接结束也会在finish阶段减回来
                    pipelineBuildSummaryDao.updateRunningCount(
                        dslContext = context, projectId = projectId, pipelineId = pipelineId,
                        buildId = buildId, runningIncrement = 1
                    )
                }
            }
            // #3138 Stage Cancel 需要走finally Stage流程
            pipelineEventDispatcher.dispatch(
                PipelineBuildStageEvent(
                    source = BS_STAGE_CANCELED_END_SOURCE, projectId = projectId,
                    pipelineId = pipelineId, userId = userId,
                    buildId = buildId, stageId = stageId,
                    actionType = ActionType.END
                ),
                PipelineBuildReviewBroadCastEvent(
                    source = "stage($stageId) reviewed with ABORT", projectId = projectId,
                    pipelineId = pipelineId, buildId = buildId, userId = userId,
                    stageId = stageId, taskId = null, reviewType = BuildReviewType.QUALITY_CHECK_IN,
                    status = BuildStatus.REVIEW_ABORT.name
                ),
                PipelineBuildNotifyEvent(
                    notifyTemplateEnum = PipelineNotifyTemplateEnum
                        .PIPELINE_MANUAL_REVIEW_STAGE_REJECT_TO_TRIGGER_TEMPLATE.name,
                    source = "s($stageId) waiting for REVIEW [triggerUser]",
                    projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId,
                    receivers = listOf(triggerUserId),
                    titleParams = mutableMapOf(
                        "projectName" to "need to add in notifyListener",
                        "pipelineName" to (pipelineName ?: pipelineId),
                        "buildNum" to buildNum.toString()
                    ),
                    bodyParams = mutableMapOf(
                        "projectName" to "need to add in notifyListener",
                        "pipelineName" to (pipelineName ?: pipelineId),
                        "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                        "reviewDesc" to (checkIn?.reviewDesc ?: ""),
                        "suggest" to (reviewRequest?.suggest ?: ""),
                        "rejectUserId" to userId,
                        // 企业微信组
                        NotifyUtils.WEWORK_GROUP_KEY to (checkIn?.notifyGroup?.joinToString(separator = ",") ?: "")
                    ),
                    position = ControlPointPosition.BEFORE_POSITION,
                    stageId = stageId,
                    notifyType = NotifyUtils.checkNotifyType(checkIn?.notifyType) ?: mutableSetOf(),
                    markdownContent = checkIn?.markdownContent
                )
                // #3400 FinishEvent会刷新HISTORY列表的Stage状态
            )
            return true
        }
    }

    fun qualityTriggerStage(
        userId: String,
        buildStage: PipelineBuildStage,
        qualityRequest: StageQualityRequest,
        inOrOut: Boolean,
        check: StagePauseCheck,
        timeout: Boolean? = false
    ) {
        with(buildStage) {
            logger.info(
                "ENGINE|$buildId|STAGE_QUALITY_TRIGGER|$stageId|" +
                    "inOrOut=$inOrOut|request=$qualityRequest|timeout=$timeout"
            )
            val (stageNextStatus, reviewType) = if (inOrOut) {
                Pair(BuildStatus.QUEUE, BuildReviewType.QUALITY_CHECK_IN)
            } else {
                Pair(BuildStatus.SUCCEED, BuildReviewType.QUALITY_CHECK_OUT)
            }
            JooqUtils.retryWhenDeadLock {
                pipelineBuildStageDao.updateStatus(
                    dslContext = dslContext, projectId = projectId, buildId = buildId, stageId = stageId,
                    buildStatus = stageNextStatus, controlOption = controlOption,
                    checkIn = checkIn, checkOut = checkOut
                )
            }
            val (source, actionType, reviewStatus) = if (qualityRequest.pass) {
                check.status = BuildStatus.QUALITY_CHECK_PASS.name
                Triple(BS_QUALITY_PASS_STAGE, ActionType.REFRESH, BuildStatus.REVIEW_PROCESSED)
            } else {
                check.status = BuildStatus.QUALITY_CHECK_FAIL.name
                Triple(BS_QUALITY_ABORT_STAGE, ActionType.END, BuildStatus.REVIEW_ABORT)
            }
            pipelineEventDispatcher.dispatch(
                PipelineBuildReviewBroadCastEvent(
                    source = "s(${buildStage.stageId}) has been reviewed",
                    projectId = buildStage.projectId, pipelineId = buildStage.pipelineId,
                    buildId = buildStage.buildId, userId = userId,
                    reviewType = reviewType,
                    status = reviewStatus.name,
                    stageId = buildStage.stageId, taskId = null,
                    timeout = timeout
                ),
                PipelineBuildStageEvent(
                    source = source, projectId = projectId,
                    pipelineId = pipelineId, userId = userId,
                    buildId = buildId, stageId = stageId,
                    actionType = actionType
                )
            )
        }
    }

    /**
     * 取构建[buildId]当前序号为[currentStageSeq]的上一个[PipelineBuildStage]
     * 如果不存在则返回null
     */
    fun getPrevStage(projectId: String, buildId: String, currentStageSeq: Int): PipelineBuildStage? {
        return pipelineBuildStageDao.getAdjacentStage(dslContext, projectId, buildId, currentStageSeq, sortAsc = false)
    }

    /**
     * 取构建[buildId]当前序号为[currentStageSeq]的下一个[PipelineBuildStage]
     * 如果不存在则返回null
     */
    fun getNextStage(projectId: String, buildId: String, currentStageSeq: Int): PipelineBuildStage? {
        return pipelineBuildStageDao.getAdjacentStage(dslContext, projectId, buildId, currentStageSeq, sortAsc = true)
    }

    /**
     * 取构建[buildId]的最后一个[PipelineBuildStage]
     * 如果不存在则返回null
     */
    fun getLastStage(projectId: String, buildId: String): PipelineBuildStage? {
        return pipelineBuildStageDao.getMaxStage(dslContext, projectId, buildId)
    }

    private val pendingStatusSet = setOf(BuildStatus.RUNNING, BuildStatus.PAUSE, BuildStatus.QUEUE)

    /**
     * 取构建[buildId]处于[BuildStatus.RUNNING] [BuildStatus.PAUSE] [BuildStatus.QUEUE]
     * 状态的Stage列表，并按stage序号递增排序的第一个最小的Stage， 如果是一个全部完成的构建，则将会返回null
     */
    fun getPendingStage(projectId: String, buildId: String): PipelineBuildStage? {
        return pipelineBuildStageDao.getOneByStatus(dslContext, projectId, buildId, pendingStatusSet)
    }

    fun pauseStageNotify(
        userId: String,
        triggerUserId: String,
        stage: PipelineBuildStage,
        pipelineName: String,
        buildNum: String
    ) {
        val checkIn = stage.checkIn ?: return
        val group = stage.checkIn?.groupToReview() ?: return

        pipelineEventDispatcher.dispatch(
            PipelineBuildReviewBroadCastEvent(
                source = "s(${stage.stageId}) waiting for REVIEW",
                projectId = stage.projectId, pipelineId = stage.pipelineId,
                buildId = stage.buildId, userId = userId,
                reviewType = BuildReviewType.STAGE_REVIEW,
                status = BuildStatus.REVIEWING.name,
                stageId = stage.stageId, taskId = null
            ),
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
                    "reviewDesc" to (checkIn.reviewDesc ?: ""),
                    "reviewers" to group.reviewers.joinToString(),
                    // 企业微信组
                    NotifyUtils.WEWORK_GROUP_KEY to (checkIn.notifyGroup?.joinToString(separator = ",") ?: "")
                ),
                position = ControlPointPosition.BEFORE_POSITION,
                stageId = stage.stageId,
                notifyType = NotifyUtils.checkNotifyType(checkIn.notifyType) ?: mutableSetOf(),
                markdownContent = checkIn.markdownContent
            )
        )
        // #7971 无指定通知类型时、或者触发人是审核人时，不去通知触发人。
        if (triggerUserId !in group.reviewers && !checkIn.notifyType.isNullOrEmpty()) {
            pipelineEventDispatcher.dispatch(
                PipelineBuildNotifyEvent(
                    notifyTemplateEnum = PipelineNotifyTemplateEnum
                        .PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TO_TRIGGER_TEMPLATE.name,
                    source = "s(${stage.stageId}) waiting for REVIEW [triggerUser]",
                    projectId = stage.projectId, pipelineId = stage.pipelineId,
                    userId = userId, buildId = stage.buildId,
                    receivers = listOf(triggerUserId),
                    titleParams = mutableMapOf(
                        "projectName" to "need to add in notifyListener",
                        "pipelineName" to pipelineName,
                        "buildNum" to buildNum
                    ),
                    bodyParams = mutableMapOf(
                        "projectName" to "need to add in notifyListener",
                        "pipelineName" to pipelineName,
                        "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                        "reviewDesc" to (checkIn.reviewDesc ?: ""),
                        "reviewers" to group.reviewers.joinToString()
                    ),
                    position = ControlPointPosition.BEFORE_POSITION,
                    stageId = stage.stageId,
                    markdownContent = checkIn.markdownContent,
                    notifyType = null // 为null时，以模板配置的通知类型为准
                )
            )
        }
    }

    /**
     * 流水线引擎Stage事件 [event]
     * 该Stage的当前配置属性 [stage]
     * 上下文中的环境变量 [variables]
     * 控制当前检查是准入还是准出使用 [inOrOut] 准入为true，准出为false
     */
    fun checkStageQuality(
        event: PipelineBuildStageEvent,
        stage: PipelineBuildStage,
        variables: Map<String, String>,
        inOrOut: Boolean
    ): BuildStatus {
        val (check, position, reviewType) = if (inOrOut) {
            Triple(stage.checkIn, ControlPointPosition.BEFORE_POSITION, BuildReviewType.QUALITY_CHECK_IN)
        } else {
            Triple(stage.checkOut, ControlPointPosition.AFTER_POSITION, BuildReviewType.QUALITY_CHECK_OUT)
        }
        // #5246 检查红线时填充预置上下文
        val buildContext = variables.toMutableMap()
        PipelineVarUtil.fillContextVarMap(buildContext, variables)
        return try {
            val request = BuildCheckParamsV3(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                position = position,
                templateId = null,
                interceptName = null,
                ruleBuildIds = check?.ruleIds!!.toSet(),
                stageId = stage.stageId,
                runtimeVariable = buildContext
            )
            logger.info(
                "ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_REQUEST|${event.stageId}|" +
                    "inOrOut=$inOrOut|request=$request|ruleIds=${check.ruleIds}"
            )
            val result = client.get(ServiceQualityRuleResource::class).check(request).data!!
            logger.info(
                "ENGINE|${event.buildId}|${event.source}|STAGE_QUALITY_CHECK_RESPONSE|${event.stageId}|" +
                    "inOrOut=$inOrOut|response=$result|ruleIds=${check.ruleIds}"
            )
            check.checkTimes = result.checkTimes

            // #5246 如果红线通过则直接成功，否则判断是否需要等待把关
            val qualityStatus = if (result.success) {
                BuildStatus.QUALITY_CHECK_PASS
            } else if (result.failEnd) {
                BuildStatus.QUALITY_CHECK_FAIL
            } else {
                // #5533 增加红线待审核的消息
                pipelineEventDispatcher.dispatch(
                    PipelineBuildReviewBroadCastEvent(
                        source = "s(${stage.stageId}) waiting for ${reviewType}_REVIEW",
                        projectId = stage.projectId, pipelineId = stage.pipelineId,
                        buildId = stage.buildId, userId = event.userId,
                        reviewType = reviewType, status = BuildStatus.REVIEWING.name,
                        stageId = stage.stageId, taskId = null
                    )
                )
                BuildStatus.QUALITY_CHECK_WAIT
            }

            pipelineEventDispatcher.dispatch(
                PipelineBuildQualityCheckBroadCastEvent(
                    source = "s(${stage.stageId}) waiting for ${reviewType}_REVIEW",
                    projectId = stage.projectId, pipelineId = stage.pipelineId,
                    buildId = stage.buildId, userId = event.userId,
                    status = qualityStatus.name, stageId = stage.stageId,
                    taskId = null, ruleIds = check.ruleIds
                )
            )
            return qualityStatus
        } catch (ignore: Throwable) {
            logger.error(
                "ENGINE|${event.buildId}|${event.source}|inOrOut=$inOrOut|STAGE_QUALITY_CHECK_ERROR|${event.stageId}",
                ignore
            )
            BuildStatus.QUALITY_CHECK_FAIL
        }
    }
}
