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

package com.tencent.devops.plugin.init

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.plugin.listener.CodeWebhookFinishListener
import com.tencent.devops.plugin.listener.GitHubPullRequestListener
import com.tencent.devops.plugin.listener.TGitCommitListener
import com.tencent.devops.plugin.listener.measure.MeasureListener
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线监控配置
 */
@Configuration
class ListenerConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun pipelineFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_EXTENDS_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
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

    @Value("\${queueConcurrency.webhook:2}")
    private val webhookConcurrency: Int? = null

    /**
     * 构建结束的webhook队列--- 并发小
     */
    @Bean
    fun buildFinishCodeWebhookQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_CODE_WEBHOOK)

    @Bean
    fun buildFinishCodeWebhookQueueBind(
        @Autowired buildFinishCodeWebhookQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(buildFinishCodeWebhookQueue).to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun codeWebhookFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildFinishCodeWebhookQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: CodeWebhookFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildFinishCodeWebhookQueue.name)
        val concurrency = webhookConcurrency!!
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(10, concurrency))
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    /**
     * Git事件交换机
     */
    @Bean
    fun gitCommitCheckExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_GIT_COMMIT_CHECK, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Value("\${queueConcurrency.webhook:1}")
    private val gitCommitCheckConcurrency: Int? = null

    /**
     * gitcommit队列--- 并发小
     */
    @Bean
    fun gitCommitCheckQueue() = Queue(MQ.QUEUE_GIT_COMMIT_CHECK)

    @Bean
    fun gitCommitCheckQueueBind(
        @Autowired gitCommitCheckQueue: Queue,
        @Autowired gitCommitCheckExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(gitCommitCheckQueue).to(gitCommitCheckExchange)
            .with(MQ.ROUTE_GIT_COMMIT_CHECK)
    }

    @Bean
    fun gitCommitCheckListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired gitCommitCheckQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: TGitCommitListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(gitCommitCheckQueue.name)
        val concurrency = gitCommitCheckConcurrency!!
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(5, concurrency))
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Value("\${queueConcurrency.githubPr:1}")
    private val githubPrConcurrency: Int? = null

    /**
     * github pr队列--- 并发小
     */
    @Bean
    fun githubPrQueue() = Queue(MQ.QUEUE_GITHUB_PR)

    @Bean
    fun githubPrQueueBind(
        @Autowired githubPrQueue: Queue,
        @Autowired gitCommitCheckExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(githubPrQueue).to(gitCommitCheckExchange)
            .with(MQ.ROUTE_GITHUB_PR)
    }

    @Bean
    fun githubPrQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired githubPrQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: GitHubPullRequestListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(githubPrQueue.name)
        val concurrency = githubPrConcurrency!!
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(5, concurrency))
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Value("\${queueConcurrency.measure:5}")
    private val measureConcurrency: Int? = null

    @Bean
    fun measureQueue() = Queue(MQ.QUEUE_MEASURE_REQUEST_EVENT)

    @Bean
    fun measureExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_MEASURE_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun measureQueueBind(
        @Autowired measureQueue: Queue,
        @Autowired measureExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(measureQueue).to(measureExchange)
            .with(MQ.ROUTE_MEASURE_REQUEST_EVENT)
    }

    @Bean
    fun measureQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired measureQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: MeasureListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(measureQueue.name)
        val concurrency = measureConcurrency!!
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(5, concurrency))
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}