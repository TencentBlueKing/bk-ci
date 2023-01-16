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

package com.tencent.devops.project.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.project.listener.ProjectEventListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
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

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ProjectMQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun projectCreateQueue() = Queue(MQ.QUEUE_PROJECT_CREATE_EVENT)

    @Bean
    fun projectUpdateQueue() = Queue(MQ.QUEUE_PROJECT_UPDATE_EVENT)

    @Bean
    fun projectUpdateLogoQueue() = Queue(MQ.QUEUE_PROJECT_UPDATE_LOGO_EVENT)

    @Bean
    fun projectCreateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_CREATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectUpdateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_UPDATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectUpdateLogoExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_UPDATE_LOGO_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectCreateQueueBind(
        @Autowired projectCreateQueue: Queue,
        @Autowired projectCreateExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectCreateQueue)
        .to(projectCreateExchange)

    @Bean
    fun projectUpdateQueueBind(
        @Autowired projectUpdateQueue: Queue,
        @Autowired projectUpdateExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectUpdateQueue)
        .to(projectUpdateExchange)

    @Bean
    fun projectUpdateLogoQueueBind(
        @Autowired projectUpdateLogoQueue: Queue,
        @Autowired projectUpdateLogoExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectUpdateLogoQueue)
        .to(projectUpdateLogoExchange)

    @Bean
    fun projectCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectCreateQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun projectUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectUpdateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectUpdateQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun projectUpdateLogoListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectUpdateLogoQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectUpdateLogoQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
