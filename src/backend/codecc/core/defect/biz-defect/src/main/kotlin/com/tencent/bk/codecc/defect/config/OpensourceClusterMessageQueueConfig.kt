package com.tencent.bk.codecc.defect.config

import com.tencent.bk.codecc.defect.component.DefectClusterComponent
import com.tencent.devops.common.util.IPUtils
import com.tencent.devops.common.web.mq.*
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "spring.application", name = ["name"], havingValue = "opensourcereport")
class OpensourceClusterMessageQueueConfig {

    @Value("\${server.port:#{null}}")
    private val localPort: String? = null

    @Bean
    fun opensourceClusterExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE, true, false)
    }

    @Bean
    fun opensourceClusterQueue() = Queue(QUEUE_CLUSTER_ALLOCATION_OPENSOURCE)

    @Bean
    fun opensourceClusterReplyQueue() = Queue("$QUEUE_REPLY_CLUSTER_ALLOCATION_OPENSOURCE.${IPUtils.getInnerIP().replace(".", "")}.$localPort")

    @Bean
    fun opensourceClusterQueueBind(
        @Autowired opensourceClusterQueue: Queue,
        @Autowired opensourceClusterExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(opensourceClusterQueue).
            to(opensourceClusterExchange).with(ROUTE_CLUSTER_ALLOCATION_OPENSOURCE)
    }


    @Bean
    fun opensourceClusterContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired opensourceClusterQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired defectClusterComponent: DefectClusterComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(opensourceClusterQueue.name)
        container.setConcurrentConsumers(20)
        container.setMaxConcurrentConsumers(20)
        container.setStartConsumerMinInterval(5)
        container.setConsecutiveActiveTrigger(5)
        container.setAmqpAdmin(rabbitAdmin)
        //确保只有一个消费者消费，保证负载不超时
        val adapter = MessageListenerAdapter(defectClusterComponent, defectClusterComponent::executeClusterNew.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }


    @Bean
    fun opensourceClusterReplyContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired opensourceClusterReplyQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        //ip地址做队列名
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(opensourceClusterReplyQueue.name)
        container.setConcurrentConsumers(20)
        container.setMaxConcurrentConsumers(20)
        container.setStartConsumerMinInterval(2)
        container.setConsecutiveActiveTrigger(2)
        container.setAmqpAdmin(rabbitAdmin)
        return container
    }

    @Bean
    fun opensourceAsyncRabbitTamplte(rabbitTemplate: RabbitTemplate, opensourceClusterReplyContainer: SimpleMessageListenerContainer) : AsyncRabbitTemplate {
        val asyncRabbitTemplate = AsyncRabbitTemplate(rabbitTemplate, opensourceClusterReplyContainer)
        asyncRabbitTemplate.setReceiveTimeout(7200000)
        return asyncRabbitTemplate
    }

}