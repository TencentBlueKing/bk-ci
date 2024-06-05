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
import com.tencent.devops.process.engine.listener.run.finish.SubPipelineBuildFinishListener
import com.tencent.devops.process.engine.listener.run.start.SubPipelineBuildStartListener
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建扩展配置
 */
@Configuration
@SuppressWarnings("TooManyFunctions")
class BuildEngineExtendConfiguration {

    /**
     * 流水线扩展行为订阅广播
     */
    @Bean
    fun pipelineExtendsFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_EXTENDS_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    /**
     * 构建排队广播交换机
     */
    @Bean
    fun pipelineBuildQueueFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    /**
     * 构建启动广播交换机
     */
    @Bean
    fun pipelineBuildStartFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_START_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    /**
     * 插件构建完成广播交换机
     */
    @Bean
    fun pipelineBuildElementFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_ELEMENT_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun measureExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_MEASURE_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    /**
     * 构建结束广播交换机
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
    @ConditionalOnMissingBean(name = ["pipelineBuildStatusCallbackFanoutExchange"])
    fun pipelineBuildStatusCallbackFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_CALL_BACK_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildCommitFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_COMMIT_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun subPipelineBuildStatusQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_SUBPIPEINE)
    }

    @Bean
    fun subPipelineBuildFinishQueueBind(
        @Autowired subPipelineBuildStatusQueue: Queue,
        @Autowired pipelineBuildFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(subPipelineBuildStatusQueue).to(pipelineBuildFanoutExchange)
    }

    @Bean
    fun subPipelineBuildFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired subPipelineBuildStatusQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: SubPipelineBuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = subPipelineBuildStatusQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 10
        )
    }

    @Bean
    fun subPipelineBuildStartQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_START_SUBPIPEINE)
    }

    @Bean
    fun subPipelineBuildStartQueueBind(
        @Autowired subPipelineBuildStartQueue: Queue,
        @Autowired pipelineBuildStartFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(subPipelineBuildStartQueue).to(pipelineBuildStartFanoutExchange)
    }

    @Bean
    fun subPipelineBuildStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired subPipelineBuildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: SubPipelineBuildStartListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = subPipelineBuildStartQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 10
        )
    }
}
