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

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
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
            (commandContext.container.status.isReadyToRun() ||
                commandContext.container.status == BuildStatus.DEPENDENT_WAITING)
    }

    override fun execute(commandContext: ContainerContext) {
        // 仅在初次进入Container时进行跳过和依赖判断
        if (checkIfSkip(commandContext)) {
            commandContext.buildStatus = BuildStatus.SKIP
            commandContext.latestSummary = "j(${commandContext.container.containerId}) skipped"
            commandContext.cmdFlowState = CmdFlowState.FINALLY // 跳转至FINALLY，处理SKIP
        } else if (commandContext.buildStatus.isFailure()) {
            // 如果前置出现失败，则走向结束
            commandContext.cmdFlowState = CmdFlowState.FINALLY
        }
    }

    /**
     * 检查[ContainerContext.container]是否被按条件跳过
     */
    fun checkIfSkip(containerContext: ContainerContext): Boolean {
        if (containerContext.containerTasks.isEmpty() &&
            containerContext.container.matrixGroupFlag != true) {
            return true // 非构建矩阵且无任务
        }
        // condition check
        val container = containerContext.container
        val containerControlOption = container.controlOption
        var needSkip = false
        if (containerControlOption != null) {
            val jobControlOption = containerControlOption.jobControlOption
            val conditions = jobControlOption.customVariables ?: emptyList()

            needSkip = if (containerControlOption.inFinallyStage) {
                skipFinallyStageJob(container, jobControlOption, containerContext.event.previousStageStatus)
            } else {
                val contextMap = pipelineContextService.buildContext(
                    projectId = container.projectId,
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
                    buildLogPrinter = buildLogPrinter,
                    jobId = container.containerHashId ?: "",
                    taskId = VMUtils.genStartVMTaskId(container.containerId),
                    executeCount = container.executeCount
                )
            }
            if (needSkip) {
                LOG.info(
                    "ENGINE|${container.buildId}|${containerContext.event.source}|CONTAINER_SKIP" +
                        "|${container.stageId}|j(${container.containerId})|conditions=$jobControlOption"
                )
            }
        }
        return needSkip
    }

    private fun skipFinallyStageJob(
        container: PipelineBuildContainer,
        jobControlOption: JobControlOption,
        previousStatus: BuildStatus?
    ): Boolean {
        val skip = when (jobControlOption.runCondition) {
            JobRunCondition.PREVIOUS_STAGE_CANCEL -> {
                previousStatus != null && !previousStatus.isCancel() // null will pass
            }
            JobRunCondition.PREVIOUS_STAGE_FAILED -> {
                previousStatus != null && !previousStatus.isFailure() // null will pass
            }
            JobRunCondition.PREVIOUS_STAGE_SUCCESS -> { // null will pass
                previousStatus != null && !previousStatus.isSuccess() && previousStatus != BuildStatus.STAGE_SUCCESS
            }
            JobRunCondition.STAGE_RUNNING -> false // 当前 Finally Stage 开始时， 无视之前的状态，不跳过
            else -> true // finallyStage 下除上述4种条件之外，都是不合法的，要跳过
        } /* need skip */
        if (skip) {
            buildLogPrinter.addLine(
                buildId = container.buildId,
                message = "Skip when ${jobControlOption.runCondition} previous stage status is $previousStatus",
                tag = container.stageId,
                jobId = container.containerHashId,
                executeCount = container.executeCount
            )
        }
        return skip
    }
}
