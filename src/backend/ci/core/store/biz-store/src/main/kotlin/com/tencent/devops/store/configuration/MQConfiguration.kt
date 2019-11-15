package com.tencent.devops.store.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.store.listener.StorePipelineBuildFinishListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建扩展配置
 */
@Configuration
class MQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Value("\${queueConcurrency.market:3}")
    private val marketConcurrency: Int? = null

    @Bean
    fun pipelineBuildAtomMarketQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_ATOM_MARKET)

    /**
     * 构建结束广播交换机
     */
    @Bean
    fun pipelineBuildFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildAtomMarketQueueBind(
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishQueue)
            .to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun pipelineBuildStoreListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildAtomMarketQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: StorePipelineBuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildAtomMarketQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 120000,
            consecutiveActiveTrigger = 10,
            concurrency = marketConcurrency!!,
            maxConcurrency = 10
        )
    }
}