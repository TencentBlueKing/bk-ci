package com.tencent.bk.codecc.task.config

import com.tencent.bk.codecc.task.listener.GrayToolCreateTaskListener
import com.tencent.devops.common.web.mq.EXCHANGE_GRAY_TASK_POOL
import com.tencent.devops.common.web.mq.QUEUE_GRAY_TASK_POOL_CREATE
import com.tencent.devops.common.web.mq.QUEUE_GRAY_TASK_POOL_TRIGGER
import com.tencent.devops.common.web.mq.ROUTE_GRAY_TASK_POOL_CREATE
import com.tencent.devops.common.web.mq.ROUTE_GRAY_TASK_POOL_TRIGGER
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GrayTaskConfig {

    @Bean
    open fun grayTaskPoolExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_GRAY_TASK_POOL, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun grayTaskPoolCreateQueue() = Queue(QUEUE_GRAY_TASK_POOL_CREATE)

    @Bean
    open fun grayTaskPoolCreateBind(
        @Autowired grayTaskPoolCreateQueue: Queue,
        @Autowired grayTaskPoolExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(grayTaskPoolCreateQueue)
            .to(grayTaskPoolExchange).with(ROUTE_GRAY_TASK_POOL_CREATE)
    }

    @Bean
    open fun grayTaskPoolCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired grayTaskPoolCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired grayToolCreateTaskListener: GrayToolCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(grayTaskPoolCreateQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(grayToolCreateTaskListener, grayToolCreateTaskListener::handleWithGrayToolProject.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    open fun grayTaskPoolTriggerQueue() = Queue(QUEUE_GRAY_TASK_POOL_TRIGGER)

    @Bean
    open fun grayTaskPoolTriggerBind(
        @Autowired grayTaskPoolTriggerQueue: Queue,
        @Autowired grayTaskPoolExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(grayTaskPoolTriggerQueue)
            .to(grayTaskPoolExchange).with(ROUTE_GRAY_TASK_POOL_TRIGGER)
    }

    @Bean
    open fun grayTaskPoolTriggerListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired grayTaskPoolTriggerQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired grayToolCreateTaskListener: GrayToolCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(grayTaskPoolTriggerQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(grayToolCreateTaskListener, grayToolCreateTaskListener::executeTriggerGrayTask.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
