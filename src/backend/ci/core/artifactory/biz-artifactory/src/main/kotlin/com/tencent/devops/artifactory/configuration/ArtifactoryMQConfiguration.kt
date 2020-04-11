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

package com.tencent.devops.artifactory.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.pipeline.listener.PipelineHardDeleteMQListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建扩展配置
 */
@Configuration
class ArtifactoryMQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Value("\${queueConcurrency.artifactory:3}")
    private val artifactoryConcurrency: Int? = null

    @Bean
    fun pipelineHardDeleteArtifactoryQueue() = Queue(MQ.QUEUE_PIPELINE_HARD_DELETE_ARTIFACTORY)

    /**
     * 流水线硬删除广播交换机
     */
    @Bean
    fun pipelineHardDeleteFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_HARD_DELETE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineHardDeleteArtifactoryQueueBind(
        @Autowired pipelineHardDeleteArtifactoryQueue: Queue,
        @Autowired pipelineHardDeleteFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineHardDeleteArtifactoryQueue)
            .to(pipelineHardDeleteFanoutExchange)
    }

    @Bean
    fun pipelineEventArtifactoryer(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun pipelineHardDeleteArtifactoryListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineHardDeleteArtifactoryQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: PipelineHardDeleteMQListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = pipelineHardDeleteArtifactoryQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 120000,
            consecutiveActiveTrigger = 10,
            concurrency = artifactoryConcurrency!!,
            maxConcurrency = 10
        )
    }
}
