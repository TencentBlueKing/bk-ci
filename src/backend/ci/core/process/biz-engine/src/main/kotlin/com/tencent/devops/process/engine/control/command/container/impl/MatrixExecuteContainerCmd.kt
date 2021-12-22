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
import com.tencent.devops.process.utils.PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_MAX
import kotlin.math.max
import kotlin.math.min
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("TooManyFunctions", "LongParameterList", "LongMethod")
@Service
class MatrixExecuteContainerCmd(
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(MatrixExecuteContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish() &&
            commandContext.container.matrixGroupFlag == true
    }

    override fun execute(commandContext: ContainerContext) {

        val parentContainer = commandContext.container

        // 获取组内所有待执行容器
        val groupContainers = pipelineContainerService.listGroupContainers(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId,
            matrixGroupId = parentContainer.containerId
        )
        val groupStatus = try {
            buildLogPrinter.addDebugLine(
                buildId = parentContainer.buildId,
                message = "Matrix status refresh: ${commandContext.buildStatus}",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            judgeGroupContainers(commandContext, parentContainer, groupContainers)
        } catch (ignore: Throwable) {
            buildLogPrinter.addDebugLine(
                buildId = parentContainer.buildId,
                message = "Matrix status refresh: ${commandContext.buildStatus} with " +
                    "error: ${ignore.message}",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            LOG.error("ENGINE|${parentContainer.buildId}|MATRIX_LOOP_MONITOR_FAILED|" +
                "matrix(${parentContainer.containerId})|groupContainers=$groupContainers" +
                "parentContainer=$parentContainer", ignore)
            BuildStatus.FAILED
        }

        if (groupStatus.isFinish()) {
            commandContext.buildStatus = groupStatus
            commandContext.latestSummary = "Matrix(${commandContext.container.containerId})_refresh_finish"
        } else {
            commandContext.buildStatus = BuildStatus.RUNNING
            commandContext.latestSummary = "Matrix(${commandContext.container.containerId})_refresh_continue"
        }
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    private fun judgeGroupContainers(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer,
        groupContainers: List<PipelineBuildContainer>
    ): BuildStatus {
        val event = commandContext.event
        val buildId = commandContext.container.buildId
        val containerHashId = commandContext.container.containerHashId
        val taskId = VMUtils.genStartVMTaskId(commandContext.container.containerId)
        val executeCount = commandContext.executeCount
        val matrixOption = parentContainer.controlOption?.matrixControlOption!!

        var failedCount = 0
        var runningCount = 0
        var finishCount = 0
        val containersToRun = mutableListOf<PipelineBuildContainer>()

        groupContainers.forEach { container ->
            when {
                container.status.isReadyToRun() -> {
                    containersToRun.add(container)
                }
                container.status.isRunning() -> {
                    runningCount++
                }
                container.status.isFinish() -> {
                    if (container.status.isFailure()) failedCount++
                    finishCount++
                }
            }
        }

        // 判断是否要进行fastKill
        val fastKill = failedCount > 0 && matrixOption.fastKill == true
        if (fastKill) {
            buildLogPrinter.addLine(
                buildId = buildId, message = "", tag = taskId,
                jobId = containerHashId, executeCount = executeCount
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "[MATRIX] Matrix(${parentContainer.containerId}) " +
                "failed containers count($failedCount), start to kill containers",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            terminateGroupContainers(commandContext, event, parentContainer, groupContainers)
        }

        val maxConcurrency = min(
            matrixOption.maxConcurrency ?: PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT,
            PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_MAX
        )

        // 如果不需要fastKill，则给前N个待执行的容器下发启动事件，N为并发上限减去正在运行的数量
        if (!fastKill && containersToRun.isNotEmpty()) {
            val countCanRun = max(0, maxConcurrency - runningCount)
            buildLogPrinter.addDebugLine(
                buildId = buildId, message = "Try to execute jobs: runningCount=$runningCount, " +
                "maxConcurrency=$maxConcurrency, countCanRun=$countCanRun",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            startGroupContainers(
                commandContext = commandContext,
                event = event,
                parentContainer = parentContainer,
                containersToRun = containersToRun.take(countCanRun)
            )
        }

        // 实时刷新数据库状态
        matrixOption.finishCount = finishCount
        matrixOption.totalCount = groupContainers.size

        pipelineContainerService.updateMatrixGroupStatus(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId,
            stageId = parentContainer.stageId,
            buildStatus = commandContext.buildStatus,
            matrixGroupId = parentContainer.containerId,
            controlOption = parentContainer.controlOption!!.copy(matrixControlOption = matrixOption),
            modelContainer = null
        )

        return if (finishCount == matrixOption.totalCount) {
            buildLogPrinter.addLine(
                buildId = buildId, message = "", tag = taskId,
                jobId = containerHashId, executeCount = executeCount
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "[MATRIX] Job execution completed",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )

            if (failedCount > 0) {
                BuildStatus.FAILED
            } else {
                BuildStatus.SUCCEED
            }
        } else {
            BuildStatus.RUNNING
        }
    }

    private fun startGroupContainers(
        event: PipelineBuildContainerEvent,
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer,
        containersToRun: List<PipelineBuildContainer>
    ) {
        LOG.info("ENGINE|${event.buildId}|MATRIX_GROUP_START|${event.stageId}|" +
            "matrix(${event.containerId})|containersToRun=$containersToRun")
        containersToRun.forEach { container ->
            buildLogPrinter.addDebugLine(
                buildId = event.buildId,
                message = "Container with id(${container.containerId}) and " +
                    "matrixGroupId(${parentContainer.containerId}）starting...",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            LOG.info("ENGINE|${event.buildId}|sendMatrixContainerEvent|START|${event.stageId}" +
                "|matrixGroupId=${parentContainer.containerId}|j(${container.containerId})")
            sendBuildContainerEvent(
                container = container,
                actionType = ActionType.START,
                userId = event.userId,
                reason = commandContext.latestSummary
            )
        }
    }

    private fun terminateGroupContainers(
        commandContext: ContainerContext,
        event: PipelineBuildContainerEvent,
        parentContainer: PipelineBuildContainer,
        groupContainers: List<PipelineBuildContainer>
    ) {
        LOG.info("ENGINE|${event.buildId}|MATRIX_GROUP_FAST_KILL|${event.stageId}|" +
            "j(${event.containerId})|count=${groupContainers.size}|groupContainers=$groupContainers")

        groupContainers.forEach { container ->
            if (!container.status.isFinish()) {
                sendBuildContainerEvent(
                    container = container,
                    actionType = ActionType.TERMINATE,
                    userId = event.userId,
                    reason = "from_matrix(${parentContainer.containerId})_fastKill"
                )
                buildLogPrinter.addLine(
                    buildId = event.buildId,
                    message = "[MATRIX] Matrix(${parentContainer.containerId}) try to stop " +
                        "container(${container.containerId})",
                    tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                    jobId = parentContainer.containerHashId,
                    executeCount = commandContext.executeCount
                )
            }
        }
    }

    private fun sendBuildContainerEvent(
        container: PipelineBuildContainer,
        actionType: ActionType,
        userId: String,
        reason: String
    ) {
        // 通知容器构建消息
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "From_s(${container.stageId})",
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                userId = userId,
                buildId = container.buildId,
                stageId = container.stageId,
                containerType = container.containerType,
                containerId = container.containerId,
                containerHashId = container.containerHashId,
                actionType = actionType,
                errorCode = 0,
                errorTypeName = null,
                reason = reason
            )
        )
    }
}
