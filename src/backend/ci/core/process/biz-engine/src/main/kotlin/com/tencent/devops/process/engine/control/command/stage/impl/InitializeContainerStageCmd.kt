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
import com.tencent.devops.common.pipeline.container.matrix.VMBuildMatrixGroupContainer
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.detail.BaseBuildDetailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage下发Container事件命令处理
 */
@Service
class InitializeContainerStageCmd(
    private val buildDetailService: BaseBuildDetailService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(InitializeContainerStageCmd::class.java)
    }

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: StageContext) {
        // 在下发构建机任务前进行构建矩阵计算
        val count = generateMatrixGroup(commandContext)
        LOG.info("ENGINE|${commandContext.stage.buildId}|MATRIX_CONTAINER_INIT|" +
            "s(${commandContext.stage.stageId})|newContainerCount=$count")
        commandContext.latestSummary = "from_s(${commandContext.stage.stageId}) generateNew($count)"
        commandContext.cmdFlowState = CmdFlowState.CONTINUE
    }

    private fun generateMatrixGroup(commandContext: StageContext): Int {
        val event = commandContext.event
        var newCount = 0
        val model = buildDetailService.getBuildModel(event.buildId) ?: return newCount

        // #4518 根据当前上下文对每一个构建矩阵进行裂变
        model.stages.forEach { stage ->
            if (stage.id == commandContext.stage.stageId) {
                stage.containers.forEach { container ->
                    if (container is VMBuildMatrixGroupContainer) {

                    }
                }
            }
        }
        return 0
    }
}
