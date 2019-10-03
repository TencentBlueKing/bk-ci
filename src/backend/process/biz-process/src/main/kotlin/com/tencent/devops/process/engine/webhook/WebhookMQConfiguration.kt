package com.tencent.devops.process.engine.webhook

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
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
 * @ Author     ：Royal Huang
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

    // 各类Commit事件监听
    @Bean
    fun svnEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired svnEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(svnEventQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(webhookEventListener, webhookEventListener::handleCommitEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start SVN commit event listener")
        return container
    }

    @Bean
    fun gitEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired gitEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(gitEventQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleCommitEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start Git commit event listener")
        return container
    }

    @Bean
    fun gitlabEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired gitlabEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(gitlabEventQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleCommitEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start Gitlab commit event listener")
        return container
    }

    @Bean
    fun githubEventListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired githubEventQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired webhookEventListener: WebhookEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(githubEventQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter = MessageListenerAdapter(webhookEventListener, WebhookEventListener::handleGithubCommitEvent.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        logger.info("Start Github commit event listener")
        return container
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookMQConfiguration::class.java)
    }
}