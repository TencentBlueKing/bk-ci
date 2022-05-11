package com.tencent.bk.codecc.quartz.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.quartz.mq.JobChangeListener
import com.tencent.devops.common.event.util.IPUtils
import com.tencent.devops.common.web.mq.EXCHANGE_EXTERNAL_JOB
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_DELETE_ALL_JOB
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_INIT_ALL_JOB
import com.tencent.devops.common.web.mq.EXCHANGE_INTERNAL_JOB
import com.tencent.devops.common.web.mq.QUEUE_EXTERNAL_JOB
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_DELETE_ALL_JOB
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_INIT_ALL_JOB
import com.tencent.devops.common.web.mq.QUEUE_INTERNAL_JOB
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
open class JobMessageListenerConfig {

    @Value("\${server.port:#{null}}")
    private val localPort: String? = null

    @Bean
    open fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    open fun externalJobExchange(): FanoutExchange {
        //auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val fanoutExchange = FanoutExchange(EXCHANGE_EXTERNAL_JOB, false, true)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    open fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    open fun externalJobMsgQueue() = Queue("$QUEUE_EXTERNAL_JOB${IPUtils.getInnerIP().replace(".", "")}$localPort")

    @Bean
    open fun externalJobQueueBind(
        @Autowired externalJobMsgQueue: Queue,
        @Autowired externalJobExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(externalJobMsgQueue)
            .to(externalJobExchange)
    }

    @Bean
    open fun externalJobListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired externalJobMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired jobChangeListener: JobChangeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(externalJobMsgQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(10000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(jobChangeListener, jobChangeListener::externalJobMsg.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    @Bean
    open fun internalJobExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_INTERNAL_JOB, false, true)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    open fun internalJobMsgQueue() = Queue("$QUEUE_INTERNAL_JOB${IPUtils.getInnerIP()
        .replace(".", "")}$localPort")

    @Bean
    open fun internalJobQueueBind(
        @Autowired internalJobMsgQueue: Queue,
        @Autowired internalJobExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(internalJobMsgQueue)
            .to(internalJobExchange)
    }

    @Bean
    open fun internalJobListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired internalJobMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired jobChangeListener: JobChangeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(internalJobMsgQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(10000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(jobChangeListener, jobChangeListener::internalJobMsg.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }


    @Bean
    open fun deleteJobExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_GONGFENG_DELETE_ALL_JOB, false, true)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    open fun deleteJobMsgQueue() =
        Queue("$QUEUE_GONGFENG_DELETE_ALL_JOB${IPUtils.getInnerIP()
            .replace(".", "")}$localPort")

    @Bean
    open fun deleteJobQueueBind(
        @Autowired deleteJobMsgQueue: Queue,
        @Autowired deleteJobExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(deleteJobMsgQueue)
            .to(deleteJobExchange)
    }

    @Bean
    open fun deleteJobListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired deleteJobMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired jobChangeListener: JobChangeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(deleteJobMsgQueue.name)
        container.setConcurrentConsumers(2)
        container.setMaxConcurrentConsumers(5)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(10000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(jobChangeListener, jobChangeListener::deleteJobMsg.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }


    @Bean
    open fun initJobExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_GONGFENG_INIT_ALL_JOB, false, true)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    open fun initJobMsgQueue() = Queue("$QUEUE_GONGFENG_INIT_ALL_JOB${IPUtils.getInnerIP()
        .replace(".", "")}$localPort")

    @Bean
    open fun initJobQueueBind(
        @Autowired initJobMsgQueue: Queue,
        @Autowired initJobExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(initJobMsgQueue)
            .to(initJobExchange)
    }

    @Bean
    open fun initJobListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired initJobMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired jobChangeListener: JobChangeListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(initJobMsgQueue.name)
        container.setConcurrentConsumers(2)
        container.setMaxConcurrentConsumers(5)
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(10000)
        container.setConsecutiveActiveTrigger(5)
        val adapter = MessageListenerAdapter(jobChangeListener, jobChangeListener::initJobMsg.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
