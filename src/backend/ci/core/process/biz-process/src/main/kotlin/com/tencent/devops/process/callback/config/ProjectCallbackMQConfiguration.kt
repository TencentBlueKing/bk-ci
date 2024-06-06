package com.tencent.devops.process.callback.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.process.callback.listener.ProjectCallbackEventListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
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

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ProjectCallbackMQConfiguration {
    @Bean
    fun projectCreateCallbackQueue() = Queue(MQ.QUEUE_PROJECT_CREATE_CALLBACK_EVENT)

    @Bean
    fun projectUpdateCallbackQueue() = Queue(MQ.QUEUE_PROJECT_UPDATE_CALLBACK_EVENT)

    @Bean
    fun projectEnableCallbackQueue() = Queue(MQ.QUEUE_PROJECT_ENABLE_CALLBACK_EVENT)

    @Bean
    fun projectCreateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_CREATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectUpdateExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_UPDATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectEnableExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_ENABLE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectCreateQueueBind(
        @Autowired projectCreateCallbackQueue: Queue,
        @Autowired projectCreateExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectCreateCallbackQueue)
        .to(projectCreateExchange)

    @Bean
    fun projectUpdateQueueBind(
        @Autowired projectUpdateCallbackQueue: Queue,
        @Autowired projectUpdateExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectUpdateCallbackQueue)
        .to(projectUpdateExchange)

    @Bean
    fun projectEnableQueueBind(
        @Autowired projectEnableCallbackQueue: Queue,
        @Autowired projectEnableExchange: FanoutExchange
    ): Binding = BindingBuilder.bind(projectEnableCallbackQueue)
        .to(projectEnableExchange)

    @Bean
    fun projectCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectCreateCallbackQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectCallbackEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectCreateCallbackQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun projectUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectUpdateCallbackQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectCallbackEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectUpdateCallbackQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun projectEnableListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectEnableCallbackQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: ProjectCallbackEventListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectEnableCallbackQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}