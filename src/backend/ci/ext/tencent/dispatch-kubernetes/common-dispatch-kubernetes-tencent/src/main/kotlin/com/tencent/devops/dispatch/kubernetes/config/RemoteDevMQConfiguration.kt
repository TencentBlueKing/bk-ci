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

package com.tencent.devops.dispatch.kubernetes.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.remotedev.MQ.EXCHANGE_REMOTE_DEV_LISTENER_DIRECT
import com.tencent.devops.common.remotedev.MQ.QUEUE_WORKSPACE_CREATE_STARTUP
import com.tencent.devops.common.remotedev.MQ.QUEUE_WORKSPACE_OPERATE_STARTUP
import com.tencent.devops.common.remotedev.MQ.ROUTE_WORKSPACE_CREATE_STARTUP
import com.tencent.devops.common.remotedev.MQ.ROUTE_WORKSPACE_OPERATE_STARTUP
import com.tencent.devops.dispatch.kubernetes.listener.WorkspaceListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RemoteDevMQConfiguration @Autowired constructor() {

    @Value("\${dispatch.agentStartQueue.concurrency:60}")
    private val agentStartQueueConcurrency: Int = 60

    @Value("\${dispatch.agentStartQueue.maxConcurrency:100}")
    private val agentStartQueueMaxConcurrency: Int = 100

/*    @Bean
    @ConditionalOnMissingBean(RabbitAdmin::class)
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }*/

    @Bean
    fun exchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_REMOTE_DEV_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    /**
     * 启动构建队列
     */
    @Bean
    fun buildWorkspaceCreateStartQueue(): Queue {
        return Queue(QUEUE_WORKSPACE_CREATE_STARTUP)
    }

    @Bean
    fun buildWorkspaceCreateQueueBind(
        @Autowired buildWorkspaceCreateStartQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildWorkspaceCreateStartQueue).to(exchange).with(ROUTE_WORKSPACE_CREATE_STARTUP)
    }

 /*   @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)*/

    @Bean
    fun workspaceCreateListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildWorkspaceCreateStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired workspaceListener: WorkspaceListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(workspaceListener, workspaceListener::handleWorkspaceCreate.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildWorkspaceCreateStartQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = agentStartQueueConcurrency,
            maxConcurrency = agentStartQueueMaxConcurrency,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    @Bean
    fun buildWorkspaceOperateQueue(): Queue {
        return Queue(QUEUE_WORKSPACE_OPERATE_STARTUP)
    }

    @Bean
    fun buildWorkspaceOperateQueueBind(
        @Autowired buildWorkspaceOperateQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildWorkspaceOperateQueue).to(exchange).with(ROUTE_WORKSPACE_OPERATE_STARTUP)
    }

    @Bean
    fun workspaceOperateListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildWorkspaceOperateQueueBind: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired workspaceListener: WorkspaceListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(workspaceListener, workspaceListener::handleWorkspaceOperate.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildWorkspaceOperateQueueBind,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 60,
            maxConcurrency = 100,
            adapter = adapter,
            prefetchCount = 1
        )
    }
}
