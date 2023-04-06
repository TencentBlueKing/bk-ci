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

package com.tencent.devops.dispatch.docker.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.Tools
import com.tencent.devops.common.event.dispatcher.mq.MQ
import com.tencent.devops.dispatch.docker.listener.AgentLessListener
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AgentLessMQConfiguration @Autowired constructor() {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    /**
     * 构建无编译构建机启动交换机
     */
    @Bean
    fun agentLessDispatchExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun agentLessDispatchStartQueue() = Queue(MQ.QUEUE_BUILD_LESS_AGENT_STARTUP_DISPATCH)

    @Bean
    fun agentLessDispatchStartQueueBind(
        @Autowired agentLessDispatchStartQueue: Queue,
        @Autowired agentLessDispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(agentLessDispatchStartQueue).to(agentLessDispatchExchange)
            .with(MQ.ROUTE_BUILD_LESS_AGENT_STARTUP_DISPATCH)
    }

    @Bean
    fun agentLessDispatchStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired agentLessDispatchStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired agentLessListener: AgentLessListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(agentLessListener, agentLessListener::listenAgentStartUpEvent.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = agentLessDispatchStartQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 50,
            maxConcurrency = 100,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    @Bean
    fun agentLessDispatchShutdownQueue() = Queue(MQ.QUEUE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH)

    @Bean
    fun agentLessDispatchShutdownQueueBind(
        @Autowired agentLessDispatchShutdownQueue: Queue,
        @Autowired agentLessDispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(agentLessDispatchShutdownQueue).to(agentLessDispatchExchange)
            .with(MQ.ROUTE_BUILD_LESS_AGENT_SHUTDOWN_DISPATCH)
    }

    @Bean
    fun agentLessDispatchShutdownListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired agentLessDispatchShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired agentLessListener: AgentLessListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(agentLessListener, agentLessListener::listenAgentShutdownEvent.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = agentLessDispatchShutdownQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 50,
            maxConcurrency = 100,
            adapter = adapter,
            prefetchCount = 1
        )
    }
}
