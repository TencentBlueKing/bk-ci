package com.tencent.devops.process.engine.init

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.process.engine.listener.run.PipelineNotifyQueueListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelineNotifyQueueConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["pipelineBuildFanoutExchange"])
    fun pipelineBuildFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun notifyQueueBuildFinishQueue(): Queue {
        return Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_NOTIFY_QUEUE)
    }

    @Bean
    fun notifyQueueBuildFinishBind(
        notifyQueueBuildFinishQueue: Queue,
        pipelineBuildFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(notifyQueueBuildFinishQueue).to(pipelineBuildFanoutExchange)
    }

    @Bean
    fun notifyQueueBuildFinishListenerContainer(
        connectionFactory: ConnectionFactory,
        notifyQueueBuildFinishQueue: Queue,
        rabbitAdmin: RabbitAdmin,
        buildListener: PipelineNotifyQueueListener,
        messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = notifyQueueBuildFinishQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 5
        )
    }
}
