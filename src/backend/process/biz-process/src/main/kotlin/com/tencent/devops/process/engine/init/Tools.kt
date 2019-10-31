package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter

object Tools {

    fun <T : IPipelineEvent> createSimpleMessageListenerContainer(
        connectionFactory: ConnectionFactory,
        queue: Queue,
        rabbitAdmin: RabbitAdmin,
        buildListener: BaseListener<T>,
        messageConverter: Jackson2JsonMessageConverter,
        startConsumerMinInterval: Long,
        consecutiveActiveTrigger: Int,
        concurrency: Int,
        maxConcurrency: Int
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(queue.name)
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(Math.max(maxConcurrency, concurrency))
        container.setRabbitAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(startConsumerMinInterval)
        container.setConsecutiveActiveTrigger(consecutiveActiveTrigger)
        val adapter = MessageListenerAdapter(buildListener, buildListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}