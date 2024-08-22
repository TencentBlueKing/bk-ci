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

package com.tencent.devops.process.engine.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.run.PipelineBuildStartListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildBatchCancelListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildBatchFinishListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildCancelListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildFinishListener
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
 * 流水线构建核心配置
 */
@Configuration
class BuildEngineCoreBuildConfiguration {

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Value("\${queueConcurrency.buildStart:5}")
    private val buildStartConcurrency: Int? = null

    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @Bean
    fun pipelineBuildStartQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_START)

    @Bean
    fun pipelineBuildStartQueueBind(
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildStartQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_START)
    }

    @Bean
    fun pipelineBuildStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildStartListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildStartQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = buildStartConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.buildFinish:5}")
    private val buildFinishConcurrency: Int? = null

    /**
     * 构建结束队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildFinishQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH)

    @Bean
    fun pipelineBuildFinishQueueBind(
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_FINISH)
    }

    @Bean
    fun pipelineBuildFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildFinishQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = buildFinishConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.buildBatchFinish:5}")
    private val buildBatchFinishConcurrency: Int? = null

    /**
     * 构建结束队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildBatchFinishQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_BATCH_FINISH)

    @Bean
    fun pipelineBuildBatchFinishQueueBind(
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_BATCH_FINISH)
    }

    @Bean
    fun pipelineBuildBatchFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildBatchFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildFinishQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = buildBatchFinishConcurrency!!,
            maxConcurrency = 20
        )
    }

    @Value("\${queueConcurrency.buildCancel:5}")
    private val buildCancelConcurrency: Int? = null

    /**
     * 构建取消队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildCancelQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_CANCEL)

    @Bean
    fun pipelineBuildCancelQueueBind(
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildCancelQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_CANCEL)
    }

    @Bean
    fun pipelineBuildCancelListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildCancelListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildCancelQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = buildCancelConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.buildBatchCancel:5}")
    private val buildBatchCancelConcurrency: Int? = null

    /**
     * 构建取消队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildBatchCancelQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_BATCH_CANCEL)

    @Bean
    fun pipelineBuildBatchCancelQueueBind(
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildCancelQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_BATCH_CANCEL)
    }

    @Bean
    fun pipelineBuildBatchCancelListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildBatchCancelListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildCancelQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = buildBatchCancelConcurrency!!,
            maxConcurrency = 20
        )
    }
}
