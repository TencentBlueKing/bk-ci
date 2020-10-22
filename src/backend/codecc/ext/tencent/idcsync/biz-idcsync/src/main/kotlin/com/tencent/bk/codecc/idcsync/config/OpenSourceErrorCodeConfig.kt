package com.tencent.bk.codecc.idcsync.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.idcsync.listener.OpenSourceErrorCodeListener
import com.tencent.devops.common.web.mq.EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT
import com.tencent.devops.common.web.mq.QUEUE_CODECC_OPENSOURCE_FAIL_DATA_REPORT
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSourceErrorCodeConfig {

    @Bean
    open fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    open fun taskFailExchange(): FanoutExchange {
        //auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val directExchange = FanoutExchange(EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    open fun taskFailReportQueue() = Queue(QUEUE_CODECC_OPENSOURCE_FAIL_DATA_REPORT)

    @Bean
    open fun taskFailQueueBind(
        @Autowired taskFailReportQueue: Queue,
        @Autowired taskFailExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(taskFailReportQueue)
            .to(taskFailExchange)
    }


    @Bean
    open fun createTaskListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired createTaskMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired openSourceErrorCodeListener: OpenSourceErrorCodeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(createTaskMsgQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        //确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(openSourceErrorCodeListener, openSourceErrorCodeListener::syncErrorCodeInfo.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}