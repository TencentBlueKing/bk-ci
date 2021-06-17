package com.tencent.bk.codecc.task.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.listener.GongfengCreateTaskListener
import com.tencent.bk.codecc.task.listener.GongfengFailRecordListener
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_RETRY_TRIGGER
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_STAT_SYNC
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_STAT_SYNC_NEW
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_ACTIVE_PROJECT
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_RETRY_TRIGGER
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_STAT_SYNC
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_STAT_SYNC_NEW
import com.tencent.devops.common.web.mq.QUEUE_GONGFENG_TRIGGER_PIPELINE
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_ACTIVE_PROJECT
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_RETRY_TRIGGER
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_STAT_SYNC
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_STAT_SYNC_NEW
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_TRIGGER_PIPELINE
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GongfengMessagListenerConfig {

    @Bean
    open fun rabbitAdmin(@Autowired connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    open fun gongfengTaskExchange(): DirectExchange {
        // auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val directExchange = DirectExchange(EXCHANGE_GONGFENG_CODECC_SCAN, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    open fun createTaskMsgQueue() = Queue(QUEUE_GONGFENG_CODECC_SCAN)

    @Bean
    open fun createTaskQueueBind(
        @Autowired createTaskMsgQueue: Queue,
        @Autowired gongfengTaskExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(createTaskMsgQueue)
            .to(gongfengTaskExchange).with(ROUTE_GONGFENG_CODECC_SCAN)
    }

    @Bean
    open fun createTaskListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired createTaskMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengCreateTaskListener: GongfengCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(createTaskMsgQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter =
            MessageListenerAdapter(gongfengCreateTaskListener, gongfengCreateTaskListener::executeCreateTask.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    open fun updateGongfengStatExchange(): DirectExchange {
        // auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val directExchange = DirectExchange(EXCHANGE_GONGFENG_STAT_SYNC, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun updateGongfengStatMsgQueue() = Queue(QUEUE_GONGFENG_STAT_SYNC)

    @Bean
    open fun udpateGongfengStatQueueBind(
        @Autowired updateGongfengStatMsgQueue: Queue,
        @Autowired updateGongfengStatExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(updateGongfengStatMsgQueue)
            .to(updateGongfengStatExchange).with(ROUTE_GONGFENG_STAT_SYNC)
    }

    @Bean
    open fun updateGongfengListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired updateGongfengStatMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengCreateTaskListener: GongfengCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(updateGongfengStatMsgQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter =
            MessageListenerAdapter(gongfengCreateTaskListener, gongfengCreateTaskListener::updateGongfengStatInfo.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    open fun updateGongfengStatNewExchange(): DirectExchange {
        // auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val directExchange = DirectExchange(EXCHANGE_GONGFENG_STAT_SYNC_NEW, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun updateGongfengStatNewMsgQueue() = Queue(QUEUE_GONGFENG_STAT_SYNC_NEW)

    @Bean
    open fun udpateGongfengStatNewQueueBind(
        @Autowired updateGongfengStatNewMsgQueue: Queue,
        @Autowired updateGongfengStatNewExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(updateGongfengStatNewMsgQueue)
            .to(updateGongfengStatNewExchange).with(ROUTE_GONGFENG_STAT_SYNC_NEW)
    }

    @Bean
    open fun updateGongfengNewListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired updateGongfengStatNewMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengCreateTaskListener: GongfengCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(updateGongfengStatNewMsgQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        // 确保只有一个消费者消费，保证负载不超时
        val adapter =
            MessageListenerAdapter(gongfengCreateTaskListener, gongfengCreateTaskListener::syncGongfengStatInfo.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    open fun triggerPipelineMsgQueue() = Queue(QUEUE_GONGFENG_TRIGGER_PIPELINE)

    @Bean
    open fun triggerPipelineQueueBind(
        @Autowired triggerPipelineMsgQueue: Queue,
        @Autowired gongfengTaskExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(triggerPipelineMsgQueue)
            .to(gongfengTaskExchange).with(ROUTE_GONGFENG_TRIGGER_PIPELINE)
    }

    @Bean
    open fun triggerPipelineListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired triggerPipelineMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengCreateTaskListener: GongfengCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(triggerPipelineMsgQueue.name)
        container.setConcurrentConsumers(2)
        container.setMaxConcurrentConsumers(2)
        container.setRabbitAdmin(rabbitAdmin)
        // 除了最小消费者外的消费者都为备用消费者，只有当主消费者负载都满了时候才开新消费者
        val adapter =
            MessageListenerAdapter(gongfengCreateTaskListener, gongfengCreateTaskListener::executeTriggerPipeline.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    open fun createActiveProjMsgQueue() = Queue(QUEUE_GONGFENG_ACTIVE_PROJECT)

    @Bean
    open fun createActiveProjQueueBind(
        @Autowired createActiveProjMsgQueue: Queue,
        @Autowired gongfengTaskExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(createActiveProjMsgQueue)
            .to(gongfengTaskExchange).with(ROUTE_GONGFENG_ACTIVE_PROJECT)
    }

    @Bean
    open fun createActiveProjListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired createActiveProjMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengCreateTaskListener: GongfengCreateTaskListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(createActiveProjMsgQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(20)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        val adapter = MessageListenerAdapter(
            gongfengCreateTaskListener,
            gongfengCreateTaskListener::executeActiveProjectTask.name
        )
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }

    @Bean
    open fun newTaskRetryExchange(): DirectExchange {
        // auto-delete设置为true，因为如果相应节点下线，则queue自动删除
        val directExchange = DirectExchange(EXCHANGE_GONGFENG_RETRY_TRIGGER, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    open fun newTaskRetryMsgQueue() = Queue(QUEUE_GONGFENG_RETRY_TRIGGER)

    @Bean
    open fun newTaskRetryQueueBind(
        @Autowired newTaskRetryMsgQueue: Queue,
        @Autowired newTaskRetryExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(newTaskRetryMsgQueue)
            .to(newTaskRetryExchange).with(ROUTE_GONGFENG_RETRY_TRIGGER)
    }

    @Bean
    open fun newTaskRetryListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired newTaskRetryMsgQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired gongfengFailRecordListener: GongfengFailRecordListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(newTaskRetryMsgQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(1)
        container.setRabbitAdmin(rabbitAdmin)
        val adapter =
            MessageListenerAdapter(gongfengFailRecordListener, gongfengFailRecordListener::openSourceFailTaskRetry.name)
        adapter.setMessageConverter(messageConverter)
        container.messageListener = adapter
        return container
    }
}
