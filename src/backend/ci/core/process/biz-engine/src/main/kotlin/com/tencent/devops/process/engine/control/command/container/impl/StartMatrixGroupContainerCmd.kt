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

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("TooManyFunctions", "LongParameterList")
@Service
class StartMatrixGroupContainerCmd(
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(StartMatrixGroupContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish() &&
            commandContext.container.status.isReadyToRun() &&
            commandContext.container.matrixGroupFlag == true
    }

    override fun execute(commandContext: ContainerContext) {

        // 获取组内所有待执行容器，下发容器启动的事件
        val parentContainer = commandContext.container
        val groupContainers = pipelineContainerService.listGroupContainers(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId,
            matrixGroupId = parentContainer.containerId
        )
        startGroupContainers(commandContext, parentContainer, groupContainers)

        commandContext.latestSummary = "Matrix(${commandContext.container.containerId})_group_start"
        commandContext.buildStatus = BuildStatus.RUNNING
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    private fun startGroupContainers(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer,
        groupContainers: List<PipelineBuildContainer>
    ) {
        val event = commandContext.event
        val actionType = ActionType.START

        LOG.info("ENGINE|${event.buildId}|MATRIX_GROUP_START|${event.stageId}|actionType=$actionType|" +
            "j(${event.containerId})|count=${groupContainers.size}|groupContainers=$groupContainers")

        buildLogPrinter.addYellowLine(
            buildId = event.buildId,
            message = "Matrix container(${parentContainer.containerId}) start to run " +
                "${groupContainers.size} inner containers:",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            jobId = parentContainer.containerHashId,
            executeCount = commandContext.executeCount
        )

        groupContainers.forEach { container ->
            buildLogPrinter.addYellowLine(
                buildId = event.buildId,
                message = "Container(${container.containerId}) starting...",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            LOG.info("ENGINE|${event.buildId}|sendMatrixContainerEvent|START|${event.stageId}" +
                "|matrixGroupIDd=${parentContainer.containerId}|j(${container.containerId})|" +
                "count=${groupContainers.size}")
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(
                    source = "From_s(${container.stageId})",
                    projectId = container.projectId,
                    pipelineId = container.pipelineId,
                    userId = event.userId,
                    buildId = container.buildId,
                    stageId = container.stageId,
                    containerType = container.containerType,
                    containerId = container.containerId,
                    containerHashId = container.containerHashId,
                    actionType = actionType,
                    errorCode = 0,
                    errorTypeName = null,
                    reason = commandContext.latestSummary
                )
            )
        }
    }
}
