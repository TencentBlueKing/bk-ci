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

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.DispatchQueueControl
import com.tencent.devops.process.engine.control.MutexControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.utils.BuildUtils
import com.tencent.devops.process.util.TaskUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UpdateStateContainerCmdFinally(
    private val mutexControl: MutexControl,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchQueueControl: DispatchQueueControl,
    private val redisOperation: RedisOperation
) : ContainerCmd {
    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.FINALLY && !commandContext.container.status.isFinish()
    }

    override fun execute(commandContext: ContainerContext) {
        // 更新状态模型
        updateContainerStatus(commandContext = commandContext)

        // 结束时才会释放锁定及返回
        if (commandContext.buildStatus.isFinish()) {
            // 释放互斥组
            mutexRelease(commandContext = commandContext)
            // 释放互斥组
            dispatchDequeue(commandContext = commandContext)
            // 释放redis中取消任务的缓存
            canceledTaskCacheRelease(commandContext = commandContext)
        }
        // 发送回Stage
        if (commandContext.buildStatus.isFinish() || commandContext.buildStatus == BuildStatus.UNKNOWN) {
            val source = commandContext.event.source
            val buildId = commandContext.container.buildId
            val stageId = commandContext.container.stageId
            val containerId = commandContext.container.containerId
            val matrixGroupId = commandContext.container.matrixGroupId
            LOG.info(
                "ENGINE|$buildId|$source|CONTAINER_FIN|$stageId|j($containerId)|" +
                    "matrixGroupId=$matrixGroupId|${commandContext.buildStatus}|${commandContext.latestSummary}"
            )
            // #4518 如果该容器不属于某个矩阵时上报stage处理，否则上报发出一个矩阵组事件
            if (matrixGroupId.isNullOrBlank()) {
                sendBackStage(commandContext = commandContext)
            } else {
                pipelineEventDispatcher.dispatch(
                    commandContext.event.copy(
                        actionType = ActionType.REFRESH,
                        containerId = matrixGroupId,
                        containerHashId = null,
                        source = commandContext.latestSummary,
                        reason = "Matrix(${commandContext.container.containerId}) inner container finished"
                    )
                )
            }
        }
    }

    /**
     * 释放[commandContext]中指定的互斥组
     */
    private fun mutexRelease(commandContext: ContainerContext) {
        commandContext.container.controlOption.mutexGroup?.let { mutexGroup ->
            mutexControl.releaseContainerMutex(
                projectId = commandContext.event.projectId,
                pipelineId = commandContext.event.pipelineId,
                buildId = commandContext.event.buildId,
                stageId = commandContext.event.stageId,
                containerId = commandContext.event.containerId,
                mutexGroup = mutexGroup,
                executeCount = commandContext.container.executeCount
            )
        }
        // 返回stage的时候，需要解锁
    }

    /**
     * 清除[commandContext]中在调度队列中的对象
     */
    private fun dispatchDequeue(commandContext: ContainerContext) {
        // 返回stage的时候，需要解锁
        dispatchQueueControl.dequeueDispatch(commandContext.container)
    }

    /**
     * 清除redis中取消任务的缓存
     */
    private fun canceledTaskCacheRelease(commandContext: ContainerContext) {
        // 清除redis中取消任务的缓存
        val buildId = commandContext.event.buildId
        val containerId = commandContext.event.containerId
        redisOperation.delete(BuildUtils.getCancelActionBuildKey(buildId))
        redisOperation.delete(TaskUtils.getCancelTaskIdRedisKey(buildId, containerId, false))
        redisOperation.delete(TaskUtils.getCancelTaskIdRedisKey(buildId, containerId))
    }

    /**
     * 更新[commandContext]下指定的Container的状态以及编排模型状态
     */
    private fun updateContainerStatus(commandContext: ContainerContext) {
        val event = commandContext.event
        val buildStatus = commandContext.buildStatus

        var startTime: LocalDateTime? = null
        if (buildStatus == BuildStatus.SKIP || commandContext.container.status.isReadyToRun()) {
            startTime = LocalDateTime.now()
        }

        var endTime: LocalDateTime? = null
        if (buildStatus.isFinish()) {
            endTime = LocalDateTime.now()
        }

        if (buildStatus == BuildStatus.SKIP) {
            commandContext.containerTasks.forEach { task ->
                pipelineTaskService.updateTaskStatus(task = task, userId = task.starter, buildStatus = buildStatus)
            }
            // 刷新Model状态为SKIP，包含containerId下的所有插件任务
            containerBuildRecordService.containerSkip(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                containerId = event.containerId,
                executeCount = commandContext.executeCount
            )
        } else if (commandContext.container.status.isReadyToRun() || buildStatus.isFinish()) {
            // 刷新Model状态-仅更新container状态
            containerBuildRecordService.updateContainerStatus(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                containerId = event.containerId,
                buildStatus = buildStatus,
                executeCount = commandContext.executeCount,
                operation = "updateContainerCmdFinally#${event.containerId}"
            )
        }

        pipelineContainerService.updateContainerStatus(
            projectId = event.projectId,
            buildId = event.buildId,
            stageId = event.stageId,
            containerId = event.containerId,
            buildStatus = buildStatus,
            controlOption = commandContext.needUpdateControlOption,
            startTime = startTime,
            endTime = endTime
        )
    }

    /**
     * 将[commandContext]中获得指定stageId,并发送Stage事件回stage
     */
    private fun sendBackStage(commandContext: ContainerContext) {
        with(commandContext.event) {
            val executeCount = commandContext.executeCount
            pipelineEventDispatcher.dispatch(
                PipelineBuildStageEvent(
                    source = "From_j($containerId)",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    stageId = stageId,
                    actionType = ActionType.REFRESH
                )
            )

            buildLogPrinter.addLine(
                buildId = buildId,
                message = "[$executeCount]| Finish Job#${this.containerId}| ${commandContext.latestSummary}",
                tag = VMUtils.genStartVMTaskId(containerId),
                jobId = containerHashId ?: "",
                executeCount = executeCount
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UpdateStateContainerCmdFinally::class.java)
    }
}
