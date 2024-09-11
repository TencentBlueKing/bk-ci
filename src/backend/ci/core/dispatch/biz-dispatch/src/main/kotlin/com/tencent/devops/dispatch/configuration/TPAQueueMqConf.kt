package com.tencent.devops.dispatch.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.dispatch.listener.TPAQueueListener
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

@Configuration
class TPAQueueMqConf {
    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun eventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun tpAgentQueueExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_THIRD_PARTY_AGENT_QUEUE, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun tpAgentQueueQueue() = Queue(MQ.QUEUE_THIRD_PARTY_AGENT_QUEUE)

    @Bean
    fun tpAgentQueueQueueBind(
        @Autowired tpAgentQueueQueue: Queue,
        @Autowired tpAgentQueueExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(tpAgentQueueQueue).to(tpAgentQueueExchange)
    }

    @Bean
    fun requestTriggerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired tpAgentQueueQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired tpAgentQueueListener: TPAQueueListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = tpAgentQueueQueue,
            rabbitAdmin = rabbitAdmin,
            adapter = MessageListenerAdapter(
                tpAgentQueueListener,
                tpAgentQueueListener::listenTpAgentQueueEvent.name
            ).also { it.setMessageConverter(messageConverter) },
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 50,
            maxConcurrency = 100,
            prefetchCount = 1
        )
    }
}