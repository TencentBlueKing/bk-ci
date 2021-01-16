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

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.springframework.stereotype.Service

/**
 * Stage下发Container事件命令处理
 */
@Service
class StartContainerStageCmd(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: StageContext) {
        val event = commandContext.event
        val stageId = commandContext.stage.stageId
        // 执行成功则结束本次事件处理，否则要尝试下一stage
        commandContext.buildStatus = judgeStageContainer(commandContext)
        event.logInfo(tag = "STAGE_DONE", message = "status=${commandContext.buildStatus}")
        commandContext.latestSummary = "from_s($stageId)"
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    /**
     * 判断当前[commandContext]里的Stage并发下发Container消息，并返回Stage状态
     */
    private fun judgeStageContainer(commandContext: StageContext): BuildStatus {
        val event = commandContext.event
        var stageStatus = commandContext.buildStatus
        var newActionType = event.actionType
        // 针对刚启动的Stage
        if (stageStatus.isReadyToRun()) {
            if (newActionType == ActionType.REFRESH) { // 对未启动的Stage要变成开始指令
                newActionType = ActionType.START
            }

            when {
                ActionType.isStart(newActionType) -> stageStatus = BuildStatus.RUNNING // 要启动Stage
                ActionType.isEnd(newActionType) -> stageStatus = BuildStatus.CANCELED // 若为终止命令，直接设置为取消
                newActionType == ActionType.SKIP -> stageStatus = BuildStatus.SKIP // 要跳过Stage
            }
//        } else if (stageStatus == BuildStatus.PAUSE && ActionType.isEnd(newActionType)) {
//            stageStatus = BuildStatus.STAGE_SUCCESS // 无意义的逻辑，移除
        } else {
            stageStatus = commandContext.buildStatus
        }

        return if (stageStatus.isFinish() || stageStatus == BuildStatus.STAGE_SUCCESS) {
            stageStatus // 已经是结束或者是STAGE_SUCCESS就直接返回
        } else {
            event.sendContainerEvent(commandContext.containers, actionType = newActionType, userId = event.userId)
        }
    }

    /**
     * 下发容器构建事件
     * @param containers 当前container对象列表
     * @param actionType 事件指令
     * @param userId 执行者
     * @return 返回true表示已经下发，如下发失败则返回false
     */
    private fun PipelineBuildStageEvent.sendContainerEvent(
        containers: List<PipelineBuildContainer>,
        actionType: ActionType,
        userId: String
    ): BuildStatus {

        var failureContainers = 0
        var cancelContainers = 0
        var stageStatus: BuildStatus? = null

        // 同一Stage下的多个Container是并行
        containers.forEach { container ->
            val containerId = container.containerId
            logInfo(tag = "STAGE_CONTAINER_CHECK", message = "j($containerId)|cs=${container.status}|nac=$actionType")
            if (container.status.isCancel()) {
                cancelContainers++
            } else if (container.status.isFailure()) {
                failureContainers++
            } else if (container.status.isReadyToRun() && !ActionType.isStart(actionType)) {
                // 失败或可重试的容器，如果不是重试动作，则跳过
            } else if (container.status.isRunning() && !ActionType.isTerminate(actionType)) {
                // 已经在运行中的, 只接受强制终止
            } else if (!container.status.isFinish()) {
                stageStatus = BuildStatus.RUNNING
                sendBuildContainerEvent(container = container, actionType = actionType, userId = userId)
                logInfo(tag = "STAGE_CONTAINER", message = "j($containerId)|newActionType=$actionType")
            }
        }

        if (stageStatus == null) {
            stageStatus = when {
                failureContainers > 0 -> BuildStatus.FAILED // 存在失败
                cancelContainers > 0 -> BuildStatus.CANCELED // 存在取消
                else -> BuildStatus.SUCCEED
            }
        }
        return stageStatus!!
    }

    private fun sendBuildContainerEvent(container: PipelineBuildContainer, actionType: ActionType, userId: String) {
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
                actionType = actionType
            )
        )
    }
}
