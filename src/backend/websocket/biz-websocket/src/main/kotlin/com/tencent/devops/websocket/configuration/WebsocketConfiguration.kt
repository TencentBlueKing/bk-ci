package com.tencent.devops.websocket.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.websocket.handler.ConnectChannelInterceptor
import com.tencent.devops.websocket.listener.WebSocketListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.InetAddress

@Component
class WebsocketConfiguration {

    @Value("\${queueConcurrency.pipelineWebSocket:1}")
    private val webSocketQueueConcurrency: Int? = null

    @Value("\${activeTrigger.pipelineWebSocket:10}")
    private val webSocketActiveTrigger: Int? = null

    @Bean
    fun websocketDispatcher(rabbitTemplate: RabbitTemplate) = WebSocketDispatcher(rabbitTemplate)

    /**
     * 构建广播交换机
     */
    @Bean
    fun pipelineWebSocketFanoutExchange(): FanoutExchange {
        return FanoutExchange(MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, false, false)
    }

    @Bean
    fun rabbitAdmin(
        @Autowired connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun pipelineWebSocketQueue(): Queue {
        val address = InetAddress.getLocalHost()
        return Queue(MQ.QUEUE_WEBSOCKET_TMP_EVENT + "." + address.hostAddress)
//        return QueueBuilder.nonDurable().autoDelete().exclusive().build()
    }

    @Bean
    fun pipelineQueueBinding(
        @Autowired pipelineWebSocketQueue: Queue,
        @Autowired pipelineWebSocketFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineWebSocketQueue).to(pipelineWebSocketFanoutExchange)
    }

    @Bean
    fun webSocketListenerContainer(
            @Autowired connectionFactory: ConnectionFactory,
            @Autowired rabbitAdmin: RabbitAdmin,
            @Autowired messageConverter: Jackson2JsonMessageConverter,
            @Autowired pipelineWebSocketQueue: Queue,
            @Autowired buildListener: WebSocketListener
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineWebSocketQueue.name)
        container.setConcurrentConsumers(webSocketQueueConcurrency!!)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(5000)
        container.setConsecutiveActiveTrigger(webSocketActiveTrigger!!)
        container.setMismatchedQueuesFatal(true)
        val adapter = MessageListenerAdapter(buildListener, buildListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    fun connectChannelInterceptor(
        redisOperation: RedisOperation
    ): ConnectChannelInterceptor {
        return ConnectChannelInterceptor(redisOperation)
    }
}