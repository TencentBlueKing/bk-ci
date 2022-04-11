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

package com.tencent.devops.log.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_LOG_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.EXCHANGE_LOG_STATUS_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.QUEUE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.QUEUE_LOG_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.QUEUE_LOG_STATUS_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_LOG_BATCH_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_LOG_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ.ROUTE_LOG_STATUS_BUILD_EVENT
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.web.mq.EXTEND_CONNECTION_FACTORY_NAME
import com.tencent.devops.common.web.mq.EXTEND_RABBIT_ADMIN_NAME
import com.tencent.devops.common.web.mq.EXTEND_RABBIT_TEMPLATE_NAME
import com.tencent.devops.log.jmx.LogPrintBean
import com.tencent.devops.log.consumer.LogBatchEventListener
import com.tencent.devops.log.consumer.LogEventListener
import com.tencent.devops.log.consumer.LogStatusEventListener
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.BuildLogPrintService
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class LogMQConfiguration @Autowired constructor() {

    @Value("\${log.rabbitmq.preprocess.concurrency:#{null}}")
    private val preprocessConcurrency: Int? = null

    @Value("\${log.rabbitmq.preprocess.maxConcurrency:#{null}}")
    private val preprocessMaxConcurrency: Int? = null

    @Value("\${log.rabbitmq.storage.concurrency:#{null}}")
    private val storageConcurrency: Int? = null

    @Value("\${log.rabbitmq.storage.maxConcurrency:#{null}}")
    private val storageMaxConcurrency: Int? = null

    @Bean
    fun rabbitAdmin(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun logEventExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LOG_BUILD_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun logBatchEventExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LOG_BATCH_BUILD_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun logStatusEventExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_LOG_STATUS_BUILD_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun logEventQueue(): Queue {
        return Queue(QUEUE_LOG_BUILD_EVENT, true)
    }

    @Bean
    fun logBatchEventQueue(): Queue {
        return Queue(QUEUE_LOG_BATCH_BUILD_EVENT, true)
    }

    @Bean
    fun logStatusEventQueue(): Queue {
        return Queue(QUEUE_LOG_STATUS_BUILD_EVENT, true)
    }

    @Bean
    fun buildLogPrintService(
        @Qualifier(value = EXTEND_RABBIT_TEMPLATE_NAME)
        rabbitTemplate: RabbitTemplate,
        logPrintBean: LogPrintBean,
        storageProperties: StorageProperties,
        logServiceConfig: LogServiceConfig
    ) = BuildLogPrintService(rabbitTemplate, logPrintBean, storageProperties, logServiceConfig)

    @Bean
    fun logEventBind(
        @Autowired logEventQueue: Queue,
        @Autowired logEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(logEventQueue).to(logEventExchange).with(ROUTE_LOG_BUILD_EVENT)
    }

    @Bean
    fun logBatchEventBind(
        @Autowired logBatchEventQueue: Queue,
        @Autowired logBatchEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(logBatchEventQueue).to(logBatchEventExchange).with(ROUTE_LOG_BATCH_BUILD_EVENT)
    }

    @Bean
    fun logStatusEventBind(
        @Autowired logStatusEventQueue: Queue,
        @Autowired logStatusEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(logStatusEventQueue).to(logStatusEventExchange).with(ROUTE_LOG_STATUS_BUILD_EVENT)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun logEventListener(
        @Qualifier(value = EXTEND_CONNECTION_FACTORY_NAME)
        @Autowired connectionFactory: ConnectionFactory,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME)
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logEventQueue: Queue,
        @Autowired logListener: LogEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val messageListenerAdapter = MessageListenerAdapter(logListener, logListener::logEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = logEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = messageListenerAdapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = preprocessConcurrency ?: 1,
            maxConcurrency = preprocessMaxConcurrency ?: 1
        )
    }

    @Bean
    fun logBatchEventListener(
        @Qualifier(value = EXTEND_CONNECTION_FACTORY_NAME)
        @Autowired connectionFactory: ConnectionFactory,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME)
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logBatchEventQueue: Queue,
        @Autowired logListener: LogBatchEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val messageListenerAdapter = MessageListenerAdapter(logListener, logListener::logBatchEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = logBatchEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = messageListenerAdapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = storageConcurrency ?: 10,
            maxConcurrency = storageMaxConcurrency ?: 100
        )
    }

    @Bean
    fun logStatusEventListener(
        @Qualifier(value = EXTEND_CONNECTION_FACTORY_NAME)
        @Autowired connectionFactory: ConnectionFactory,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME)
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logStatusEventQueue: Queue,
        @Autowired logListener: LogStatusEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val messageListenerAdapter = MessageListenerAdapter(logListener, logListener::logStatusEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = logStatusEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = messageListenerAdapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 20,
            maxConcurrency = 20
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
    fun pipelineBuildFinishLogQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_LOG)

    @Bean
    fun pipelineBuildFinishLogQueueBind(
        @Autowired pipelineBuildFinishLogQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishLogQueue).to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildFinishLogListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildFinishLogQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired logService: LogService,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(logService, logService::pipelineFinish.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = pipelineBuildFinishLogQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 1
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogMQConfiguration::class.java)
    }
}
