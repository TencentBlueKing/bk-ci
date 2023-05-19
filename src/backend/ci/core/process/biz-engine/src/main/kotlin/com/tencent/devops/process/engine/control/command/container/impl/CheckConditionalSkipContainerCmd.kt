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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PREVIOUS_STAGE_CANCEL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PREVIOUS_STAGE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PREVIOUS_STAGE_SUCCESS
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Job的按条件跳过命令处理
 */
@Service
class CheckConditionalSkipContainerCmd constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineContextService: PipelineContextService
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckConditionalSkipContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        // 仅在初次进入Container
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.container.status.isReadyToRun()
    }

    override fun execute(commandContext: ContainerContext) {
        val container = commandContext.container
        // 仅在初次进入Container时进行跳过和依赖判断
        try {
            if (checkIfSkip(commandContext)) {
                commandContext.buildStatus = BuildStatus.SKIP
                commandContext.latestSummary = "j(${container.containerId}) skipped"
                commandContext.cmdFlowState = CmdFlowState.FINALLY // 跳转至FINALLY，处理SKIP
            } else if (commandContext.buildStatus.isFailure()) {
                // 如果前置出现失败，则走向结束
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
        } catch (e: ExpressionParseException) {
            // 当条件判断出现异常情况时，stage直接判定为失败
            buildLogPrinter.addErrorLine(
                buildId = container.buildId,
                message = "[${e.kind}] condition of job is invalid: ${e.message}",
                jobId = container.containerHashId,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                executeCount = commandContext.executeCount
            )
            commandContext.buildStatus = BuildStatus.FAILED
            commandContext.latestSummary = "j(${container.containerId}) check condition failed"
            commandContext.cmdFlowState = CmdFlowState.FINALLY
        }
    }

    /**
     * 检查[ContainerContext.container]是否被按条件跳过
     */
    fun checkIfSkip(containerContext: ContainerContext): Boolean {
        if (containerContext.containerTasks.isEmpty() &&
            containerContext.container.matrixGroupFlag != true
        ) {
            return true // 非构建矩阵且无任务
        }
        // condition check
        val container = containerContext.container
        val containerControlOption = container.controlOption
        val jobControlOption = containerControlOption.jobControlOption
        val conditions = jobControlOption.customVariables ?: emptyList()

        val message = StringBuilder()
        val needSkip = if (containerControlOption.inFinallyStage) {
            skipFinallyStageJob(jobControlOption, containerContext.event.previousStageStatus, message)
        } else {
            val contextMap = pipelineContextService.buildContext(
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                buildId = container.buildId,
                stageId = container.stageId,
                containerId = container.containerId,
                taskId = null,
                variables = containerContext.variables
            )
            ControlUtils.checkJobSkipCondition(
                conditions = conditions,
                variables = containerContext.variables.plus(contextMap),
                buildId = container.buildId,
                runCondition = jobControlOption.runCondition,
                customCondition = jobControlOption.customCondition,
                message = message,
                asCodeEnabled = containerContext.pipelineAsCodeEnabled == true
            )
        }

        if (message.isNotBlank()) {
            // #6366 增加日志明确展示跳过的原因
            buildLogPrinter.addDebugLine(
                executeCount = containerContext.executeCount,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                buildId = container.buildId,
                message = message.toString(),
                jobId = container.containerHashId
            )
        }

        if (needSkip) {
            LOG.info(
                "ENGINE|${container.buildId}|${containerContext.event.source}|CONTAINER_SKIP" +
                    "|${container.stageId}|j(${container.containerId})|conditions=$jobControlOption"
            )
        }
        return needSkip
    }

    private fun skipFinallyStageJob(
        jobControlOption: JobControlOption,
        previousStatus: BuildStatus?,
        message: StringBuilder
    ): Boolean {
        // #6366 增加日志明确展示跳过的原因
        val skip = when (jobControlOption.runCondition) {
            JobRunCondition.PREVIOUS_STAGE_CANCEL -> {
                message.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_PREVIOUS_STAGE_CANCEL,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                previousStatus != null && !previousStatus.isCancel() // null will pass
            }

            JobRunCondition.PREVIOUS_STAGE_FAILED -> {
                message.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_PREVIOUS_STAGE_FAILED,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                previousStatus != null && !previousStatus.isFailure() // null will pass
            }

            JobRunCondition.PREVIOUS_STAGE_SUCCESS -> { // null will pass
                message.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_PREVIOUS_STAGE_SUCCESS,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                previousStatus != null && !previousStatus.isSuccess() && previousStatus != BuildStatus.STAGE_SUCCESS
            }

            JobRunCondition.STAGE_RUNNING -> false // 当前 Finally Stage 开始时， 无视之前的状态，不跳过
            else -> true // finallyStage 下除上述4种条件之外，都是不合法的，要跳过
        } /* need skip */
        if (skip) {
            message.append("Final stage Job skip when previous stage status is $previousStatus")
        }
        return skip
    }
}
