/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 暂停逻辑处理
 */
@Service
class CheckPauseContainerCmd : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckPauseContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE && !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: ContainerContext) {
        val container = commandContext.container
        val event = commandContext.event
        // #5244 暂停时收到取消指令[ActionType.END]
        if (container.status.isPause() && event.actionType.isEnd() && event.source != "completeClaimBuildTask") {
            LOG.info("ENGINE|${event.buildId}|${event.source}|PAUSE_CANCEL|${event.stageId}|j(${event.containerId})")
            commandContext.buildStatus = BuildStatus.CANCELED
            commandContext.latestSummary = "j(${container.containerId}) pause cancel"
            commandContext.cmdFlowState = CmdFlowState.FINALLY
        }
    }
}
