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
package com.tencent.devops.process.trigger.mq.pacTrigger

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PacTriggerMQConfig {

    @Bean
    fun traceEventDispatcher(rabbitTemplate: RabbitTemplate) = TraceEventDispatcher(rabbitTemplate)

    /**
     * pac流水线触发交换机
     */
    @Bean
    fun pacPipelineExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_PAC_PIPELINE_LISTENER, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun pacEnableQueue() = Queue(MQ.QUEUE_PAC_ENABLE_PIPELINE_EVENT)

    @Bean
    fun pacEnableQueueBind(
        @Autowired pacEnableQueue: Queue,
        @Autowired pacPipelineExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pacEnableQueue).to(pacPipelineExchange).with(MQ.ROUTE_PAC_ENABLE_PIPELINE_EVENT)
    }

    @Bean
    fun pacEnableContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pacEnableQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pacExchange: PacTriggerListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pacEnableQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pacExchange,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 20,
            prefetchCount = 1
        )
    }

    @Bean
    fun pacTriggerQueue() = Queue(MQ.QUEUE_PAC_TRIGGER_PIPELINE_EVENT)

    @Bean
    fun pacTriggerQueueBind(
        @Autowired pacTriggerQueue: Queue,
        @Autowired pacPipelineExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pacTriggerQueue).to(pacPipelineExchange).with(MQ.ROUTE_PAC_TRIGGER_PIPELINE_EVENT)
    }

    @Bean
    fun pacTriggerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pacTriggerQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pacTriggerListener: PacTriggerListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pacTriggerQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pacTriggerListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 30,
            maxConcurrency = 50,
            prefetchCount = 1
        )
    }
}
