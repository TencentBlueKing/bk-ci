package com.tencent.devops.remotedev.config.async

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.remotedev.MQ
import com.tencent.devops.remotedev.listener.AsyncExecuteListener
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * remotedev 服务内作为更安全的线程池的异步实现方式
 */
@Configuration
class AsyncExecuteMq {
    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun eventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    /**
     * 构建结束广播交换机
     */
    @Bean
    fun asyncExecuteExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_REMOTE_DEV_ASYNC_EXECUTE, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun asyncExecuteQueue() = Queue(MQ.QUEUE_REMOTE_DEV_ASYNC_EXECUTE)

    @Bean
    fun asyncExecuteQueueBind(
        @Autowired asyncExecuteQueue: Queue,
        @Autowired asyncExecuteExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(asyncExecuteQueue).to(asyncExecuteExchange)
    }

    @Bean
    fun requestTriggerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired asyncExecuteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired asyncExecuteListener: AsyncExecuteListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = asyncExecuteQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = MessageListenerAdapter(
                asyncExecuteListener,
                asyncExecuteListener::listenAsyncExecuteEvent.name
            ).also { it.setMessageConverter(messageConverter) },
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 30,
            maxConcurrency = 30,
            prefetchCount = 1
        )
    }
}