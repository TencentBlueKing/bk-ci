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
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Job的按条件跳过命令处理
 */
@Service
class CheckConditionalSkipContainerCmd : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckConditionalSkipContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        // 仅在初次进入Container
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE && commandContext.container.status.isReadyToRun()
    }

    override fun execute(commandContext: ContainerContext) {
        // 仅在初次进入Container时进行跳过和依赖判断
        if (checkIfSkip(commandContext)) {
            commandContext.buildStatus = BuildStatus.SKIP
            commandContext.latestSummary = "j(${commandContext.container.containerId}) skipped"
            commandContext.cmdFlowState = CmdFlowState.FINALLY // 跳转至FINALLY，处理SKIP
        }
    }

    /**
     * 检查[ContainerContext.container]是否被按条件跳过
     */
    fun checkIfSkip(containerContext: ContainerContext): Boolean {
        if (containerContext.containerTasks.isEmpty()) {
            return true // 无任务
        }
        // condition check
        val container = containerContext.container
        val variables = containerContext.variables
        val buildId = container.buildId
        val stageId = container.stageId
        val containerId = container.containerId
        val containerControlOption = container.controlOption
        var skip = false
        if (containerControlOption != null) {
            val jobControlOption = containerControlOption.jobControlOption
            val runCondition = jobControlOption.runCondition
            val conditions = jobControlOption.customVariables ?: emptyList()
            skip = ControlUtils.checkJobSkipCondition(conditions, variables, buildId, runCondition)
            if (skip) {
                LOG.info("ENGINE|$buildId|${containerContext.event.source}|CONTAINER_SKIP|$stageId|j($containerId)" +
                    "|conditions=$jobControlOption")
            }
        }
        return skip
    }
}
