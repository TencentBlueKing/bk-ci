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

package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.run.monitor.PipelineBuildHeartbeatListener
import com.tencent.devops.process.engine.listener.run.monitor.PipelineBuildMonitorListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线监控配置
 */
@Configuration
class PipelineMonitorConfiguration {

    @Bean
    fun pipelineMonitorExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_PIPELINE_MONITOR_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Value("\${queueConcurrency.monitor:2}")
    private val monitorConcurrency: Int? = null

    /**
     * 监控队列--- 并发可小
     */
    @Bean
    fun pipelineBuildMonitorQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_MONITOR)

    @Bean
    fun pipelineBuildMonitorQueueBind(
        @Autowired pipelineBuildMonitorQueue: Queue,
        @Autowired pipelineMonitorExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildMonitorQueue)
            .to(pipelineMonitorExchange).with(MQ.ROUTE_PIPELINE_BUILD_MONITOR)
    }

    @Bean
    fun pipelineBuildMonitorListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildMonitorQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildMonitorListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildMonitorQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 60000,
            consecutiveActiveTrigger = 10,
            concurrency = monitorConcurrency!!,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.heartBeat:3}")
    private val heartBeatConcurrency: Int? = null

    /**
     * 心跳监听队列--- 并发可小
     */
    @Bean
    fun pipelineBuildHeartBeatQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_HEART_BEAT)

    @Bean
    fun pipelineBuildHeartBeatQueueBind(
        @Autowired pipelineBuildHeartBeatQueue: Queue,
        @Autowired pipelineMonitorExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildHeartBeatQueue)
            .to(pipelineMonitorExchange).with(MQ.ROUTE_PIPELINE_BUILD_HEART_BEAT)
    }

    @Bean
    fun pipelineBuildHeartBeatListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildHeartBeatQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: PipelineBuildHeartbeatListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildHeartBeatQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 120000,
            consecutiveActiveTrigger = 10,
            concurrency = heartBeatConcurrency!!,
            maxConcurrency = 10
        )
    }
}