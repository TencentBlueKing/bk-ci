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

package com.tencent.devops.process.init

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineCreateListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineDeleteListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineRestoreListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineStreamEnabledListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineUpdateListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建核心配置
 */
@Configuration
class PipelineBaseConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    @ConditionalOnMissingBean(name = ["pipelineCoreExchange"])
    fun pipelineCoreExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Value("\${queueConcurrency.pipelineCreate:5}")
    private val pipelineCreateConcurrency: Int? = null

    /**
     * 流水线创建队列--- 并发小
     */
    @Bean
    fun pipelineCreateQueue() = Queue(MQ.QUEUE_PIPELINE_CREATE)

    @Bean
    fun pipelineCreateQueueBind(
        @Autowired pipelineCreateQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineCreateQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_CREATE)
    }

    @Bean
    fun pipelineCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired createListener: MQPipelineCreateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineCreateQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = createListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineCreateConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.pipelineDelete:2}")
    private val pipelineDeleteConcurrency: Int? = null

    /**
     * 流水线删除队列--- 并发小
     */
    @Bean
    fun pipelineDeleteQueue() = Queue(MQ.QUEUE_PIPELINE_DELETE)

    @Bean
    fun pipelineDeleteQueueBind(
        @Autowired pipelineDeleteQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineDeleteQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_DELETE)
    }

    @Bean
    fun pipelineDeleteListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineDeleteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired deleteListener: MQPipelineDeleteListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineDeleteQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = deleteListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineDeleteConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.pipelineUpdate:5}")
    private val pipelineUpdateConcurrency: Int? = null

    /**
     * 流水线更新队列--- 并发一般
     */
    @Bean
    fun pipelineUpdateQueue() = Queue(MQ.QUEUE_PIPELINE_UPDATE)

    @Bean
    fun pipelineUpdateQueueBind(
        @Autowired pipelineUpdateQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineUpdateQueue).to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_UPDATE)
    }

    @Bean
    fun pipelineUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineUpdateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired updateListener: MQPipelineUpdateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineUpdateQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = updateListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineUpdateConcurrency!!,
            maxConcurrency = 50
        )
    }

    @Value("\${queueConcurrency.pipelineRestoreUpdate:2}")
    private val pipelineRestoreConcurrency: Int? = null

    /**
     * 流水线恢复队列--- 并发一般
     */
    @Bean
    fun pipelineRestoreQueue() = Queue(MQ.QUEUE_PIPELINE_RESTORE)

    @Bean
    fun pipelineRestoreQueueBind(
        @Autowired pipelineRestoreQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineRestoreQueue).to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_RESTORE)
    }

    @Bean
    fun pipelineRestoreListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineRestoreQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired restoreListener: MQPipelineRestoreListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineRestoreQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = restoreListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineRestoreConcurrency!!,
            maxConcurrency = 50
        )
    }

    /**
     * 声明 流水线设置变更 事件交换机
     */
    @Bean
    @ConditionalOnMissingBean(name = ["pipelineSettingChangeExchange"])
    fun pipelineSettingChangeExchange(): FanoutExchange {
        val pipelineSettingChangeExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT, true, false)
        pipelineSettingChangeExchange.isDelayed = true
        return pipelineSettingChangeExchange
    }

    @Value("\${queueConcurrency.pipelineStreamEnabled:5}")
    private val pipelineStreamEnabledConcurrency: Int? = null

    /**
     * 流水线开启stream队列--- 并发一般
     */
    @Bean
    fun pipelineStreamEnabledQueue() = Queue(MQ.QUEUE_PIPELINE_STREAM_ENABLED)

    @Bean
    fun pipelineStreamEnabledQueueBind(
        @Autowired pipelineUpdateQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineUpdateQueue).to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_STREAM_ENABLED)
    }

    @Bean
    fun pipelineStreamEnabledListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineStreamEnabledQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired streamEnabledListener: MQPipelineStreamEnabledListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineStreamEnabledQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = streamEnabledListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineStreamEnabledConcurrency!!,
            maxConcurrency = 50
        )
    }
}
