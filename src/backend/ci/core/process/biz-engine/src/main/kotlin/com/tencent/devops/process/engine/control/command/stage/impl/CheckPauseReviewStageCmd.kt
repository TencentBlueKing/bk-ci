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

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.common.BS_QUALITY_ABORT_STAGE
import com.tencent.devops.process.engine.common.BS_QUALITY_PASS_STAGE
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage暂停及审核事件的命令处理
 */
@Service
class CheckPauseReviewStageCmd(
    private val buildVariableService: BuildVariableService,
    private val pipelineStageService: PipelineStageService
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: StageContext) {
        val stage = commandContext.stage
        val event = commandContext.event

        // 处于等待中，遇到停止/取消等行为直接结束，因为本Stage还未进入
        if (event.actionType.isEnd() && commandContext.buildStatus.isPause()) {
            commandContext.buildStatus = BuildStatus.CANCELED
            commandContext.cmdFlowState = CmdFlowState.FINALLY
            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_CANCEL|${event.stageId}")
            return
        }

        // #115 若stage状态为暂停，且来源不是BS_MANUAL_START_STAGE，碰到状态为暂停就停止运行
        if (commandContext.buildStatus.isPause() && event.source != BS_MANUAL_START_STAGE) {

            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_STOP_BY_PAUSE|${event.stageId}")
            commandContext.latestSummary = "s(${stage.stageId}) already in PAUSE!"
            commandContext.cmdFlowState = CmdFlowState.BREAK
        } else if (commandContext.buildStatus.isReadyToRun()) {

            LOG.info("ENGINE|${event.buildId}|${event.stageId}|STAGE_CHECK_IN_START|event=$event")

            // #5019 只用第一次进入时做准入质量红线检查，如果是审核后的检查则跳过红线
            if (qualityCheckInAndBreak(event, commandContext, stage)) return

            // Stage人工审核
            if (needPause(event, stage)) {
                // #3742 进入暂停状态则刷新完状态后直接返回，等待手动触发
                LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_PAUSE|${event.stageId}")

                stage.checkIn?.parseReviewVariables(commandContext.variables, commandContext.pipelineAsCodeEnabled)
                pipelineStageService.pauseStageNotify(
                    userId = event.userId,
                    triggerUserId = commandContext.variables[PIPELINE_START_USER_NAME] ?: event.userId,
                    stage = stage,
                    pipelineName = commandContext.variables[PIPELINE_NAME] ?: stage.pipelineId,
                    buildNum = commandContext.variables[PIPELINE_BUILD_NUM] ?: "1"
                )
                commandContext.buildStatus = BuildStatus.STAGE_SUCCESS
                commandContext.latestSummary = "s(${stage.stageId}) waiting for REVIEW"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
                return
            }

            commandContext.cmdFlowState = CmdFlowState.CONTINUE
            // #3742 只有经过手动审核才做审核变量的保存
            if (stage.checkIn?.manualTrigger == true) {
                saveStageReviewParams(stage = stage)
            }
        }
    }

    private fun qualityCheckInAndBreak(
        event: PipelineBuildStageEvent,
        commandContext: StageContext,
        stage: PipelineBuildStage
    ): Boolean {

        // #4732 如果是手动触发stage的事件或未配置审核则直接略过
        if (stage.checkIn?.ruleIds?.isNotEmpty() != true ||
            event.source == BS_MANUAL_START_STAGE
        ) {
            return false
        }

        var needBreak = false
        when {
            event.source == BS_QUALITY_PASS_STAGE -> {
                qualityCheckInPass(commandContext)
            }
            event.source == BS_QUALITY_ABORT_STAGE || event.actionType.isEnd() -> {
                qualityCheckInFailed(commandContext)
                needBreak = true
            }
            else -> {
                val checkStatus = pipelineStageService.checkStageQuality(
                    event = event,
                    stage = stage,
                    variables = commandContext.variables,
                    inOrOut = true
                )
                when (checkStatus) {
                    BuildStatus.QUALITY_CHECK_PASS -> {
                        qualityCheckInPass(commandContext)
                    }
                    BuildStatus.QUALITY_CHECK_WAIT -> {
                        // #5246 如果设置了把关人则卡在运行状态等待审核
                        qualityCheckInNeedReview(commandContext)
                        needBreak = true
                    }
                    else -> {
                        // #4732 优先判断是否能通过质量红线检查
                        qualityCheckInFailed(commandContext)
                        needBreak = true
                    }
                }
            }
        }
        return needBreak
    }

    private fun qualityCheckInFailed(commandContext: StageContext) {
        LOG.info(
            "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}" +
                "|STAGE_QUALITY_CHECK_IN_FAILED|${commandContext.event.stageId}"
        )
        commandContext.stage.checkIn?.status = BuildStatus.QUALITY_CHECK_FAIL.name
        commandContext.buildStatus = BuildStatus.QUALITY_CHECK_FAIL
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) failed with QUALITY_CHECK_IN"
        commandContext.cmdFlowState = CmdFlowState.FINALLY
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = true
        )
    }

    private fun qualityCheckInNeedReview(commandContext: StageContext) {
        LOG.info(
            "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}" +
                "|STAGE_QUALITY_CHECK_IN_REVIEWING|${commandContext.event.stageId}"
        )
        commandContext.stage.checkIn?.status = BuildStatus.QUALITY_CHECK_WAIT.name
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) need reviewing with QUALITY_CHECK_IN"
        commandContext.cmdFlowState = CmdFlowState.BREAK
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = true
        )
    }

    private fun qualityCheckInPass(commandContext: StageContext) {
        LOG.info(
            "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}" +
                "|STAGE_QUALITY_CHECK_IN_PASSED|${commandContext.event.stageId}"
        )
        commandContext.stage.checkIn?.status = BuildStatus.QUALITY_CHECK_PASS.name
        commandContext.latestSummary = "s(${commandContext.stage.stageId}) passed with QUALITY_CHECK_IN"
        pipelineStageService.refreshCheckStageStatus(
            userId = commandContext.event.userId,
            buildStage = commandContext.stage,
            inOrOut = true
        )
    }

    private fun saveStageReviewParams(stage: PipelineBuildStage) {
        val reviewVariables = mutableMapOf<String, Any>()
        // # 4531 遍历全部审核组的参数，后序覆盖前序的同名变量
        stage.checkIn?.reviewParams?.forEach {
            reviewVariables[it.key] = it.value ?: return@forEach
        }
        if (stage.checkIn?.reviewParams?.isNotEmpty() == true) {
            buildVariableService.batchUpdateVariable(
                projectId = stage.projectId,
                pipelineId = stage.pipelineId,
                buildId = stage.buildId,
                variables = reviewVariables
            )
        }
    }

    private fun needPause(event: PipelineBuildStageEvent, stage: PipelineBuildStage): Boolean {
        // #115 只有在非手动触发该Stage的首次运行做审核暂停
        if (event.source == BS_MANUAL_START_STAGE || stage.checkIn?.manualTrigger != true) {
            return false
        }
        return stage.checkIn?.groupToReview() != null
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckPauseReviewStageCmd::class.java)
    }
}
