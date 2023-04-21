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

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.BS_CANCEL_BUILD_SOURCE
import com.tencent.devops.process.engine.common.BS_QUALITY_ABORT_STAGE
import com.tencent.devops.process.engine.common.BS_QUALITY_PASS_STAGE
import com.tencent.devops.process.engine.common.BS_STAGE_CANCELED_END_SOURCE
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.DispatchQueueControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.detail.StageBuildDetailService
import com.tencent.devops.process.engine.service.record.StageBuildRecordService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 每一个Stage结束后续命令处理
 */
@Service
@Suppress("LongParameterList", "TooManyFunctions")
class UpdateStateForStageCmdFinally(
    private val pipelineStageService: PipelineStageService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContainerService: PipelineContainerService,
    private val stageBuildDetailService: StageBuildDetailService,
    private val stageBuildRecordService: StageBuildRecordService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchQueueControl: DispatchQueueControl
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.FINALLY
    }

    override fun execute(commandContext: StageContext) {
        val event = commandContext.event
        val stage = commandContext.stage

        // 不在当前重试范围的请求。 比如 重试Stage-3，他之前的Stage直接跳过
        if (stage.status.isFinish() && stage.executeCount < commandContext.executeCount) {
            return nextOrFinish(event, stage, commandContext, false)
        }

        // #3138 stage cancel 不在此处理 更新状态&模型 @see PipelineStageService.cancelStage
        // #4732 stage 准入准出的质量红线失败时不需要刷新当前 stage 的状态
        if (event.source != BS_STAGE_CANCELED_END_SOURCE &&
            commandContext.buildStatus != BuildStatus.QUALITY_CHECK_FAIL
        ) {
            updateStageStatus(commandContext = commandContext)
        }

        // Stage 暂停或者 插件暂停
        if (commandContext.buildStatus == BuildStatus.STAGE_SUCCESS) {
            if (event.source != BS_STAGE_CANCELED_END_SOURCE && event.source != BS_CANCEL_BUILD_SOURCE) {
                // 不是 stage cancel 或取消构建，则进行暂停逻辑
                pipelineStageService.pauseStage(stage)
            } else {
                nextOrFinish(event, stage, commandContext, false)
                sendStageEndCallBack(stage, event)
            }
        } else if (commandContext.buildStatus.isFinish()) { // 当前Stage结束
            if (commandContext.buildStatus == BuildStatus.SKIP) { // 跳过
                pipelineStageService.skipStage(userId = event.userId, buildStage = stage)
            } else if (commandContext.buildStatus == BuildStatus.QUALITY_CHECK_FAIL) {
                pipelineStageService.refreshCheckStageStatus(userId = event.userId, buildStage = stage, inOrOut = false)
            }
            nextOrFinish(event, stage, commandContext, commandContext.buildStatus.isSuccess())
            sendStageEndCallBack(stage, event)
        }
    }

    private fun sendStageEndCallBack(stage: PipelineBuildStage, event: PipelineBuildStageEvent) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "UpdateStateForStageCmdFinally", projectId = stage.projectId, pipelineId = stage.pipelineId,
                userId = event.userId, buildId = stage.buildId, stageId = stage.stageId, actionType = ActionType.END
            )
        )
    }

    private fun nextOrFinish(
        event: PipelineBuildStageEvent,
        stage: PipelineBuildStage,
        commandContext: StageContext,
        needCheckQuality: Boolean
    ) {

        // #6440 stage结束时清理构建机启动队列
        dispatchQueueControl.flushDispatchQueue(stage.buildId, stage.stageId)

        // #5019 在结束阶段做stage准出判断，如果不需要红线检查则直接跳过
        if (needCheckQuality && qualityCheckOutAndBreak(stage, commandContext, event)) return

        val nextStage: PipelineBuildStage?

        // 中断的失败事件或者FastKill快速失败，或者 #3138 stage cancel 则直接寻找FinallyStage
        val gotoFinal = commandContext.buildStatus.isFailure() ||
            commandContext.buildStatus.isCancel() ||
            commandContext.fastKill ||
            event.source == BS_STAGE_CANCELED_END_SOURCE

        if (gotoFinal) {
            nextStage = pipelineStageService.getLastStage(projectId = event.projectId, buildId = event.buildId)
            if (nextStage == null || nextStage.seq == stage.seq || nextStage.controlOption?.finally != true) {

                LOG.info(
                    "ENGINE|${stage.buildId}|${event.source}|END_STAGE|${stage.stageId}|" +
                        "${commandContext.buildStatus}|${commandContext.latestSummary}"
                )

                return finishBuild(commandContext = commandContext)
            }
            event.actionType = ActionType.START // final 需要执行
        } else {
            nextStage = pipelineStageService.getNextStage(
                projectId = event.projectId,
                buildId = event.buildId,
                currentStageSeq = stage.seq
            )
        }

        if (nextStage != null) {
            LOG.info(
                "ENGINE|${event.buildId}|${event.source}|NEXT_STAGE|${event.stageId}|gotoFinal=$gotoFinal|" +
                    "next_s(${nextStage.stageId})|e=${stage.executeCount}|summary=${commandContext.latestSummary}"
            )
            event.sendNextStage(source = "From_s(${stage.stageId})", stageId = nextStage.stageId)
        } else {

            // 正常完成构建
            finishBuild(commandContext = commandContext)

            LOG.info(
                "ENGINE|${stage.buildId}|${event.source}|STAGE_FINALLY|${stage.stageId}|" +
                    "${commandContext.buildStatus}|${commandContext.latestSummary}"
            )
        }
    }

    private fun qualityCheckOutAndBreak(
        stage: PipelineBuildStage,
        commandContext: StageContext,
        event: PipelineBuildStageEvent
    ): Boolean {

        // #5246 只在stage运行成功（不包括被跳过）并配置了红线规则时做准出判断
        if (stage.checkOut?.ruleIds?.isNotEmpty() != true ||
            !commandContext.buildStatus.isSuccess() ||
            commandContext.buildStatus == BuildStatus.SKIP
        ) {
            return false
        }

        var needBreak = false
        when {
            event.source == BS_QUALITY_PASS_STAGE -> {
                qualityCheckOutPass(commandContext)
            }
            event.source == BS_QUALITY_ABORT_STAGE || event.actionType.isEnd() -> {
                qualityCheckOutFailed(commandContext)
            }
            else -> {
                val checkStatus = pipelineStageService.checkStageQuality(
                    event = event,
                    stage = stage,
                    variables = commandContext.variables,
                    inOrOut = false
                )
                when (checkStatus) {
                    BuildStatus.QUALITY_CHECK_PASS -> {
                        qualityCheckOutPass(commandContext)
                    }
                    BuildStatus.QUALITY_CHECK_WAIT -> {
                        // #5246 如果设置了把关人则卡在运行状态等待审核
                        qualityCheckOutNeedReview(commandContext)
                        needBreak = true
                    }
                    else -> {
                        qualityCheckOutFailed(commandContext)
                    }
                }
            }
        }
        return needBreak
    }

    private fun qualityCheckOutNeedReview(
        commandContext: StageContext
    ) {
        LOG.info(
            "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}" +
                "|STAGE_QUALITY_CHECK_OUT_REVIEWING|${commandContext.event.stageId}"
        )
        commandContext.stage.checkOut?.status = BuildStatus.QUALITY_CHECK_WAIT.name
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) need reviewing with QUALITY_CHECK_OUT"
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = false
        )
    }

    private fun qualityCheckOutFailed(commandContext: StageContext) {
        commandContext.stage.checkOut?.status = BuildStatus.QUALITY_CHECK_FAIL.name
        commandContext.buildStatus = BuildStatus.QUALITY_CHECK_FAIL
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) failed with QUALITY_CHECK_OUT"
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = false
        )
    }

    private fun qualityCheckOutPass(commandContext: StageContext) {
        LOG.info(
            "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}" +
                "|STAGE_QUALITY_CHECK_OUT_PASSED|${commandContext.event.stageId}"
        )
        commandContext.stage.checkOut?.status = BuildStatus.QUALITY_CHECK_PASS.name
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) passed with QUALITY_CHECK_OUT"
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = false
        )
    }

    /**
     * 更新[commandContext]下指定的Stage的状态以及编排模型状态
     */
    private fun updateStageStatus(commandContext: StageContext) {
        val event = commandContext.event
        // 更新状态
        pipelineStageService.updateStageStatus(
            projectId = event.projectId,
            buildId = event.buildId,
            stageId = event.stageId,
            buildStatus = commandContext.buildStatus,
            checkIn = commandContext.stage.checkIn,
            checkOut = commandContext.stage.checkOut
        )

        // 对未结束的Container进行强制更新[失败状态]
        if (commandContext.buildStatus.isFailure()) {
            forceFlushContainerStatus(commandContext = commandContext, stageStatus = commandContext.buildStatus)
        }

        // stage第一次启动[isReadyToRun]或者准备结束[commandContext.buildStatus]，要刷新编排模型 TODO 改进
        if (commandContext.stage.status.isReadyToRun() || commandContext.buildStatus.isFinish()) {

            // 如果是因fastKill强制终止，流水线状态标记为失败
            if (commandContext.fastKill || commandContext.buildStatus.isFailure()) {
                commandContext.buildStatus = BuildStatus.FAILED
            }
            val allStageStatus = stageBuildRecordService.updateStageStatus(
                projectId = event.projectId, pipelineId = event.pipelineId, buildId = event.buildId,
                stageId = event.stageId, executeCount = commandContext.executeCount,
                buildStatus = commandContext.buildStatus
            )
            pipelineRuntimeService.updateBuildHistoryStageState(
                projectId = event.projectId,
                buildId = event.buildId,
                allStageStatus = allStageStatus
            )
        }
    }

    /**
     * 仅在[stageStatus]为结束状态时，强制刷新下面的Container状态
     */
    private fun forceFlushContainerStatus(commandContext: StageContext, stageStatus: BuildStatus) {
        if (!stageStatus.isFinish()) {
            val buildId = commandContext.event.buildId
            val stageId = commandContext.event.stageId
            LOG.warn("ENGINE|$buildId|${commandContext.event.source}|$stageId|STAGE_FLUSH_STATUS|illegal $stageStatus")
            return
        }
        commandContext.containers.forEach { c ->
            if (!c.status.isFinish()) { // #4315 未结束的，都需要刷新
                pipelineContainerService.updateContainerStatus(
                    projectId = c.projectId,
                    buildId = c.buildId,
                    stageId = c.stageId,
                    containerId = c.containerId,
                    endTime = LocalDateTime.now(),
                    buildStatus = stageStatus
                )

                if (commandContext.fastKill) {
                    buildLogPrinter.addYellowLine(
                        buildId = c.buildId,
                        tag = VMUtils.genStartVMTaskId(c.containerId),
                        jobId = c.containerHashId,
                        executeCount = c.executeCount,
                        message = "job(${c.containerId}) stop by fast kill"
                    )
                }
            }
        }
    }

    /**
     * 发送指定[stageId]的Stage启动事件
     */
    private fun PipelineBuildStageEvent.sendNextStage(source: String, stageId: String) {
        // #5108 修正因为END被改写成START，导致取消变成继续往下执行。
        pipelineEventDispatcher.dispatch(this.copy(source = source, stageId = stageId))
    }

    /**
     * 完成构建事件
     */
    private fun finishBuild(commandContext: StageContext) {

        commandContext.latestSummary = "finally_s(${commandContext.stage.stageId})"
        if (commandContext.buildStatus.isSuccess()) { // #3400 最后一个Stage成功代表整个Build成功，but when finally stage:
            if (commandContext.stage.controlOption?.finally == true) {
                if (commandContext.previousStageStatus == BuildStatus.STAGE_SUCCESS) {
                    // #3138 如果上游流水线STAGE成功，则继承
                    commandContext.buildStatus = BuildStatus.STAGE_SUCCESS
                } else if (commandContext.previousStageStatus?.isFailure() == true ||
                    commandContext.previousStageStatus?.isCancel() == true
                ) {
                    // #3138 如果上游流水线失败/取消，则流水线最终状态为失败/取消， 否则为finallyStage的状态
                    commandContext.buildStatus = commandContext.previousStageStatus!!
                    commandContext.latestSummary += "|previousStageStatus=${commandContext.buildStatus}"
                }
            }
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = commandContext.latestSummary,
                projectId = commandContext.stage.projectId,
                pipelineId = commandContext.stage.pipelineId,
                userId = commandContext.event.userId,
                buildId = commandContext.stage.buildId,
                status = commandContext.buildStatus
            )
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UpdateStateForStageCmdFinally::class.java)
    }
}
