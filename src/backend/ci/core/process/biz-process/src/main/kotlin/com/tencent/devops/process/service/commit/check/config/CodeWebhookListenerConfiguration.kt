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

package com.tencent.devops.process.service.commit.check.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.service.commit.check.listener.CodeWebhookListener
import com.tencent.devops.process.service.commit.check.listener.GitHubCommitCheckListener
import com.tencent.devops.process.service.commit.check.listener.TGitCommitCheckListener
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
@Suppress("TooManyFunctions")
class CodeWebhookListenerConfiguration {

    companion object {
        private const val BUILD_MAX_CONCURRENT = 10
        private const val CHECK_MAX_CONCURRENT = 5
    }

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

    @Bean
    fun pipelineBuildQueueFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT, true, false)
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
    fun buildFinishCodeWebhookQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_COMMIT_CHECK)

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
        @Autowired listener: CodeWebhookListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, CodeWebhookListener::onBuildFinished.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildFinishCodeWebhookQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = webhookConcurrency!!,
            maxConcurrency = BUILD_MAX_CONCURRENT
        )
    }

    @Bean
    fun buildQueueCodeWebhookQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_QUEUE_CODE_WEBHOOK)

    @Bean
    fun buildQueueCodeWebhookQueueBind(
        @Autowired buildQueueCodeWebhookQueue: Queue,
        @Autowired pipelineBuildQueueFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(buildQueueCodeWebhookQueue).to(pipelineBuildQueueFanoutExchange)
    }

    @Bean
    fun codeWebhookBuildQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildQueueCodeWebhookQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: CodeWebhookListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, CodeWebhookListener::onBuildQueue.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = buildQueueCodeWebhookQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = webhookConcurrency!!,
            maxConcurrency = BUILD_MAX_CONCURRENT
        )
    }

    /**
     * Git事件交换机
     */
    @Bean
    fun tgitCommitCheckExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_GIT_COMMIT_CHECK_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Value("\${queueConcurrency.webhook:1}")
    private val gitCommitCheckConcurrency: Int? = null

    /**
     * gitcommit队列--- 并发小
     */
    @Bean
    fun tgitCommitCheckQueue() = Queue(MQ.QUEUE_GIT_COMMIT_CHECK_EVENT)

    @Bean
    fun tgitCommitCheckQueueBind(
        @Autowired tgitCommitCheckQueue: Queue,
        @Autowired tgitCommitCheckExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(tgitCommitCheckQueue).to(tgitCommitCheckExchange)
            .with(MQ.ROUTE_GIT_COMMIT_CHECK_EVENT)
    }

    @Bean
    fun gitCommitCheckListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired tgitCommitCheckQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: TGitCommitCheckListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = tgitCommitCheckQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = gitCommitCheckConcurrency!!,
            maxConcurrency = CHECK_MAX_CONCURRENT
        )
    }

    @Value("\${queueConcurrency.githubPr:1}")
    private val githubPrConcurrency: Int? = null

    /**
     * github pr队列--- 并发小
     */
    @Bean
    fun githubCommitCheckQueue() = Queue(MQ.QUEUE_GITHUB_COMMIT_CHECK_EVENT)

    @Bean
    fun githubPrQueueBind(
        @Autowired githubCommitCheckQueue: Queue,
        @Autowired tgitCommitCheckExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(githubCommitCheckQueue).to(tgitCommitCheckExchange)
            .with(MQ.ROUTE_GITHUB_COMMIT_CHECK_EVENT)
    }

    @Bean
    fun githubPrQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired githubCommitCheckQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: GitHubCommitCheckListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = githubCommitCheckQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = githubPrConcurrency!!,
            maxConcurrency = CHECK_MAX_CONCURRENT
        )
    }
}
