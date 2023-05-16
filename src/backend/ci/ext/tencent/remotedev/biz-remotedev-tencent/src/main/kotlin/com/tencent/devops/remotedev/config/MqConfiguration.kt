package com.tencent.devops.remotedev.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.remotedev.MQ.EXCHANGE_WORKSPACE_UPDATE_FROM_K8S
import com.tencent.devops.common.remotedev.MQ.QUEUE_WORKSPACE_UPDATE_FROM_K8S
import com.tencent.devops.common.remotedev.MQ.ROUTE_WORKSPACE_UPDATE_FROM_K8S
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.remotedev.listener.RemoteDevUpdateListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MqConfiguration {

    @Bean
    @ConditionalOnMissingBean(RabbitAdmin::class)
    fun rabbitAdmin(
        connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun remoteDevExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_WORKSPACE_UPDATE_FROM_K8S, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    /**
     * k8s -> remote dev 事件
     */
    @Bean
    fun remoteDevUpdateQueue() = Queue(QUEUE_WORKSPACE_UPDATE_FROM_K8S)

    @Bean
    fun remoteDevUpdateQueueBind(
        @Autowired remoteDevUpdateQueue: Queue,
        @Autowired remoteDevExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(remoteDevUpdateQueue)
            .to(remoteDevExchange).with(ROUTE_WORKSPACE_UPDATE_FROM_K8S)
    }

    @Bean
    fun pipelinePauseTaskExecuteListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired remoteDevUpdateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired remoteDevUpdateListener: RemoteDevUpdateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = remoteDevUpdateQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = remoteDevUpdateListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 50
        )
    }

    @Bean
    fun remoteDevDispatcher(rabbitTemplate: RabbitTemplate) = RemoteDevDispatcher(rabbitTemplate)
}
