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

package com.tencent.devops.dispatch.pcg.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_SHUTDOWN
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_STARTUP
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.dispatch.pcg.listener.PCGBuildListener
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PCGMQConfiguration @Autowired constructor() {

    /**
     * 启动构建队列
     */
    @Bean
    fun buildPCGAgentStartQueue(@Autowired pcgBuildListener: PCGBuildListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartQueue(pcgBuildListener))
    }

    @Bean
    fun buildPCGAgentStartQueueBind(
        @Autowired buildPCGAgentStartQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildPCGAgentStartQueue).to(exchange).with(buildPCGAgentStartQueue.name)
    }

    @Bean
    fun startPCGListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildPCGAgentStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pcgBuildListener: PCGBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(pcgBuildListener, pcgBuildListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildPCGAgentStartQueue,
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
    fun buildPCGAgentStartDemoteQueue(@Autowired pcgBuildListener: PCGBuildListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartDemoteQueue(pcgBuildListener))
    }

    @Bean
    fun buildPCGAgentStartDemoteQueueBind(
        @Autowired buildPCGAgentStartDemoteQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildPCGAgentStartDemoteQueue).to(exchange).with(buildPCGAgentStartDemoteQueue.name)
    }

    @Bean
    fun startPCGDemoteListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildPCGAgentStartDemoteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pcgBuildListener: PCGBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(pcgBuildListener, pcgBuildListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildPCGAgentStartDemoteQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = demoteQueueConcurrency,
            maxConcurrency = demoteQueueMaxConcurrency,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    /**
     * 启动构建结束队列
     */
    @Bean
    fun buildPCGAgentShutdownQueue(@Autowired pcgBuildListener: PCGBuildListener): Queue {
        return Queue(ROUTE_AGENT_SHUTDOWN + getShutdownQueue(pcgBuildListener))
    }

    @Bean
    fun buildPCGAgentShutdownQueueBind(
        @Autowired buildPCGAgentShutdownQueue: Queue,
        @Autowired exchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildPCGAgentShutdownQueue).to(exchange).with(buildPCGAgentShutdownQueue.name)
    }

    @Bean
    fun shutdownPCGListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildPCGAgentShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pcgBuildListener: PCGBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(pcgBuildListener, pcgBuildListener::handleShutdownMessage.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildPCGAgentShutdownQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 20,
            maxConcurrency = 30,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    private fun getStartQueue(pcgBuildListener: PCGBuildListener): String {
        val startupQueue = pcgBuildListener.getStartupQueue()
        logger.info("Get the start up queue ($startupQueue)")
        if (startupQueue.isBlank()) {
            throw RuntimeException("The startup queue is blank")
        }
        return startupQueue
    }

    private fun getStartDemoteQueue(pcgBuildListener: PCGBuildListener): String {
        val startupDemoteQueue = pcgBuildListener.getStartupDemoteQueue()
        logger.info("Get the startupDemoteQueue ($startupDemoteQueue)")
        if (startupDemoteQueue.isBlank()) {
            throw RuntimeException("The startupDemoteQueue is blank")
        }
        return startupDemoteQueue
    }

    private fun getShutdownQueue(pcgBuildListener: PCGBuildListener): String {
        val shutdownQueue = pcgBuildListener.getShutdownQueue()
        logger.info("Get the shutdown queue ($shutdownQueue)")
        if (shutdownQueue.isBlank()) {
            throw RuntimeException("The shutdown queue is blank")
        }
        return shutdownQueue
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PCGMQConfiguration::class.java)

        private const val demoteQueueConcurrency = 2

        private const val demoteQueueMaxConcurrency = 2

        private const val agentStartQueueConcurrency = 20

        private const val agentStartQueueMaxConcurrency = 30
    }
}
