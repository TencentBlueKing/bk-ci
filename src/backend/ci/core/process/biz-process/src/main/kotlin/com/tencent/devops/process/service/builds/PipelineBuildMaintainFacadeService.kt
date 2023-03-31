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

package com.tencent.devops.process.service.builds

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildMonitorEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 *
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelineBuildMaintainFacadeService(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelinePermissionService: PipelinePermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildMaintainFacadeService::class.java)
    }

    fun tryFinishStuckBuilds(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildIds: Set<String>,
        checkPermission: Boolean = true
    ): Boolean {
        if (buildIds.isEmpty()) {
            return false
        }

        if (checkPermission) { // 不用校验查看权限，只校验执行权限
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE,
                message = "用户（$userId) 无权限操作流水线($pipelineId)" // 这里不用设置message，会被里面替换
            )
        }

        logger.info("tryFinishStuckBuilds_start|ids=$buildIds")

        buildIds.forEach next@{ buildId ->
            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId)
            if (buildInfo == null || buildInfo.isFinish()) {
                logger.info("tryFinishStuckBuilds_| $buildId not found!")
                return@next
            }
            var executeCount = 0
            pipelineContainerService.listContainers(projectId, buildId).forEach { container ->
                if (container.status.isRunning()) {
                    container.tryFinishStuckContainer(userId = userId)

                    if (executeCount == 0) {
                        executeCount = container.executeCount
                    }
                }
            }

            // 兜底，可能整体监控已经失效/丢失，增加监控对长时间（过期）构建进行监控
            pipelineEventDispatcher.dispatch(
                PipelineBuildMonitorEvent(
                    source = "tryFinishStuckBuilds_Monitor_${buildId}_$userId",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    executeCount = executeCount,
                    delayMills = 0
                )
            )
        }

        return true
    }

    private fun PipelineBuildContainer.tryFinishStuckContainer(userId: String) {
        logger.warn("BKSystemMonitor|tryFinishStuckBuilds_${buildId}_$containerId")

        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "tryFinishStuckBuilds_${buildId}_${containerId}_$userId",
                containerId = containerId,
                containerHashId = containerHashId,
                stageId = stageId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = userId,
                projectId = projectId,
                actionType = ActionType.REFRESH,
                containerType = containerType
            )
        )

        tryFinishStuckContainerTask(userId = userId)
    }

    private fun PipelineBuildContainer.tryFinishStuckContainerTask(userId: String) {
        pipelineTaskService.listContainerBuildTasks(projectId, buildId, containerId).forEach { task ->
            if (task.status.isRunning()) {
                logger.warn("BKSystemMonitor|tryFinishStuckBuilds_${buildId}_${containerId}_${task.taskId}")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildAtomTaskEvent(
                        source = "tryFinishStuckBuilds_${buildId}_${containerId}_${task.taskId}", // 来源
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = userId,
                        buildId = buildId,
                        stageId = task.stageId,
                        containerId = task.containerId,
                        containerHashId = task.containerHashId,
                        containerType = task.containerType,
                        taskId = task.taskId,
                        taskParam = task.taskParams,
                        actionType = ActionType.REFRESH,
                        executeCount = task.executeCount ?: 1
                    )
                )
            }
        }
    }
}
