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

package com.tencent.devops.websocket.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.websocket.handler.ConnectChannelInterceptor
import com.tencent.devops.websocket.listener.WebSocketListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.InetAddress

@Component
class WebsocketConfiguration {

    @Value("\${queueConcurrency.pipelineWebSocket:1}")
    private val webSocketQueueConcurrency: Int? = null

    @Value("\${activeTrigger.pipelineWebSocket:10}")
    private val webSocketActiveTrigger: Int? = null

    @Bean
    fun websocketDispatcher(rabbitTemplate: RabbitTemplate) = WebSocketDispatcher(rabbitTemplate)

    /**
     * 构建广播交换机
     */
    @Bean
    fun pipelineWebSocketFanoutExchange(): FanoutExchange {
        return FanoutExchange(MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, true, false)
    }

    @Bean
    fun rabbitAdmin(
        @Autowired connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun pipelineWebSocketQueue(): Queue {
        val address = InetAddress.getLocalHost()
        return Queue(MQ.QUEUE_WEBSOCKET_TMP_EVENT + "." + address.hostAddress)
//        return QueueBuilder.nonDurable().autoDelete().exclusive().build()
    }

    @Bean
    fun pipelineQueueBinding(
        @Autowired pipelineWebSocketQueue: Queue,
        @Autowired pipelineWebSocketFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineWebSocketQueue).to(pipelineWebSocketFanoutExchange)
    }

    @Bean
    fun webSocketListenerContainer(
            @Autowired connectionFactory: ConnectionFactory,
            @Autowired rabbitAdmin: RabbitAdmin,
            @Autowired messageConverter: Jackson2JsonMessageConverter,
            @Autowired pipelineWebSocketQueue: Queue,
            @Autowired buildListener: WebSocketListener
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineWebSocketQueue.name)
        container.setConcurrentConsumers(webSocketQueueConcurrency!!)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(5000)
        container.setConsecutiveActiveTrigger(webSocketActiveTrigger!!)
        container.setMismatchedQueuesFatal(true)
        val adapter = MessageListenerAdapter(buildListener, buildListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    fun connectChannelInterceptor(
        redisOperation: RedisOperation
    ): ConnectChannelInterceptor {
        return ConnectChannelInterceptor(redisOperation)
    }
}