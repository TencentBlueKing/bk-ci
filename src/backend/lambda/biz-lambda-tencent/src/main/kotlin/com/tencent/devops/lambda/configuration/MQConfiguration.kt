package com.tencent.devops.lambda.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.lambda.listener.BuildElementFinishListener
import com.tencent.devops.lambda.listener.BuildFinishListener
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
class MQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

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
    fun buildFinishLambdaQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH_LAMBDA)

    @Bean
    fun buildFinishLambdaQueueBind(
        @Autowired buildFinishLambdaQueue: Queue,
        @Autowired pipelineBuildFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(buildFinishLambdaQueue).to(pipelineBuildFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildFinishLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildFinishListener: BuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildFinishLambdaQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(buildFinishListener, buildFinishListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    /**
     * 构建结束广播交换机
     */
    @Bean
    fun pipelineBuildElementFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_BUILD_ELEMENT_FINISH_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun buildElementFinishLambdaQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_ELEMENT_FINISH_LAMBDA)

    @Bean
    fun buildElementFinishLambdaQueueBind(
        @Autowired buildElementFinishLambdaQueue: Queue,
        @Autowired pipelineBuildElementFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(buildElementFinishLambdaQueue).to(pipelineBuildElementFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildElementFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildElementFinishLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildElementFinishListener: BuildElementFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildElementFinishLambdaQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(10)
        container.setRabbitAdmin(rabbitAdmin)

        val adapter = MessageListenerAdapter(buildElementFinishListener, buildElementFinishListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}