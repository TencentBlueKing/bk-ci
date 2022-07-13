package com.tencent.bk.codecc.codeccjob.config

import com.tencent.bk.codecc.codeccjob.consumer.WebsocketConsumer
import com.tencent.devops.common.util.IPUtils
import com.tencent.devops.common.web.mq.EXCHANGE_TASKLOG_DEFECT_WEBSOCKET
import com.tencent.devops.common.web.mq.QUEUE_TASKLOG_DEFECT_WEBSOCKET
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebSocketMQConfig {

    @Value("\${server.port:#{null}}")
    private val localPort: String? = null

    @Bean
    fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        val rabbitAdmin = RabbitAdmin(connectionFactory)
        rabbitAdmin.isAutoStartup = true
        return rabbitAdmin
    }
    
    @Bean
    fun websocketDefectExchange() : FanoutExchange{
        return FanoutExchange(EXCHANGE_TASKLOG_DEFECT_WEBSOCKET, true, false)
    }

    @Bean
    fun websocketDefectQueue() : Queue {
        return Queue("$QUEUE_TASKLOG_DEFECT_WEBSOCKET${IPUtils.getInnerIP().replace(".", "")}$localPort")
    }

    @Bean
    fun websocketDefectBind(
        websocketDefectExchange: FanoutExchange,
        websocketDefectQueue: Queue
    ) : Binding {
        return BindingBuilder.bind(websocketDefectQueue)
            .to(websocketDefectExchange)
    }

    @Bean
    open fun externalJobListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired websocketDefectQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired websocketConsumer: WebsocketConsumer,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        rabbitAdmin.declareQueue(websocketDefectQueue)
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(websocketDefectQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(16)
        container.setPrefetchCount(1)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(10000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(websocketConsumer, websocketConsumer::sendWebsocketMsg.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}