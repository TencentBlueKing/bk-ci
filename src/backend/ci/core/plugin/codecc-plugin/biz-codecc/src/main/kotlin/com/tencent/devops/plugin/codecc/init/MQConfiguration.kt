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

package com.tencent.devops.plugin.codecc.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.plugin.codecc.event.listener.ChangeCodeCCListener
import com.tencent.devops.plugin.codecc.event.listener.PipelineModelAnalysisListener
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
 * MQ配置
 */
@Configuration
class MQConfiguration {

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    /**
     * 声明 流水线设置变更 事件交换机
     */
    @Bean
    fun pipelineSettingChangeExchange(): FanoutExchange {
        val pipelineSettingChangeExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT, true, false)
        pipelineSettingChangeExchange.isDelayed = true
        return pipelineSettingChangeExchange
    }

    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @Bean
    fun pipelineSettingChangeQueue() = Queue(MQ.QUEUE_PIPELINE_SETTING_CHANGE)

    @Bean
    fun pipelineSettingChangeQueueBind(
        @Autowired pipelineSettingChangeQueue: Queue,
        @Autowired pipelineSettingChangeExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineSettingChangeQueue).to(pipelineSettingChangeExchange)
    }

    @Bean
    fun pipelineSettingChangeQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineSettingChangeQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired changeCodeCCListener: ChangeCodeCCListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineSettingChangeQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = changeCodeCCListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 20000,
            consecutiveActiveTrigger = 3,
            concurrency = 1,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.modelAnalysis:2}")
    private val modelAnalysisConcurrency: Int? = null

    /**
     * 监控队列--- 并发可小
     */
    @Bean
    fun pipelineModelAnalysisQueue() = Queue(MQ.QUEUE_PIPELINE_EXTENDS_MODEL)

    @Bean
    fun pipelineModelAnalysisQueueBind(
        @Autowired pipelineModelAnalysisQueue: Queue,
        @Autowired pipelineFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineModelAnalysisQueue).to(pipelineFanoutExchange)
    }

    @Bean
    fun pipelineModelAnalysisListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineModelAnalysisQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineModelAnalysisListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineModelAnalysisQueue.name)
        val concurrency = modelAnalysisConcurrency!!
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(10, concurrency))
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(buildListener, buildListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}