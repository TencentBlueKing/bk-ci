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
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.context.MatrixBuildContext
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.utils.PIPELINE_RETRY_ALL_FAILED_CONTAINER
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage下发Container事件命令处理
 */
@Service
class InitializeContainerStageCmd(
    private val dslContext: DSLContext,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(InitializeContainerStageCmd::class.java)
    }

    override fun canExecute(commandContext: StageContext): Boolean {
        // 仅在初次准备并发执行Stage下Container是执行
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.buildStatus.isReadyToRun()
    }

    override fun execute(commandContext: StageContext) {

        // 在下发构建机任务前进行构建矩阵计算
        val count = generateMatrixGroup(commandContext)

        LOG.info("ENGINE|${commandContext.stage.buildId}|MATRIX_CONTAINER_INIT|" +
            "s(${commandContext.stage.stageId})|newContainerCount=$count")
        commandContext.latestSummary = "from_s(${commandContext.stage.stageId}) generateNew($count)"
        commandContext.cmdFlowState = CmdFlowState.CONTINUE
    }

    private fun generateMatrixGroup(commandContext: StageContext) {
        val event = commandContext.event
        val variables = commandContext.variables
        val model = containerBuildDetailService.getBuildModel(event.buildId) ?: return

        // #4518 待生成的分裂后container表和task表记录
        val buildContainers = mutableListOf<PipelineBuildContainer>()
        val buildTaskList = mutableListOf<PipelineBuildTask>()

        // #4518 根据当前上下文对每一个构建矩阵进行裂变
        model.stages.forEach { stage ->
            if (stage.id == commandContext.stage.stageId) {
                stage.containers.forEachIndexed { index, container ->
                    if (container.matrixGroupFlag == true) {

                        val context = MatrixBuildContext(
                            actionType = ActionType.START,
                            executeCount = commandContext.executeCount,
                            firstTaskId = "",
                            stageRetry = false,
                            retryStartTaskId = null,
                            userId = event.userId,
                            triggerUser = event.userId,
                            startType = StartType.valueOf(variables[PIPELINE_START_TYPE] as String),
                            parentBuildId = variables[PIPELINE_START_PARENT_BUILD_ID],
                            parentTaskId = variables[PIPELINE_START_PARENT_BUILD_TASK_ID],
                            channelCode = if (variables[PIPELINE_START_CHANNEL] != null) {
                                ChannelCode.valueOf(variables[PIPELINE_START_CHANNEL].toString())
                            } else {
                                ChannelCode.BS
                            },
                            retryFailedContainer = variables[PIPELINE_RETRY_ALL_FAILED_CONTAINER]?.toBoolean() ?: false,
                            skipFailedTask = variables[PIPELINE_SKIP_FAILED_TASK]?.toBoolean() ?: false,
                            // #4518 裂变的容器的seq id需要以父容器的seq id作为前缀
                            containerSeq = VMUtils.genMatrixContainerSeq(container.id!!.toInt(), index)
                        )

                        LOG.info("ENGINE|${event.buildId}|${event.source}|INIT_MATRIX_CONTAINER|${event.stageId}|" +
                            "containerId=${container.id}|containerHashId=${container.containerHashId}|context=$context")

                        if (container is VMBuildContainer) {
                            pipelineContainerService.prepareMatrixVMBuildContainerTasks(
                                projectId = event.projectId,
                                pipelineId = event.pipelineId,
                                buildId = event.buildId,
                                container = container,
                                stage = stage,
                                context = context,
                                buildContainers = buildContainers,
                                buildTaskList = buildTaskList
                            )
                        }

                        LOG.info("ENGINE|${event.buildId}|${event.source}|INIT_MATRIX_CONTAINER|${event.stageId}|" +
                            "${container.id}|containerHashId=${container.containerHashId}|context=$context|" +
                            "buildContainers=$buildContainers|buildTaskList=$buildTaskList")
                    }
                }
            }
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineContainerService.batchSave(transactionContext, buildContainers)
            pipelineTaskService.batchSave(transactionContext, buildTaskList)
        }
    }
}
