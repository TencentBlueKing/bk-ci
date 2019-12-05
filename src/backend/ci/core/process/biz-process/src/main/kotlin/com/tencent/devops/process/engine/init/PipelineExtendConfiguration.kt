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
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.engine.listener.run.callback.PipelineBuildCallBackListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

/**
 * 流水线构建扩展配置
 */
@Configuration
class PipelineExtendConfiguration {

    /**
     * 构建广播交换机
     */
    @Bean
    fun pipelineBuildFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    /**
     * 构建构建回调广播交换机
     */
    @Bean
    fun pipelineBuildStatusFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_CALL_BACK_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildStatusChangeQueue(): Queue {
        return Queue(
            MQ.QUEUE_PIPELINE_BUILD_STATUS_CHANGE + ".${CommonUtils.getInnerIP()}.${UUID.randomUUID()}",
            false,
            true,
            true
        )
    }

    @Bean
    fun pipelineBuildStatusChangeQueueBind(
        @Autowired pipelineBuildStatusChangeQueue: Queue,
        @Autowired pipelineBuildStatusFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildStatusChangeQueue).to(pipelineBuildStatusFanoutExchange)
    }

    @Bean
    fun pipelineBuildStatusChangeListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildStatusChangeQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildCallBackListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildStatusChangeQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 20
        )
    }
}