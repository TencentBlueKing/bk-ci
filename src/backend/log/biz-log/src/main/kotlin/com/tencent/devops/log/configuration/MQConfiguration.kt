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

package com.tencent.devops.log.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_LOG_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.QUEUE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.QUEUE_LOG_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_LOG_BUILD_EVENT
import com.tencent.devops.log.mq.LogListener
import org.slf4j.LoggerFactory
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
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class MQConfiguration @Autowired constructor() {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun logEventExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LOG_BUILD_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun logBatchEventExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LOG_BATCH_BUILD_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun logEventQueue(): Queue {
        return Queue(QUEUE_LOG_BUILD_EVENT, true)
    }

    @Bean
    fun logBatchEventQueue(): Queue {
        return Queue(QUEUE_LOG_BATCH_BUILD_EVENT, true)
    }

    @Bean
    fun logEventBind(
        @Autowired logEventQueue: Queue,
        @Autowired logEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(logEventQueue).to(logEventExchange).with(ROUTE_LOG_BUILD_EVENT)
    }

    @Bean
    fun logBatchEventBind(
        @Autowired logBatchEventQueue: Queue,
        @Autowired logBatchEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(logBatchEventQueue).to(logBatchEventExchange).with(ROUTE_LOG_BATCH_BUILD_EVENT)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun logEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired logEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logListener: LogListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(logEventQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(logListener, logListener::logEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start log event listener")
        return container
    }

    @Bean
    fun logBatchEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired logBatchEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logListener: LogListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(logBatchEventQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(100)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(logListener, logListener::logBatchEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start log batch event listener")
        return container
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MQConfiguration::class.java)
    }
}
