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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Stage暂停及审核事件的命令处理
 */
@Service
class CheckPauseReviewStageCmd(
    private val buildVariableService: BuildVariableService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: StageContext) {
        val stage = commandContext.stage
        val event = commandContext.event
        // 若stage状态为暂停，且来源不是BS_MANUAL_START_STAGE，碰到状态为暂停就停止运行
        if (commandContext.buildStatus.isPause() && event.source != BS_MANUAL_START_STAGE) {

            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_STOP_BY_PAUSE|${event.stageId}")
            commandContext.latestSummary = "s(${stage.stageId}) already in PAUSE!"
            commandContext.cmdFlowState = CmdFlowState.BREAK
        } else if (commandContext.buildStatus.isReadyToRun()) {

            val stageControlOption = stage.controlOption?.stageControlOption

            // 只有在非手动触发该Stage的首次运行做审核暂停
            val needPause = event.source != BS_MANUAL_START_STAGE &&
                stageControlOption?.manualTrigger == true &&
                stageControlOption.triggered == false

            if (needPause) {
                // #3742 进入暂停状态则刷新完状态后直接返回，等待手动触发
                LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_PAUSE|${event.stageId}")
                pauseStageNotify(commandContext)
                commandContext.buildStatus = BuildStatus.STAGE_SUCCESS
                commandContext.latestSummary = "s(${stage.stageId}) waiting for REVIEW"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
                return
            }

            commandContext.cmdFlowState = CmdFlowState.CONTINUE
            // #3742 只有经过手动审核才做审核变量的保存
            if (stageControlOption?.manualTrigger == true) {
                saveStageReviewParams(stage = stage, stageControlOption = stageControlOption)
            }
        }
    }

    private fun saveStageReviewParams(stage: PipelineBuildStage, stageControlOption: StageControlOption?) {
        // 该Stage进入运行状态，若存在审核变量设置则写入环境
        if (stageControlOption?.reviewParams?.isNotEmpty() == true) {
            buildVariableService.batchUpdateVariable(
                projectId = stage.projectId,
                pipelineId = stage.pipelineId,
                buildId = stage.buildId,
                variables = stageControlOption.reviewParams!!
                    .filter { !it.key.isNullOrBlank() }
                    .map { it.key!! to it.value.toString() }
                    .toMap()
            )
        }
    }

    /**
     * 发送通知
     */
    private fun pauseStageNotify(commandContext: StageContext) {
        val stage = commandContext.stage
        val triggerUsers = stage.controlOption?.stageControlOption?.triggerUsers?.joinToString(",") ?: ""
        val realUsers = EnvUtils.parseEnv(triggerUsers, commandContext.variables).split(",").toList()
        stage.controlOption!!.stageControlOption.triggerUsers = realUsers // 替换真正收件人

        // 发送通知
        val pipelineName = commandContext.variables[PIPELINE_NAME] ?: stage.pipelineId
        val buildNum = commandContext.variables[PIPELINE_BUILD_NUM] ?: "1"
        var reviewDesc = stage.controlOption!!.stageControlOption.reviewDesc
        reviewDesc = EnvUtils.parseEnv(reviewDesc, commandContext.variables)
        stage.controlOption!!.stageControlOption.reviewDesc = reviewDesc // 替换变量 #3395

        pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE.name,
                source = commandContext.latestSummary, projectId = stage.projectId, pipelineId = stage.pipelineId,
                userId = commandContext.event.userId, buildId = stage.buildId,
                receivers = stage.controlOption!!.stageControlOption.triggerUsers!!,
                titleParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "buildNum" to buildNum
                ),
                bodyParams = mutableMapOf(
                    "projectName" to "need to add in notifyListener",
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewDesc" to reviewDesc
                )
            )
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckPauseReviewStageCmd::class.java)
    }
}
