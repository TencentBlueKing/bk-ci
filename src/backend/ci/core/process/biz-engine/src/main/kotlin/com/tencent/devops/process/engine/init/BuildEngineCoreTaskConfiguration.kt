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

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.run.PipelineAtomTaskBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineTaskPauseListener
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
class BuildEngineCoreTaskConfiguration {

    @Value("\${queueConcurrency.task:10}")
    private val taskConcurrency: Int? = null

    /**
     * 任务队列---- 并发要大
     */
    @Bean
    fun pipelineBuildTaskQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_TASK_START)

    @Bean
    fun pipelineBuildTaskQueueBind(
        @Autowired pipelineBuildTaskQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildTaskQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_TASK_START)
    }

    @Bean
    fun pipelineAtomTaskBuildListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildTaskQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineAtomTaskBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildTaskQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 1,
            concurrency = taskConcurrency!!,
            maxConcurrency = 50
        )
    }
//
//    /**
//     * 本机专属任务队列
//     */
//    @Bean
//    fun localPipelineBuildTaskQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_TASK_START + CommonUtils.getInnerIP())
//
//    @Bean
//    fun localPipelineBuildTaskQueueBind(
//        @Autowired localPipelineBuildTaskQueue: Queue,
//        @Autowired pipelineCoreExchange: DirectExchange
//    ): Binding {
//        return BindingBuilder.bind(localPipelineBuildTaskQueue)
//            .to(pipelineCoreExchange)
//            .with(MQ.ROUTE_PIPELINE_BUILD_TASK_START + CommonUtils.getInnerIP())
//    }

//    @Bean
//    fun localPipelineAtomTaskBuildListenerContainer(
//        @Autowired connectionFactory: ConnectionFactory,
//        @Autowired localPipelineBuildTaskQueue: Queue,
//        @Autowired rabbitAdmin: RabbitAdmin,
//        @Autowired buildListener: PipelineAtomTaskBuildListener,
//        @Autowired messageConverter: Jackson2JsonMessageConverter
//    ): SimpleMessageListenerContainer {
//
//        return Tools.createSimpleMessageListenerContainer(
//            connectionFactory = connectionFactory,
//            queue = localPipelineBuildTaskQueue,
//            rabbitAdmin = rabbitAdmin,
//            buildListener = buildListener,
//            messageConverter = messageConverter,
//            startConsumerMinInterval = 5000,
//            consecutiveActiveTrigger = 1,
//            concurrency = 1,
//            maxConcurrency = 50
//        )
//    }

    /**
     * 流水线暂停操作队列
     */
    @Bean
    fun pipelinePauseTaskExecuteQueue() = Queue(MQ.QUEUE_PIPELINE_PAUSE_TASK_EXECUTE)

    @Bean
    fun pipelinePauseTaskExecuteQueueBind(
        @Autowired pipelinePauseTaskExecuteQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelinePauseTaskExecuteQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_PAUSE_TASK_EXECUTE)
    }

    @Bean
    fun pipelinePauseTaskExecuteListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelinePauseTaskExecuteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineTaskPauseListener: PipelineTaskPauseListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelinePauseTaskExecuteQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pipelineTaskPauseListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = taskConcurrency!!,
            maxConcurrency = 50
        )
    }
}
