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

package com.tencent.devops.process.websocket

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.service.utils.CommonUtils
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
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
 * 流水线状态websocket刷新配置
 */
@Configuration
class PipelineStatusMQConfiguration {

    /**
     * 构建广播交换机
     */
    @Bean
    fun pipelineStatusChangeFanoutExchange(): FanoutExchange {
        return FanoutExchange(MQ.EXCHANGE_PIPELINE_STATUS_CHANGE_TMP_FANOUT, false, false)
    }

    @Value("\${queueConcurrency.pipelineStatusChange:1}")
    private val pipelineStatusChangeConcurrency: Int? = null

    @Bean
    fun pipelineStatusChangeQueue() =
        Queue(MQ.QUEUE_PIPELINE_STATUS_CHANGE_TMP_EVENT + "." + CommonUtils.getInnerIP(), false, true, true)

    @Bean
    fun pipelineStatusChangeQueueBind(
        @Autowired pipelineStatusChangeQueue: Queue,
        @Autowired pipelineStatusChangeFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineStatusChangeQueue).to(pipelineStatusChangeFanoutExchange)
    }

    @Bean
    fun pipelineStatusChangeListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineStatusChangeQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: PipelineStatusChangeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineStatusChangeQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 600000,
            consecutiveActiveTrigger = 100,
            concurrency = pipelineStatusChangeConcurrency!!,
            maxConcurrency = 4
        )
    }
}