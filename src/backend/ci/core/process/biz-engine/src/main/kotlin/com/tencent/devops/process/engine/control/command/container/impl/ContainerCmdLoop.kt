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

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContainerCmdLoop(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(ContainerCmdLoop::class.java)
        private const val DEFAULT_LOOP_TIME_MILLS = 10000
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.LOOP && !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: ContainerContext) {
        // 需要将消息循环
        with(commandContext.container) {
            LOG.info("ENGINE|$buildId|${commandContext.event.source}]|EVENT_LOOP|$stageId|j($containerId)")
        }
        pipelineEventDispatcher.dispatch(
            commandContext.event.copy(delayMills = DEFAULT_LOOP_TIME_MILLS, source = commandContext.latestSummary)
        )
        // #5454 增加可视化的互斥状态打印
        if (commandContext.latestSummary == "mutex_print" ||
            commandContext.latestSummary == "agent_reuse_mutex_print"
        ) {
            commandContext.cmdFlowState = CmdFlowState.FINALLY
            with(commandContext.container) {
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStatusBroadCastEvent(
                        source = "container-queue-loop-$containerId", projectId = projectId,
                        pipelineId = pipelineId, userId = commandContext.event.userId,
                        buildId = buildId, taskId = null, actionType = ActionType.START,
                        containerHashId = containerHashId, jobId = jobId, stageId = null,
                        stepId = null, atomCode = null, executeCount = executeCount,
                        buildStatus = BuildStatus.QUEUE.name,
                        type = PipelineBuildStatusBroadCastEventType.BUILD_JOB_QUEUE,
                        labels = mapOf(
                            PipelineBuildStatusBroadCastEvent.Labels::jobMutexType.name to
                                commandContext.latestSummary,
                            PipelineBuildStatusBroadCastEvent.Labels::mutexGroup.name to
                                (controlOption.mutexGroup?.runtimeMutexGroup ?: ""),
                            PipelineBuildStatusBroadCastEvent.Labels::agentReuseMutex.name to
                                (controlOption.agentReuseMutex?.runtimeAgentOrEnvId ?: "")
                        )
                    )
                )
            }
        }
    }
}
