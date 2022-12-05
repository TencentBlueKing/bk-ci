package com.tencent.bk.codecc.defect.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.component.DefectClusterComponent
import com.tencent.bk.codecc.defect.component.ExpireTaskHandleComponent
import com.tencent.devops.common.web.mq.*
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonMessageQueueConfig {

    @Bean
    fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }


    @Bean
    fun expiredTaskExchange(): CustomExchange {
        return CustomExchange(EXCHANGE_EXPIRED_TASK_STATUS, "x-delayed-message", true, false, mapOf("x-delayed-type" to "direct"))
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)


    @Bean
    fun expiredTaskQueue() = Queue(QUEUE_EXPIRED_TASK_STATUS)

    @Bean
    fun expiredTaskQueueBind(
        @Autowired expiredTaskQueue: Queue,
        @Autowired expiredTaskExchange: CustomExchange
    ): Binding {
        return BindingBuilder.bind(expiredTaskQueue).
        to(expiredTaskExchange).with(ROUTE_EXPIRED_TASK_STATUS).noargs()
    }


    @Bean
    fun expiredTaskListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired expiredTaskQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired expiredTaskHandleComponent: ExpireTaskHandleComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(expiredTaskQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(5)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setAmqpAdmin(rabbitAdmin)
        //确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(expiredTaskHandleComponent, expiredTaskHandleComponent::updateExpiredTaskStatus.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }


}