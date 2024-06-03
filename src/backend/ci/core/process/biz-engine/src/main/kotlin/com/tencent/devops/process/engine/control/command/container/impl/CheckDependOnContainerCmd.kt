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

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.DependOnControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import org.springframework.stereotype.Service

/**
 * Job的互斥组命令处理
 */
@Service
class CheckDependOnContainerCmd(
    private val dependOnControl: DependOnControl
) : ContainerCmd {

//    companion object {
//        private val LOG = LoggerFactory.getLogger(CheckDependOnContainerCmd::class.java)
//    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish() &&
            commandContext.container.matrixGroupId.isNullOrBlank() // 非矩阵组内容器才判断依赖
    }

    override fun execute(commandContext: ContainerContext) {
        val container = commandContext.container
        // 仅在初次进入(readyToRun), 或处于DependOn的等待状态下，做依赖链路查检
        if (container.status.isReadyToRun()) {
            checkDependOnStatus(commandContext, container)
        }
    }

    /**
     * 判断depend on状态
     * @return true:继续执行 false:不往下执行
     */
    private fun checkDependOnStatus(commandContext: ContainerContext, container: PipelineBuildContainer) {
        // 当有依赖job时，根据依赖job的运行状态执行
        when (dependOnControl.dependOnJobStatus(container = container)) {
            BuildStatus.SKIP -> {
                commandContext.buildStatus = BuildStatus.SKIP
                commandContext.latestSummary = "j(${container.containerId}) dependency was skip"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
            BuildStatus.FAILED -> {
                commandContext.buildStatus = BuildStatus.SKIP
                commandContext.latestSummary = "j(${container.containerId}) dependency was failed"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
            BuildStatus.SUCCEED -> {
                // 所有依赖都成功运行,则继续执行
                commandContext.cmdFlowState = CmdFlowState.CONTINUE // 依赖全部通过，可继续执行
            }
            else -> {
                commandContext.buildStatus = BuildStatus.DEPENDENT_WAITING
                commandContext.latestSummary = "j(${container.containerId}) waiting for dependency job"
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
        }
    }
}
