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

package com.tencent.devops.dispatch.kubernetes.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_SHUTDOWN
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_AGENT_STARTUP
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.dispatch.kubernetes.listeners.KubernetesListener
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class DispatchKubernetesMQConfiguration @Autowired constructor() {

    @Value("\${dispatch.demoteQueue.concurrency:2}")
    private val demoteQueueConcurrency: Int = 2

    @Value("\${dispatch.demoteQueue.maxConcurrency:2}")
    private val demoteQueueMaxConcurrency: Int = 2

    @Value("\${dispatch.agentStartQueue.concurrency:60}")
    private val agentStartQueueConcurrency: Int = 60

    @Value("\${dispatch.agentStartQueue.maxConcurrency:100}")
    private val agentStartQueueMaxConcurrency: Int = 100

    /**
     * 启动构建队列
     */
    @Bean
    fun buildDispatchKubernetesAgentStartQueue(@Autowired kubernetesListener: KubernetesListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartQueue(kubernetesListener))
    }

    @Bean
    fun buildDispatchKubernetesAgentStartQueueBind(
        @Autowired buildDispatchKubernetesAgentStartQueue: Queue,
        @Autowired dispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildDispatchKubernetesAgentStartQueue).to(dispatchExchange)
            .with(buildDispatchKubernetesAgentStartQueue.name)
    }

    @Bean
    fun startDispatchKubernetesListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildDispatchKubernetesAgentStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired kubernetesListener: KubernetesListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(kubernetesListener, kubernetesListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildDispatchKubernetesAgentStartQueue,
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
    fun buildDispatchKubernetesAgentStartDemoteQueue(@Autowired kubernetesListener: KubernetesListener): Queue {
        return Queue(ROUTE_AGENT_STARTUP + getStartDemoteQueue(kubernetesListener))
    }

    @Bean
    fun buildDispatchKubernetesAgentStartDemoteQueueBind(
        @Autowired buildDispatchKubernetesAgentStartDemoteQueue: Queue,
        @Autowired dispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildDispatchKubernetesAgentStartDemoteQueue).to(dispatchExchange)
            .with(buildDispatchKubernetesAgentStartDemoteQueue.name)
    }

    @Bean
    fun startDispatchKubernetesDemoteListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildDispatchKubernetesAgentStartDemoteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired kubernetesListener: KubernetesListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(kubernetesListener, kubernetesListener::handleStartup.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildDispatchKubernetesAgentStartDemoteQueue,
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
    fun buildDispatchKubernetesAgentShutdownQueue(@Autowired kubernetesListener: KubernetesListener): Queue {
        return Queue(ROUTE_AGENT_SHUTDOWN + getShutdownQueue(kubernetesListener))
    }

    @Bean
    fun buildDispatchKubernetesAgentShutdownQueueBind(
        @Autowired buildDispatchKubernetesAgentShutdownQueue: Queue,
        @Autowired dispatchExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(buildDispatchKubernetesAgentShutdownQueue).to(dispatchExchange)
            .with(buildDispatchKubernetesAgentShutdownQueue.name)
    }

    @Bean
    fun shutdownDispatchKubernetesListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildDispatchKubernetesAgentShutdownQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired kubernetesListener: KubernetesListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(kubernetesListener, kubernetesListener::handleShutdownMessage.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildDispatchKubernetesAgentShutdownQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 60,
            maxConcurrency = 100,
            adapter = adapter,
            prefetchCount = 1
        )
    }

    private fun getStartQueue(kubernetesListener: KubernetesListener): String {
        val startupQueue = kubernetesListener.getStartupQueue()
        logger.info("Get the start up queue ($startupQueue)")
        if (startupQueue.isBlank()) {
            throw RuntimeException("The startup queue is blank")
        }
        return startupQueue
    }

    private fun getStartDemoteQueue(kubernetesListener: KubernetesListener): String {
        val startupDemoteQueue = kubernetesListener.getStartupDemoteQueue()
        logger.info("Get the startupDemoteQueue ($startupDemoteQueue)")
        if (startupDemoteQueue.isBlank()) {
            throw RuntimeException("The startupDemoteQueue is blank")
        }
        return startupDemoteQueue
    }

    private fun getShutdownQueue(kubernetesListener: KubernetesListener): String {
        val shutdownQueue = kubernetesListener.getShutdownQueue()
        logger.info("Get the shutdown queue ($shutdownQueue)")
        if (shutdownQueue.isBlank()) {
            throw RuntimeException("The shutdown queue is blank")
        }
        return shutdownQueue
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchKubernetesMQConfiguration::class.java)
    }
}
