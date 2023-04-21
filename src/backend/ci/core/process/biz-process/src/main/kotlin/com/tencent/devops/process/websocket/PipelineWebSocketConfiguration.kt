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

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.process.websocket.listener.PipelineWebSocketListener
import com.tencent.devops.process.websocket.page.DefaultDetailPageBuild
import com.tencent.devops.process.websocket.page.DefaultHistoryPageBuild
import com.tencent.devops.process.websocket.page.DefaultRecordPageBuild
import com.tencent.devops.process.websocket.page.DefaultStatusPageBuild
import com.tencent.devops.process.websocket.page.GithubDetailPageBuild
import com.tencent.devops.process.websocket.page.GithubHistoryPageBuild
import com.tencent.devops.process.websocket.page.GithubStatusPageBuild
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 流水线websocket扩展配置
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineWebSocketConfiguration {

    @Bean
    fun webSocketDispatcher(@Autowired rabbitTemplate: RabbitTemplate) = WebSocketDispatcher(rabbitTemplate)

    @Bean
    @ConditionalOnMissingBean(name = ["pipelineMonitorExchange"])
    fun pipelineMonitorExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_PIPELINE_MONITOR_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun pipelineBuildWebSocketQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_WEBSOCKET)
    }

    @Bean
    fun pipelineBuildWebSocketQueueBind(
        @Autowired pipelineBuildWebSocketQueue: Queue,
        @Autowired pipelineMonitorExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildWebSocketQueue)
            .to(pipelineMonitorExchange).with(MQ.ROUTE_PIPELINE_BUILD_WEBSOCKET)
    }

    @Bean
    fun pipelineWebSocketListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildWebSocketQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineWebSocketListener: PipelineWebSocketListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildWebSocketQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pipelineWebSocketListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 20
        )
    }

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
