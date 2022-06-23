package com.tencent.bk.codecc.defect.config

import com.tencent.bk.codecc.defect.component.DefectClusterComponent
import com.tencent.bk.codecc.defect.condition.AsyncReportCondition
import com.tencent.devops.common.util.IPUtils
import com.tencent.devops.common.web.mq.EXCHANGE_CLUSTER_ALLOCATION
import com.tencent.devops.common.web.mq.QUEUE_CLUSTER_ALLOCATION
import com.tencent.devops.common.web.mq.QUEUE_REPLY_CLUSTER_ALLOCATION
import com.tencent.devops.common.web.mq.ROUTE_CLUSTER_ALLOCATION
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration

@Configuration
@Conditional(AsyncReportCondition::class)
open class ClusterMessageQueueConfig {

    @Value("\${server.port:#{null}}")
    private val localPort: String? = null

    @Bean
    open fun clusterExchange(): DirectExchange {
        return DirectExchange(EXCHANGE_CLUSTER_ALLOCATION, true, false)
    }

    @Bean
    open fun clusterQueue() = Queue(QUEUE_CLUSTER_ALLOCATION)

    @Bean
    open fun clusterReplyQueue() = Queue("$QUEUE_REPLY_CLUSTER_ALLOCATION.${IPUtils.getInnerIP().replace(".", "")}.$localPort")

    @Bean
    open fun clusterQueueBind(
        @Autowired clusterQueue: Queue,
        @Autowired clusterExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(clusterQueue).
            to(clusterExchange).with(ROUTE_CLUSTER_ALLOCATION)
    }

    @Bean
    open fun clusterContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired clusterQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired defectClusterComponent: DefectClusterComponent,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(clusterQueue.name)
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
    open fun clusterReplyContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired clusterReplyQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        //ip地址做队列名
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(clusterReplyQueue.name)
        container.setConcurrentConsumers(20)
        container.setMaxConcurrentConsumers(20)
        container.setStartConsumerMinInterval(2)
        container.setConsecutiveActiveTrigger(2)
        container.setAmqpAdmin(rabbitAdmin)
        return container
    }

    @Bean
    open fun clusterAsyncRabbitTamplte(rabbitTemplate: RabbitTemplate, clusterReplyContainer: SimpleMessageListenerContainer) : AsyncRabbitTemplate {
        val asyncRabbitTemplate = AsyncRabbitTemplate(rabbitTemplate, clusterReplyContainer)
        asyncRabbitTemplate.setReceiveTimeout(7200000)
        return asyncRabbitTemplate
    }

}