package com.tencent.devops.plugin.codecc.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.plugin.codecc.event.listener.ChangeCodeCCListener
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MQ配置
 */
@Configuration
class MQConfiguration {

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    /**
     * 声明 流水线设置变更 事件交换机
     */
    @Bean
    fun pipelineSettingChangeExchange(): FanoutExchange {
        val pipelineSettingChangeExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT, true, false)
        pipelineSettingChangeExchange.isDelayed = true
        return pipelineSettingChangeExchange
    }

    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @Bean
    fun pipelineSettingChangeQueue() = Queue(MQ.QUEUE_PIPELINE_SETTING_CHANGE)

    @Bean
    fun pipelineSettingChangeQueueBind(
        @Autowired pipelineSettingChangeQueue: Queue,
        @Autowired pipelineSettingChangeExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineSettingChangeQueue).to(pipelineSettingChangeExchange)
    }

    @Bean
    fun pipelineSettingChangeQueueListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineSettingChangeQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired changeCodeCCListener: ChangeCodeCCListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineSettingChangeQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = changeCodeCCListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 20000,
            consecutiveActiveTrigger = 3,
            concurrency = 1,
            maxConcurrency = 10
        )
    }
}