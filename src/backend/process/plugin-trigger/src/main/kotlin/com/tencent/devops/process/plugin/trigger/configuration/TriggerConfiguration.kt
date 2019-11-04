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

package com.tencent.devops.process.plugin.trigger.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.plugin.trigger.timer.SchedulerManager
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerBuildListener
import com.tencent.devops.process.plugin.trigger.timer.listener.PipelineTimerChangerListener
import com.tencent.devops.process.plugin.trigger.timer.quartz.PipelineJobBean
import com.tencent.devops.process.plugin.trigger.timer.quartz.QuartzSchedulerManager
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @version 1.0
 */

@Configuration
open class TriggerConfiguration {

    @Bean
    open fun pipelineJobBean(
        pipelineEventDispatcher: PipelineEventDispatcher,
        schedulerManager: SchedulerManager,
        pipelineTimerService: PipelineTimerService,
        redisOperation: RedisOperation
    ): PipelineJobBean {
        return PipelineJobBean(
            pipelineEventDispatcher,
            schedulerManager,
            pipelineTimerService,
            redisOperation
        )
    }

    @Bean
    open fun schedulerManager() = QuartzSchedulerManager()

    @Bean
    open fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Value("\${queueConcurrency.timerTrigger:5}")
    private val timerConcurrency: Int? = null

    @Bean
    open fun pipelineCoreExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    /**
     * 定时构建队列--- 并发一般
     */
    @Bean
    open fun pipelineTimerBuildQueue() = Queue(MQ.QUEUE_PIPELINE_TIMER)

    @Bean
    open fun pipelineTimerBuildQueueBind(
        @Autowired pipelineTimerBuildQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineTimerBuildQueue).to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_TIMER)
    }

    @Bean
    open fun pipelineTimerBuildListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineTimerBuildQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineTimerBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineTimerBuildQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 20000,
            consecutiveActiveTrigger = 3,
            concurrency = timerConcurrency!!,
            maxConcurrency = 20
        )
    }

    /**
     * 构建定时构建定时变化的广播交换机
     */
    @Bean
    open fun pipelineTimerChangeFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_TIMER_CHANGE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Value("\${queueConcurrency.timerChanger:3}")
    private val timerChangerConcurrency: Int? = null

    /**
     * 用于接收定时任务状态变化广播的临时队列，队列将自动销毁
     */
    @Bean
    open fun timerChangeQueueTemp(): Queue {
        return QueueBuilder.nonDurable().autoDelete().build()
    }

    @Bean
    open fun timerChangeQueueTempBind(
        @Autowired timerChangeQueueTemp: Queue,
        @Autowired pipelineTimerChangeFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(timerChangeQueueTemp).to(pipelineTimerChangeFanoutExchange)
    }

    @Bean
    open fun timerChangeQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired timerChangeQueueTemp: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineTimerChangerListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = timerChangeQueueTemp,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 20000,
            consecutiveActiveTrigger = 3,
            concurrency = timerChangerConcurrency!!,
            maxConcurrency = 10
        )
    }
}
