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

package com.tencent.devops.process.websocket.listener

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.enums.ChannelCode
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
) : BaseListener<PipelineBuildWebSocketPushEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildWebSocketPushEvent) {

        val channelCode = pipelineInfoFacadeService.getPipelineChannel(event.projectId, event.pipelineId)
        // 非页面类的流水线,直接返回。 不占用redis资源
        if (channelCode != null && !ChannelCode.webChannel(channelCode)) {
            return
        }

        if (event.refreshTypes and RefreshType.HISTORY.binary == RefreshType.HISTORY.binary) {
            webSocketDispatcher.dispatch(
                pipelineWebsocketService.buildHistoryMessage(
                    buildId = event.buildId,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId
                )
            )
        }

        if (event.refreshTypes and RefreshType.DETAIL.binary == RefreshType.DETAIL.binary) {
            webSocketDispatcher.dispatch(
                pipelineWebsocketService.buildDetailMessage(
                    buildId = event.buildId,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId
                )
            )
        }

        if (event.refreshTypes and RefreshType.STATUS.binary == RefreshType.STATUS.binary) {
            webSocketDispatcher.dispatch(
                pipelineWebsocketService.buildStatusMessage(
                    buildId = event.buildId,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId
                )
            )
        }

        if (event.refreshTypes and RefreshType.RECORD.binary == RefreshType.RECORD.binary) {
            event.executeCount?.let { executeCount ->
                webSocketDispatcher.dispatch(
                    // #8955 增加对没有执行次数的默认页面的重复推送
                    pipelineWebsocketService.buildRecordMessage(
                        buildId = event.buildId,
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        userId = event.userId,
                        executeCount = executeCount
                    ),
                    pipelineWebsocketService.buildRecordMessage(
                        buildId = event.buildId,
                        projectId = event.projectId,
                        pipelineId = event.pipelineId,
                        userId = event.userId,
                        executeCount = null
                    )
                )
            }
        }
    }
}
