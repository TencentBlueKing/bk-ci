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

package com.tencent.devops.process.engine.webhook

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.run.WebhookEventListener
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * @ Date       ：Created in 16:50 2019-08-07
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class WebhookMQConfiguration @Autowired constructor() {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
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

    // SVN 消息队列配置
    @Bean
    fun svnEventExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_SVN_BUILD_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun svnEventQueue(): Queue {
        return Queue(MQ.QUEUE_SVN_BUILD_REQUEST_EVENT, true)
    }

    @Bean
    fun svnEventBind(
        @Autowired svnEventQueue: Queue,
        @Autowired svnEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(svnEventQueue).to(svnEventExchange).with(MQ.ROUTE_SVN_BUILD_REQUEST_EVENT)
    }

    // Git 消息队列配置
    @Bean
    fun gitEventExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_GIT_BUILD_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun gitEventQueue(): Queue {
        return Queue(MQ.QUEUE_GIT_BUILD_REQUEST_EVENT, true)
    }

    @Bean
    fun gitEventBind(
        @Autowired gitEventQueue: Queue,
        @Autowired gitEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(gitEventQueue).to(gitEventExchange).with(MQ.ROUTE_GIT_BUILD_REQUEST_EVENT)
    }

    // Gitlab 消息队列配置
    @Bean
    fun gitlabEventExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_GITLAB_BUILD_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun gitlabEventQueue(): Queue {
        return Queue(MQ.QUEUE_GITLAB_BUILD_REQUEST_EVENT, true)
    }

    @Bean
    fun gitlabEventBind(
        @Autowired gitlabEventQueue: Queue,
        @Autowired gitlabEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(gitlabEventQueue).to(gitlabEventExchange).with(MQ.ROUTE_GITLAB_BUILD_REQUEST_EVENT)
    }

    // Github 消息队列配置
    @Bean
    fun githubEventExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_GITHUB_BUILD_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun githubEventQueue(): Queue {
        return Queue(MQ.QUEUE_GITHUB_BUILD_REQUEST_EVENT, true)
    }

    @Bean
    fun githubEventBind(
        @Autowired githubEventQueue: Queue,
        @Autowired githubEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(githubEventQueue).to(githubEventExchange).with(MQ.ROUTE_GITHUB_BUILD_REQUEST_EVENT)
    }

    // tGit 消息队列配置
    @Bean
    fun tgitEventExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_TGIT_BUILD_REQUEST_EVENT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun tgitEventQueue(): Queue {
        return Queue(MQ.QUEUE_TGIT_BUILD_REQUEST_EVENT, true)
    }

    @Bean
    fun tgitEventBind(
        @Autowired tgitEventQueue: Queue,
        @Autowired tgitEventExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(tgitEventQueue).to(tgitEventExchange).with(MQ.ROUTE_TGIT_BUILD_REQUEST_EVENT)
    }

    // 各类Commit事件监听
    @Bean
    fun svnEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired svnEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        logger.info("Start SVN commit event listener")
        val adapter = MessageListenerAdapter(webhookEventListener, webhookEventListener::handleCommitEvent.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = svnEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 1,
            consecutiveActiveTrigger = 1,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun gitEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired gitEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        logger.info("Start Git commit event listener")
        val adapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleCommitEvent.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = gitEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 1,
            consecutiveActiveTrigger = 1,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun gitlabEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired gitlabEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleCommitEvent.name)
        adapter.setMessageConverter(messageConverter)
        logger.info("Start Gitlab commit event listener")
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = gitlabEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 1,
            consecutiveActiveTrigger = 1,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    @Bean
    fun githubEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired githubEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleGithubCommitEvent.name)
        adapter.setMessageConverter(messageConverter)
        logger.info("Start Github commit event listener")
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = githubEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 1,
            consecutiveActiveTrigger = 1,
            concurrency = 5,
            maxConcurrency = 20
        )
    }

    @Bean
    fun tgitEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired tgitEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        logger.info("Start Git commit event listener")
        val adapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleCommitEvent.name)
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = tgitEventQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = 1,
            consecutiveActiveTrigger = 1,
            concurrency = 10,
            maxConcurrency = 20
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookMQConfiguration::class.java)
    }
}