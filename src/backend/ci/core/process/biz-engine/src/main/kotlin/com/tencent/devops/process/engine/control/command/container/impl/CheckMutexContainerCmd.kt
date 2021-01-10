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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.ContainerMutexStatus
import com.tencent.devops.process.engine.control.MutexControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Job的互斥组命令处理
 */
@Service
class CheckMutexContainerCmd(
    private val mutexControl: MutexControl
) : ContainerCmd {

    companion object {
        private val logger = LoggerFactory.getLogger(CheckMutexContainerCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: ContainerContext) {
        // 终止或者结束事件不做互斥判断
        if (!ActionType.isEnd(commandContext.event.actionType)) {
            mutexCheck(commandContext)
        }
    }

    private fun mutexCheck(commandContext: ContainerContext) {
        val event = commandContext.event
        val container = commandContext.container
        val mutexResult = mutexControl.checkContainerMutex(
            projectId = event.projectId,
            buildId = event.buildId,
            stageId = event.stageId,
            containerId = event.containerId,
            mutexGroup = commandContext.mutexGroup,
            container = container
        )
        with(event) {
            when (mutexResult) {
                ContainerMutexStatus.CANCELED -> {
                    logger.info("[$buildId]|MUTEX_CANCEL|stage=$stageId|container=$containerId|projectId=$projectId")
                    // job互斥失败处理
                    commandContext.latestSummary = "container_dependOn_failed"
                    commandContext.cmdFlowState = CmdFlowState.FINALLY // 结束命令
                }
                ContainerMutexStatus.WAITING -> {
                    logger.info("[$buildId]|MUTEX_DELAY|stage=$stageId|container=$containerId|projectId=$projectId")
                    commandContext.latestSummary = "CONTAINER_MUTEX_DELAY"
                    commandContext.cmdFlowState = CmdFlowState.LOOP // 循环消息命令 延时10秒钟
                }
                else -> { // 正常运行
                    commandContext.latestSummary = "CONTAINER_MUTEX_DELAY"
                    commandContext.cmdFlowState = CmdFlowState.CONTINUE // 检查通过，继续向下执行
                    logger.info("[$buildId]|MUTEX_RUNNING|stage=$stageId|container=$containerId|projectId=$projectId")
                }
            }
        }
    }
}
