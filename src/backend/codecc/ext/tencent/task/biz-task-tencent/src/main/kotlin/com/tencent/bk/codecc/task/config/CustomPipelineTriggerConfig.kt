package com.tencent.bk.codecc.task.config

import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.devops.common.web.mq.EXCHANGE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.common.web.mq.QUEUE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.common.web.mq.ROUTE_CUSTOM_PIPELINE_TRIGGER
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
open class CustomPipelineTriggerConfig {

    @Bean
    open fun customTriggerExchange() : DirectExchange{
        val directExchange = DirectExchange(EXCHANGE_CUSTOM_PIPELINE_TRIGGER, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun customTriggerMsgQueue() = Queue(QUEUE_CUSTOM_PIPELINE_TRIGGER)

    @Bean
    open fun customTriggerQueueBind(
        @Autowired customTriggerMsgQueue: Queue,
        @Autowired customTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(customTriggerMsgQueue)
            .to(customTriggerExchange).with(ROUTE_CUSTOM_PIPELINE_TRIGGER)
    }

    @Bean
    open fun customTriggerListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired customTriggerMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengTriggerService: GongfengTriggerService,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(customTriggerMsgQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        //确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(gongfengTriggerService, gongfengTriggerService::manualStartupCustomPipeline.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }


}