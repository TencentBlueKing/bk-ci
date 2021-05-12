package com.tencent.bk.codecc.task.config

import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.devops.common.constant.ComConstants
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
    open fun customTriggerExchange(): DirectExchange {
        val directExchange = DirectExchange(EXCHANGE_CUSTOM_PIPELINE_TRIGGER, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun independentTriggerMsgQueue() = Queue("$QUEUE_CUSTOM_PIPELINE_TRIGGER.${ComConstants.CodeCCDispatchRoute.INDEPENDENT.name.toLowerCase()}")

    @Bean
    open fun independentTriggerQueueBind(
        @Autowired independentTriggerMsgQueue: Queue,
        @Autowired customTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(independentTriggerMsgQueue)
            .to(customTriggerExchange).with("$ROUTE_CUSTOM_PIPELINE_TRIGGER.${ComConstants.CodeCCDispatchRoute.INDEPENDENT.name.toLowerCase()}")
    }

    @Bean
    open fun independentTriggerListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired independentTriggerMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengTriggerService: GongfengTriggerService,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(independentTriggerMsgQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(gongfengTriggerService, gongfengTriggerService::manualStartupCustomPipeline.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }


    @Bean
    open fun devcloudTriggerMsgQueue() = Queue("$QUEUE_CUSTOM_PIPELINE_TRIGGER.${ComConstants.CodeCCDispatchRoute.DEVCLOUD.name.toLowerCase()}")

    @Bean
    open fun devcloudTriggerQueueBind(
        @Autowired devcloudTriggerMsgQueue: Queue,
        @Autowired customTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(devcloudTriggerMsgQueue)
            .to(customTriggerExchange).with("$ROUTE_CUSTOM_PIPELINE_TRIGGER.${ComConstants.CodeCCDispatchRoute.DEVCLOUD.name.toLowerCase()}")
    }

    @Bean
    open fun devcloudTriggerListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired devcloudTriggerMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengTriggerService: GongfengTriggerService,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(devcloudTriggerMsgQueue.name)
        container.setConcurrentConsumers(2)
        container.setMaxConcurrentConsumers(2)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(gongfengTriggerService, gongfengTriggerService::manualStartupCustomPipeline.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}
