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
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.FastKillUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage下发Container事件命令处理
 */
@Service
class StartContainerStageCmd(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(StartContainerStageCmd::class.java)
    }

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: StageContext) {
        // 执行成功则结束本次事件处理，否则要尝试下一stage
        judgeStageContainer(commandContext)
        commandContext.latestSummary = "from_s(${commandContext.stage.stageId})"
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    /**
     * 判断当前[commandContext]里的Stage并发下发Container消息
     */
    private fun judgeStageContainer(commandContext: StageContext) {
        val event = commandContext.event
        var stageStatus = commandContext.stage.status
        var newActionType = event.actionType
        // 针对刚启动的Stage
        if (stageStatus.isReadyToRun()) {
            if (newActionType == ActionType.REFRESH) { // 对未启动的Stage要变成开始指令
                newActionType = ActionType.START
            }

            when {
                newActionType.isStart() -> {
                    sendStageStartCallback(commandContext)
                    stageStatus = BuildStatus.RUNNING // 要启动Stage
                }

                newActionType.isEnd() -> stageStatus = BuildStatus.CANCELED // 若为终止命令，直接设置为取消
                newActionType == ActionType.SKIP -> stageStatus = BuildStatus.SKIP // 要跳过Stage
            }
        }

        if (stageStatus.isFinish() || stageStatus == BuildStatus.STAGE_SUCCESS) {
            commandContext.buildStatus = stageStatus // 已经是结束或者是STAGE_SUCCESS就直接返回
        } else if (commandContext.containers.isEmpty()) {
            commandContext.buildStatus = BuildStatus.SUCCEED
        } else {
            stageStatus = pickJob(commandContext, actionType = newActionType, userId = event.userId)

            if (commandContext.skipContainerNum == commandContext.containers.size) {
                stageStatus = BuildStatus.SKIP
            }

            commandContext.buildStatus = stageStatus
        }
    }

    private fun sendStageStartCallback(commandContext: StageContext) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStatusBroadCastEvent(
                source = "StartContainerStageCmd",
                projectId = commandContext.stage.projectId,
                pipelineId = commandContext.stage.pipelineId,
                userId = commandContext.event.userId,
                buildId = commandContext.stage.buildId,
                actionType = ActionType.START,
                stageId = commandContext.stage.stageId
            )
        )
    }

    /**
     * 根据Job状态补偿Stage状态
     * [userId]执行人利用当前[commandContext]的容器列表，以事件动作[actionType]
     * 加上各容器的状态，决定是否对相应容器进行下发容器动作事件，并最终返回当前stage的构建状态:
     * 仍然有容器未结束，会继续向该容器下发事件，并将stageStatus设置为：[BuildStatus.RUNNING]
     * 所有容器都结束，但有失败的容器，则根据容器的状态判断[BuildStatusSwitcher.stageStatusMaker]决定哪种失败状态设置stageStatus
     * 所有容器都结束，但有取消的容器，则直接返回[BuildStatusSwitcher.stageStatusMaker] 决定取消的状态设置stageStatus
     */
    @Suppress("ComplexMethod")
    private fun pickJob(commandContext: StageContext, actionType: ActionType, userId: String): BuildStatus {
        // hotfix：可能在出现最后的Job失败将前面的运行中的Job中断
        var running: BuildStatus? = null
        var fail: BuildStatus? = null
        var cancel: BuildStatus? = null

        val stage = commandContext.stage

        // 同一Stage下的多个Container是并行
        commandContext.containers.forEach { container ->
            val jobCount = container.controlOption.matrixControlOption?.totalCount ?: 1 // MatrixGroup存在裂变计算
            if (container.status.isCancel()) {
                commandContext.cancelContainerNum++
                cancel = BuildStatusSwitcher.stageStatusMaker.cancel(container.status)
            } else if (ControlUtils.checkContainerFailure(container)) {
                commandContext.failureContainerNum++
                fail = BuildStatusSwitcher.stageStatusMaker.forceFinish(container.status, commandContext.fastKill)
            } else if (container.status == BuildStatus.SKIP) {
                commandContext.skipContainerNum++
            } else if (container.status.isRunning() && !actionType.isEnd()) {
                commandContext.concurrency += jobCount
                // 已经在运行中的, 只接受终止
                running = BuildStatus.RUNNING
            } else if (!container.status.isFinish()) {
                running = BuildStatus.RUNNING
                commandContext.concurrency += jobCount
                sendBuildContainerEvent(commandContext, container, actionType = actionType, userId = userId)

                LOG.info(
                    "ENGINE|${container.buildId}|STAGE_CONTAINER_SEND|s(${container.stageId})|" +
                        "j(${container.containerId})|status=${container.status}|newActonType=$actionType"
                )
            }
        }

        if (commandContext.concurrency > commandContext.maxConcurrency) { // #5109 增加日志埋点监控，以免影响Redis性能
            LOG.warn(
                "ENGINE|${stage.buildId}|JOB_BOMB_CK|${stage.projectId}|${stage.pipelineId}|s(${stage.stageId})" +
                    "|concurrency=${commandContext.concurrency}"
            )
        }

        // 如果有运行态,否则返回失败，如无失败，则返回取消，最后成功
        return running ?: fail ?: cancel ?: BuildStatus.SUCCEED
    }

    private fun sendBuildContainerEvent(
        commandContext: StageContext,
        container: PipelineBuildContainer,
        actionType: ActionType,
        userId: String
    ) {
        val errorCode: Int
        val errorTypeName: String?
        if (commandContext.fastKill) {
            val fastKillCodeType = FastKillUtils.fastKillCodeType()
            errorCode = fastKillCodeType.second
            errorTypeName = fastKillCodeType.first.name
        } else {
            errorTypeName = null
            errorCode = 0
        }
        // 通知容器构建消息
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "From_s(${container.stageId})",
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                userId = userId,
                buildId = container.buildId,
                stageId = container.stageId,
                previousStageStatus = commandContext.previousStageStatus,
                containerType = container.containerType,
                containerId = container.containerId,
                containerHashId = container.containerHashId,
                actionType = actionType,
                errorCode = errorCode,
                errorTypeName = errorTypeName,
                reason = commandContext.latestSummary
            )
        )
    }
}
