package com.tencent.devops.turbo.config

import com.tencent.devops.common.util.constants.EXCHANGE_TURBO_REPORT
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_CREATE
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_UPDATE
import com.tencent.devops.common.util.constants.ROUTE_TURBO_REPORT_CREATE
import com.tencent.devops.common.util.constants.ROUTE_TURBO_REPORT_UPDATE
import com.tencent.devops.turbo.component.TurboRecordConsumer
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
class TurboRecordMqConfig {

    @Bean
    fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun turboRecordHandleExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_TURBO_REPORT)
        directExchange.isDelayed = true
        directExchange.isDurable
        return directExchange
    }

    @Bean
    fun turboRecordCreateQueue(): Queue {
        return Queue(QUEUE_TURBO_REPORT_CREATE)
    }

    @Bean
    fun turboRecordCreateBind(turboRecordCreateQueue: Queue, turboRecordHandleExchange: DirectExchange): Binding {
        return BindingBuilder.bind(turboRecordCreateQueue)
            .to(turboRecordHandleExchange)
            .with(ROUTE_TURBO_REPORT_CREATE)
    }

    @Bean
    fun turboRecordCreateListenerContainer(
        connectionFactory: ConnectionFactory,
        turboRecordCreateQueue: Queue,
        rabbitAdmin: RabbitAdmin,
        turboRecordConsumer: TurboRecordConsumer,
        messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setPrefetchCount(1)
        container.setQueueNames(turboRecordCreateQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(turboRecordConsumer, turboRecordConsumer::createSingleTurboRecord.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    fun turboRecordUpdateQueue(): Queue {
        return Queue(QUEUE_TURBO_REPORT_UPDATE)
    }

    @Bean
    fun turboRecordUpdateBind(turboRecordUpdateQueue: Queue, turboRecordHandleExchange: DirectExchange): Binding {
        return BindingBuilder.bind(turboRecordUpdateQueue)
            .to(turboRecordHandleExchange)
            .with(ROUTE_TURBO_REPORT_UPDATE)
    }

    @Bean
    fun turboRecordUpdateListenerContainer(
        connectionFactory: ConnectionFactory,
        turboRecordUpdateQueue: Queue,
        rabbitAdmin: RabbitAdmin,
        turboRecordConsumer: TurboRecordConsumer,
        messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setPrefetchCount(1)
        container.setQueueNames(turboRecordUpdateQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setAmqpAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(turboRecordConsumer, turboRecordConsumer::updateSingleTurboRecord.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
