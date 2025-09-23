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

package com.tencent.devops.process.websocket.listener

import com.tencent.devops.common.event.listener.pipeline.PipelineEventListener
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.websocket.service.PipelineWebsocketService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineWebSocketListener @Autowired constructor(
    private val pipelineWebsocketService: PipelineWebsocketService,
    private val webSocketDispatcher: WebSocketDispatcher,
    pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService
) : PipelineEventListener<PipelineBuildWebSocketPushEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildWebSocketPushEvent) {
        val channelCode = pipelineInfoFacadeService.getPipelineChannel(event.projectId, event.pipelineId)
        // 非页面类的流水线,直接返回。 不占用redis资源
        if (channelCode != null && !ChannelCode.webChannel(channelCode)) return

        when {
            event.refreshTypes.contains(RefreshType.HISTORY) -> dispatchHistoryMessage(event)
            event.refreshTypes.contains(RefreshType.STATUS) -> dispatchStatusMessage(event)
            event.refreshTypes.contains(RefreshType.RECORD) -> dispatchRecordMessage(event)
        }
    }

    private fun dispatchHistoryMessage(event: PipelineBuildWebSocketPushEvent) {
        val message = pipelineWebsocketService.buildHistoryMessage(
            buildId = event.buildId,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            userId = event.userId
        )
        webSocketDispatcher.dispatch(message)
    }

    private fun dispatchStatusMessage(event: PipelineBuildWebSocketPushEvent) {
        val message = pipelineWebsocketService.buildStatusMessage(
            buildId = event.buildId,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            userId = event.userId
        )
        webSocketDispatcher.dispatch(message)
    }

    private fun dispatchRecordMessage(event: PipelineBuildWebSocketPushEvent) {
        // #8955 增加对没有执行次数的默认页面的重复推送
        val buildId = event.buildId
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val userId = event.userId
        val events = listOfNotNull(
            event.executeCount?.let {
                pipelineWebsocketService.buildRecordMessage(
                    buildId = buildId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    executeCount = it
                )
            },
            // 始终推送 executeCount = null 的消息（兼容默认进入的没带executeCount参数的页面）
            pipelineWebsocketService.buildRecordMessage(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                executeCount = null
            )
        ).toTypedArray()
        webSocketDispatcher.dispatch(*events)
    }

    // 为 RefreshType 添加扩展函数，简化位运算检查
    fun Long.contains(refreshType: RefreshType): Boolean {
        return this and refreshType.binary == refreshType.binary
    }
}
