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

package com.tencent.devops.dispatch.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.dispatch.listener.ThirdPartyBuildListener
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
class DispatchMQConfiguration @Autowired constructor() {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    /**
     * 第三方构建机启动交换机
     */
    @Bean
    fun dispatchExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_AGENT_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun thirdAgentDispatchStartQueue() = Queue(MQ.QUEUE_AGENT_STARTUP)

    @Bean
    fun thirdAgentDispatchStartQueueBind(
        @Autowired thirdAgentDispatchStartQueue: Queue,
        @Autowired dispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(thirdAgentDispatchStartQueue).to(dispatchExchange)
            .with(MQ.ROUTE_AGENT_STARTUP)
    }

    @Bean
    fun thirdAgentDispatchStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired thirdAgentDispatchStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired thirdPartyBuildListener: ThirdPartyBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            thirdPartyBuildListener,
            thirdPartyBuildListener::handleStartup.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = thirdAgentDispatchStartQueue,
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
    fun thirdAgentDispatchShutdownQueue() = Queue(MQ.QUEUE_AGENT_SHUTDOWN)

    @Bean
    fun thirdAgentDispatchShutdownQueueBind(
        @Autowired thirdAgentDispatchShutdownQueue: Queue,
        @Autowired dispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(thirdAgentDispatchShutdownQueue).to(dispatchExchange)
            .with(MQ.ROUTE_AGENT_SHUTDOWN)
    }

    @Bean
    fun thirdAgentDispatchShutdownListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired thirdAgentDispatchShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired thirdPartyBuildListener: ThirdPartyBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            thirdPartyBuildListener,
            thirdPartyBuildListener::handleShutdownMessage.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = thirdAgentDispatchShutdownQueue,
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
