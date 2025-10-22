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

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage的快速失败或者中断执行的命令处理
 */
@Service
class CheckInterruptStageCmd : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: StageContext) {
        // 检查是不是FastKill的场景
        val fastKillHasFailureJob = parseFastKill(commandContext = commandContext)
        // [终止事件]或[等待审核超时] 直接结束流水线，不需要判断各个Stage的状态，可直接停止
        val fastKill = commandContext.fastKill
        if (fastKill || commandContext.event.actionType.isTerminate()) {
            val event = commandContext.event
            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_INTERRUPT|${event.stageId}|fastKill=$fastKill")
            commandContext.buildStatus = detectStageInterruptStatus(commandContext, fastKillHasFailureJob)
            commandContext.latestSummary = if (fastKill) {
                "FastKill"
            } else {
                "${commandContext.event.actionType}"
            }
            commandContext.event.actionType = ActionType.TERMINATE
            commandContext.cmdFlowState = CmdFlowState.CONTINUE // 进入StartContainerStageCmd进行Job下发终止处理，而非直接更新状态
        }
    }

    /**
     * 检查[commandContext]是否中了FastKil场景。
     * 并返回该FastKill场景下有没有失败的Container， 有返回true
     */
    private fun parseFastKill(commandContext: StageContext): Boolean {
        if (commandContext.stage.controlOption?.fastKill == true) {
            commandContext.containers.forEach { container ->
                if (ControlUtils.checkContainerFailure(container)) {
                    commandContext.fastKill = true // 设置标志快速失败
                    return container.status.isFailure()
                }
            }
        }
        return false
    }

    /**
     * 在FastKill中断结束时，构建应当是什么状态:
     * 若当前Stage下有容器因任务暂停导致出现暂停状态的容器，取决于(快速失败中是否有失败容器[fastKillHasFailureJob])标识值决定：
     * 为false时：[BuildStatus.CANCELED]状态
     * 为true时：[BuildStatus.FAILED]状态
     * 若当前Stage下没有容器因任务暂停导致出现暂停状态的容器，直接[BuildStatus.FAILED]状态
     */
    private fun detectStageInterruptStatus(commandContext: StageContext, fastKillHasFailureJob: Boolean): BuildStatus {
        if (!commandContext.fastKill) {
            return BuildStatus.TERMINATE
        }
        // fastKill状态下：因插件任务暂停导致的CANCEL状态的容器
        val pauseStop = commandContext.containers.filter { it.status.isCancel() }
        return if (pauseStop.isNotEmpty()) { // 存在因插件任务暂停的容器
            if (fastKillHasFailureJob) { // 如失败用FAILED，否则CANCELED
                BuildStatus.FAILED
            } else {
                BuildStatus.CANCELED
            }
        } else { // 不存在插件任务暂停，直接FAIL
            BuildStatus.FAILED
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CheckInterruptStageCmd::class.java)
    }
}
