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

package com.tencent.devops.dispatch.docker.sdk.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_AGENT_LISTENER_DIRECT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_SHUTDOWN
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_STARTUP
import com.tencent.devops.dispatch.docker.sdk.listener.BuildListener
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class MQConfiguration @Autowired constructor() {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    /**
     * 构建启动广播交换机
     */
    @Bean
    fun pipelineBuildStartFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_START_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildStartQueue(@Autowired buildListener: BuildListener) = Queue(MQ.QUEUE_PIPELINE_BUILD_START_DISPATCHER + getStartQueue(buildListener))

    @Bean
    fun pipelineBuildStartQueueBind(
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired pipelineBuildStartFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildStartQueue).to(pipelineBuildStartFanoutExchange)
    }

    @Bean
    fun pipelineBuildStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineBuildStartQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(buildListener, buildListener::onPipelineStartup.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    /**
     * 构建结束广播交换机
     */
    @Bean
    fun pipelineBuildFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildFinishQueue(@Autowired buildListener: BuildListener) = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_DISPATCHER + getShutdownQueue(buildListener))

    @Bean
    fun pipelineBuildFinishQueueBind(
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishQueue).to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineBuildFinishQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(buildListener, buildListener::onPipelineShutdown.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    fun exchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_AGENT_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun buildStartQueue(
        @Autowired buildListener: BuildListener
    ): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartQueue(buildListener))
    }

    @Bean
    fun buildStartBind(@Autowired buildStartQueue: Queue, @Autowired exchange: DirectExchange): Binding {
        return BindingBuilder.bind(buildStartQueue).to(exchange).with(buildStartQueue.name)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun startListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        logger.info("Start listener ====== ")
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildStartQueue.name)
        container.setConcurrentConsumers(20)
        container.setMaxConcurrentConsumers(100)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(buildListener, buildListener::handleStartMessage.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start listener")
        return container
    }

    @Bean
    fun buildShutdownQueue(@Autowired buildListener: BuildListener): Queue {
        return Queue(ROUTE_AGENT_SHUTDOWN + getShutdownQueue(buildListener))
    }

    @Bean
    fun buildShutdownBind(@Autowired buildShutdownQueue: Queue, @Autowired exchange: DirectExchange): Binding {
        return BindingBuilder.bind(buildShutdownQueue).to(exchange).with(buildShutdownQueue.name)
    }

    @Bean
    fun shutdownListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildShutdownQueue.name)
        container.setConcurrentConsumers(20)
        container.setMaxConcurrentConsumers(100)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(buildListener, buildListener::handleShutdownMessage.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start shutdown listener")
        return container
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MQConfiguration::class.java)
    }

    private fun getStartQueue(buildListener: BuildListener): String {
        val startupQueue = buildListener.getStartupQueue()
        logger.info("Get the start up queue ($startupQueue)")
        if (startupQueue.isBlank()) {
            throw RuntimeException("The startup queue is blank")
        }
        return startupQueue
    }

    private fun getShutdownQueue(buildListener: BuildListener): String {
        val shutdownQueue = buildListener.getShutdownQueue()
        logger.info("Get the shutdown queue ($shutdownQueue)")
        if (shutdownQueue.isBlank()) {
            throw RuntimeException("The shutdown queue is blank")
        }
        return shutdownQueue
    }
}
