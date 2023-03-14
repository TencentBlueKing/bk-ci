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

package com.tencent.devops.process.websocket

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.websocket.listener.PipelineWebSocketListener
import com.tencent.devops.process.websocket.page.DefaultDetailPageBuild
import com.tencent.devops.process.websocket.page.DefaultHistoryPageBuild
import com.tencent.devops.process.websocket.page.DefaultRecordPageBuild
import com.tencent.devops.process.websocket.page.DefaultStatusPageBuild
import com.tencent.devops.process.websocket.page.GithubDetailPageBuild
import com.tencent.devops.process.websocket.page.GithubHistoryPageBuild
import com.tencent.devops.process.websocket.page.GithubStatusPageBuild
import com.tencent.devops.process.websocket.service.PipelineWebsocketService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * 流水线websocket扩展配置
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineWebSocketConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "process-service"
    }

    /**
     * webhook构建触发广播监听
     */
    @EventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_WEBSOCKET, STREAM_CONSUMER_GROUP)
    fun pipelineWebSocketPushEventListener(
        @Autowired pipelineWebSocketListener: PipelineWebSocketListener
    ): Consumer<Message<PipelineBuildWebSocketPushEvent>> {
        return Consumer { event: Message<PipelineBuildWebSocketPushEvent> ->
            pipelineWebSocketListener.run(event.payload)
        }
    }

    @Bean
    fun pipelineWebSocketListener(
        @Autowired pipelineWebsocketService: PipelineWebsocketService,
        @Autowired webSocketDispatcher: WebSocketDispatcher,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired pipelineInfoFacadeService: PipelineInfoFacadeService
    ) = PipelineWebSocketListener(
        pipelineWebsocketService = pipelineWebsocketService,
        webSocketDispatcher = webSocketDispatcher,
        pipelineEventDispatcher = pipelineEventDispatcher,
        pipelineInfoFacadeService = pipelineInfoFacadeService
    )

    @Bean
    fun websocketDispatcher(streamBridge: StreamBridge) = WebSocketDispatcher(streamBridge)

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "github")
    fun githubDetailPage() = GithubDetailPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "github")
    fun githubHistoryPage() = GithubHistoryPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "github")
    fun githubStatusPage() = GithubStatusPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun defaultHistoryPage() = DefaultHistoryPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun defaultDetailPage() = DefaultDetailPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun defaultRecordPage() = DefaultRecordPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun defaultStatusPage() = DefaultStatusPageBuild()
}
