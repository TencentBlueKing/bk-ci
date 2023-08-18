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

package com.tencent.devops.process.notify

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线扩展通知配置
 */
@Configuration
class PipelineExtendsNotifyConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["pipelineMonitorExchange"])
    fun pipelineMonitorExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_PIPELINE_MONITOR_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun pipelineBuildNotifyQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_NOTIFY)
    }

    @Bean
    fun pipelineBuildNotifyQueueBind(
        @Autowired pipelineBuildNotifyQueue: Queue,
        @Autowired pipelineMonitorExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildNotifyQueue).to(pipelineMonitorExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_NOTIFY)
    }

    @Bean
    fun pipelineBuildNotifyListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildNotifyQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineBuildNotifyListener: PipelineBuildNotifyListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildNotifyQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pipelineBuildNotifyListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 10
        )
    }

    @Bean
    fun pipelineBuildNotifyReminderQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_REVIEW_REMINDER)
    }

    @Bean
    fun pipelineBuildNotifyReminderQueueBind(
        @Autowired pipelineBuildNotifyReminderQueue: Queue,
        @Autowired pipelineMonitorExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildNotifyReminderQueue).to(pipelineMonitorExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_REVIEW_REMINDER)
    }

    @Bean
    fun pipelineBuildNotifyReminderListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildNotifyReminderQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineAtomTaskReminderListener: PipelineAtomTaskReminderListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildNotifyReminderQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pipelineAtomTaskReminderListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 10
        )
    }
}
