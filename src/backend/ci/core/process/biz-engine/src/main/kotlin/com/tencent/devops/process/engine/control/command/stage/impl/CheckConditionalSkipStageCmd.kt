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

import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.DispatchQueueControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage的按条件跳过命令处理
 */
@Service
class CheckConditionalSkipStageCmd constructor(
    private val pipelineContextService: PipelineContextService,
    private val dispatchQueueControl: DispatchQueueControl,
    private val buildLogPrinter: BuildLogPrinter
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        // 仅在初次进入Stage
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.buildStatus.isReadyToRun()
    }

    override fun execute(commandContext: StageContext) {
        val stage = commandContext.stage
        // 仅在初次进入Stage时进行跳过和依赖判断
        try {
            if (checkIfSkip(commandContext)) {
                commandContext.buildStatus = BuildStatus.SKIP
                commandContext.latestSummary = "s(${stage.stageId}) skipped"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
        } catch (e: ExpressionParseException) {
            // 当条件判断出现异常情况时，stage直接判定为失败
            buildLogPrinter.addErrorLine(
                buildId = stage.buildId,
                message = "[${e.kind}] condition of stage is invalid: ${e.message}",
                tag = stage.stageId,
                executeCount = commandContext.executeCount
            )
            commandContext.buildStatus = BuildStatus.FAILED
            commandContext.latestSummary = "s(${stage.stageId}) check condition failed"
            commandContext.cmdFlowState = CmdFlowState.FINALLY
        }
        // 初次进入时清理构建机启动队列
        dispatchQueueControl.flushDispatchQueue(stage.buildId, stage.stageId)
    }

    /**
     * 检查[commandContext]中的[PipelineBuildStage]是否被按条件跳过
     */
    fun checkIfSkip(commandContext: StageContext): Boolean {
        val stage = commandContext.stage
        val controlOption = stage.controlOption?.stageControlOption
        val event = commandContext.event
        if (controlOption?.enable == false || commandContext.containers.isEmpty()) { // 无任务
//            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_SKIP|${event.stageId}|${controlOption?.enable}")
            return true
        }

        // condition check
        val variables = commandContext.variables
        var skip = false
        if (controlOption != null) {
            val conditions = controlOption.customVariables ?: emptyList()
            val contextMap = pipelineContextService.buildContext(
                projectId = stage.projectId,
                pipelineId = stage.pipelineId,
                buildId = stage.buildId,
                stageId = stage.stageId,
                containerId = null,
                taskId = null,
                variables = variables
            )
            skip = ControlUtils.checkStageSkipCondition(
                conditions = conditions,
                variables = variables.plus(contextMap),
                buildId = stage.buildId,
                runCondition = controlOption.runCondition,
                customCondition = controlOption.customCondition,
                asCodeEnabled = commandContext.pipelineAsCodeEnabled == true
            ) // #6366 增加日志明确展示跳过的原因  stage 没有相关可展示的地方，暂时不加
        }
        if (skip) {
            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_CONDITION_SKIP|${event.stageId}|$controlOption")
        }

        return skip
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckConditionalSkipStageCmd::class.java)
    }
}
