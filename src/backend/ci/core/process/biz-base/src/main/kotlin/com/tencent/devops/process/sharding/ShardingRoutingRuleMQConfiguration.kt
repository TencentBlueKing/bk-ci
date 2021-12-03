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

package com.tencent.devops.process.sharding

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
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

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ShardingRoutingRuleMQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun shardingRoutingRuleCreateQueue() = Queue(MQ.QUEUE_SHARDING_ROUTING_RULE_CREATE_EVENT)

    @Bean
    fun shardingRoutingRuleUpdateQueue() = Queue(MQ.QUEUE_SHARDING_ROUTING_RULE_UPDATE_EVENT)

    @Bean
    fun shardingRoutingRuleDeleteQueue() = Queue(MQ.QUEUE_SHARDING_ROUTING_RULE_DELETE_EVENT)

    @Bean
    fun shardingRoutingRuleCreateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.QUEUE_SHARDING_ROUTING_RULE_CREATE_EVENT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun shardingRoutingRuleUpdateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.QUEUE_SHARDING_ROUTING_RULE_UPDATE_EVENT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun shardingRoutingRuleDeleteExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.QUEUE_SHARDING_ROUTING_RULE_DELETE_EVENT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun shardingRoutingRuleCreateQueueBind(
        @Autowired shardingRoutingRuleCreateQueue: Queue,
        @Autowired shardingRoutingRuleCreateExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(shardingRoutingRuleCreateQueue)
        .to(shardingRoutingRuleCreateExchange)

    @Bean
    fun shardingRoutingRuleUpdateQueueBind(
        @Autowired shardingRoutingRuleUpdateQueue: Queue,
        @Autowired shardingRoutingRuleUpdateExchange: FanoutExchange,
    ): Binding = BindingBuilder.bind(shardingRoutingRuleUpdateQueue)
        .to(shardingRoutingRuleUpdateExchange)

    @Bean
    fun shardingRoutingRuleDeleteQueueBind(
        @Autowired shardingRoutingRuleDeleteQueue: Queue,
        @Autowired shardingRoutingRuleDeleteExchange: FanoutExchange,
    ): Binding = BindingBuilder.bind(shardingRoutingRuleDeleteQueue)
        .to(shardingRoutingRuleDeleteExchange)

    @Bean
    fun shardingRoutingRuleCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired shardingRoutingRuleCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired shardingRoutingRuleListener: ShardingRoutingRuleListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return generateSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queueName = shardingRoutingRuleCreateQueue.name,
            rabbitAdmin = rabbitAdmin,
            shardingRoutingRuleListener = shardingRoutingRuleListener,
            messageConverter = messageConverter
        )
    }

    @Bean
    fun shardingRoutingRuleUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired shardingRoutingRuleUpdateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired shardingRoutingRuleListener: ShardingRoutingRuleListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return generateSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queueName = shardingRoutingRuleUpdateQueue.name,
            rabbitAdmin = rabbitAdmin,
            shardingRoutingRuleListener = shardingRoutingRuleListener,
            messageConverter = messageConverter
        )
    }

    @Bean
    fun shardingRoutingRuleDeleteListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired shardingRoutingRuleDeleteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired shardingRoutingRuleListener: ShardingRoutingRuleListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return generateSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queueName = shardingRoutingRuleDeleteQueue.name,
            rabbitAdmin = rabbitAdmin,
            shardingRoutingRuleListener = shardingRoutingRuleListener,
            messageConverter = messageConverter
        )
    }

    private fun generateSimpleMessageListenerContainer(
        connectionFactory: ConnectionFactory,
        queueName: String?,
        rabbitAdmin: RabbitAdmin,
        shardingRoutingRuleListener: ShardingRoutingRuleListener,
        messageConverter: Jackson2JsonMessageConverter,
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(queueName)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(shardingRoutingRuleListener, shardingRoutingRuleListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
