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

package com.tencent.devops.process.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.PipelineStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class PipelineStatusChangeListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineService: PipelineService,
    private val pipelineBuildService: PipelineBuildService,
    private val objectMapper: ObjectMapper
) : BaseListener<PipelineStatusChangeEvent>(pipelineEventDispatcher) {

    @Autowired
    private val messagingTemplate: SimpMessagingTemplate? = null

    override fun run(event: PipelineStatusChangeEvent) {
        when (event.changeType) {
            ChangeType.STATUS -> {
                val status = pipelineService.getPipelineStatus(event.userId, event.projectId, setOf(event.pipelineId))
                val currentTimestamp = System.currentTimeMillis()
                val result = status.map {
                    it.pipelineId to PipelineStatus(
                        taskCount = it.taskCount,
                        buildCount = it.buildCount,
                        lock = it.lock,
                        canManualStartup = it.canManualStartup,
                        latestBuildStartTime = it.latestBuildStartTime,
                        latestBuildEndTime = it.latestBuildEndTime,
                        latestBuildStatus = it.latestBuildStatus,
                        latestBuildNum = it.latestBuildNum,
                        latestBuildTaskName = it.latestBuildTaskName,
                        latestBuildEstimatedExecutionSeconds = it.latestBuildEstimatedExecutionSeconds,
                        latestBuildId = it.latestBuildId,
                        currentTimestamp = currentTimestamp,
                        runningBuildCount = it.runningBuildCount,
                        hasCollect = it.hasCollect
                    )
                }.toMap()
                messagingTemplate!!.convertAndSend(
                    "/topic/pipelineStatus/${event.projectId}",
                    objectMapper.writeValueAsString(result)
                )
            }
            ChangeType.HISTORY -> {
                messagingTemplate!!.convertAndSend("/topic/pipelineHistory/${event.pipelineId}", event.pipelineId)
            }
            ChangeType.DETAIL -> {
                logger.info("$event")
                val modelDetail = pipelineBuildService.getBuildDetail(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    channelCode = ChannelCode.BS,
                    checkPermission = false
                )
                messagingTemplate!!.convertAndSend(
                    "/topic/pipelineDetail/${event.buildId}",
                    objectMapper.writeValueAsString(modelDetail)
                )
            }
        }
    }
}
