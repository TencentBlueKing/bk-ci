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

import com.tencent.devops.process.engine.control.DispatchQueueControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Job的互斥组命令处理
 */
@Service
class CheckDispatchQueueContainerCmd(
    private val dispatchQueueControl: DispatchQueueControl
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckDispatchQueueContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.container.status.isReadyToRun() &&
            commandContext.container.matrixGroupFlag != true &&
            commandContext.stageMatrixCount > 1 // 存在矩阵才进行并发队列判断
    }

    override fun execute(commandContext: ContainerContext) {
        // 终止或者结束事件不做队列判断
        if (!commandContext.event.actionType.isEnd()) {
            dispatchQueueCheck(commandContext)
        }
    }

    private fun dispatchQueueCheck(commandContext: ContainerContext) {
        val container = commandContext.container
        val dequeueResult = dispatchQueueControl.tryToDispatch(container)
        if (dequeueResult) {
            LOG.info("ENGINE|${container.buildId}|DEQUEUE_SUCCESS|${container.stageId}|j(${container.containerId})")
            commandContext.cmdFlowState = CmdFlowState.CONTINUE // 出队列成功，继续向下执行
        } else {
            LOG.info("ENGINE|${container.buildId}|DEQUEUE_FAILED|${container.stageId}|j(${container.containerId})")
            commandContext.latestSummary = "dispatch_delay"
            commandContext.cmdFlowState = CmdFlowState.LOOP // 循环消息命令 延时10秒钟
        }
    }
}
