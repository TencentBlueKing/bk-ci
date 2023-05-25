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

package com.tencent.devops.common.dispatch.sdk.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_AGENT_LISTENER_DIRECT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_SHUTDOWN
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_STARTUP
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class MQConfiguration @Autowired constructor() {

    @Value("\${dispatch.demoteQueue.concurrency:2}")
    private val demoteQueueConcurrency: Int = 2

    @Value("\${dispatch.demoteQueue.maxConcurrency:2}")
    private val demoteQueueMaxConcurrency: Int = 2

    @Value("\${dispatch.agentStartQueue.concurrency:60}")
    private val agentStartQueueConcurrency: Int = 60

    @Value("\${dispatch.agentStartQueue.maxConcurrency:100}")
    private val agentStartQueueMaxConcurrency: Int = 100

    @Bean
    @ConditionalOnMissingBean(RabbitAdmin::class)
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
    fun pipelineBuildDispatchStartQueue(@Autowired buildListener: BuildListener) =
        Queue(MQ.QUEUE_PIPELINE_BUILD_START_DISPATCHER + getStartQueue(buildListener))

    @Bean
    fun pipelineBuildDispatchStartQueueBind(
        @Autowired pipelineBuildDispatchStartQueue: Queue,
        @Autowired pipelineBuildStartFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildDispatchStartQueue).to(pipelineBuildStartFanoutExchange)
    }

    @Bean
    fun pipelineBuildDispatchStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildDispatchStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(buildListener, buildListener::onPipelineStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = pipelineBuildDispatchStartQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 10,
            adapter = adapter,
            prefetchCount = 1
        )
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
    fun pipelineBuildDispatchFinishQueue(@Autowired buildListener: BuildListener) =
        Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_DISPATCHER + getShutdownQueue(buildListener))

    @Bean
    fun pipelineBuildDispatchFinishQueueBind(
        @Autowired pipelineBuildDispatchFinishQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildDispatchFinishQueue).to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildDispatchFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildDispatchFinishQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(buildListener, buildListener::onPipelineShutdown.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = pipelineBuildDispatchFinishQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 10,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    @Bean
    fun exchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_AGENT_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    /**
     * 启动构建队列
     */
    @Bean
    fun buildAgentStartQueue(@Autowired buildListener: BuildListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartQueue(buildListener))
    }

    @Bean
    fun buildAgentStartQueueBind(
        @Autowired buildAgentStartQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildAgentStartQueue).to(exchange).with(buildAgentStartQueue.name)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun startListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildAgentStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(buildListener, buildListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildAgentStartQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = agentStartQueueConcurrency,
            maxConcurrency = agentStartQueueMaxConcurrency,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    /**
     * 啓動构建降级队列
     */
    @Bean
    fun buildAgentStartDemoteQueue(@Autowired buildListener: BuildListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartDemoteQueue(buildListener))
    }

    @Bean
    fun buildAgentStartDemoteQueueBind(
        @Autowired buildAgentStartDemoteQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildAgentStartDemoteQueue).to(exchange).with(buildAgentStartDemoteQueue.name)
    }

    @Bean
    fun startDemoteListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildAgentStartDemoteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(buildListener, buildListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildAgentStartDemoteQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = demoteQueueConcurrency,
            maxConcurrency = demoteQueueMaxConcurrency,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    @Bean
    fun buildAgentShutdownQueue(@Autowired buildListener: BuildListener): Queue {
        return Queue(ROUTE_AGENT_SHUTDOWN + getShutdownQueue(buildListener))
    }

    @Bean
    fun buildAgentShutdownQueueBind(
        @Autowired buildAgentShutdownQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildAgentShutdownQueue).to(exchange).with(buildAgentShutdownQueue.name)
    }

    @Bean
    fun shutdownListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildAgentShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: BuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(buildListener, buildListener::handleShutdownMessage.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildAgentShutdownQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 60,
            maxConcurrency = 100,
            adapter = adapter,
            prefetchCount = 1
        )
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

    private fun getStartDemoteQueue(buildListener: BuildListener): String {
        val startupDemoteQueue = buildListener.getStartupDemoteQueue()
        logger.info("Get the startupDemoteQueue ($startupDemoteQueue)")
        if (startupDemoteQueue.isBlank()) {
            throw RuntimeException("The startupDemoteQueue is blank")
        }
        return startupDemoteQueue
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
