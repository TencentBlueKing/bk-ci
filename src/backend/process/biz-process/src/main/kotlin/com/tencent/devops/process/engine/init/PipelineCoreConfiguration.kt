/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineCreateListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineDeleteListener
import com.tencent.devops.process.engine.listener.pipeline.MQPipelineUpdateListener
import com.tencent.devops.process.engine.listener.run.PipelineAtomTaskBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineBuildStartListener
import com.tencent.devops.process.engine.listener.run.PipelineContainerBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineStageBuildListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildCancelListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildFinishListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 流水线构建核心配置
 */
@Configuration
class PipelineCoreConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun pipelineCoreExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.ENGINE_PROCESS_LISTENER_EXCHANGE, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Value("\${queueConcurrency.buildStart:5}")
    private val buildStartConcurrency: Int? = null

    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @Bean
    fun pipelineBuildStartQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_START)

    @Bean
    fun pipelineBuildStartQueueBind(
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildStartQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_START)
    }

    @Bean
    fun pipelineStageBuildStartListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildStartListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildStartQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = buildStartConcurrency!!,
            maxConcurrency = 20
        )
    }

    @Value("\${queueConcurrency.stage:5}")
    private val stageConcurrency: Int? = null

    /**
     * Stage构建队列---- 并发一般
     */
    @Bean
    fun pipelineBuildStageQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_STAGE)

    @Bean
    fun pipelineBuildStageQueueBind(
        @Autowired pipelineBuildStageQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildStageQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_STAGE)
    }

    @Bean
    fun pipelineStageBuildStageListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildStageQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineStageBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildStageQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = stageConcurrency!!,
            maxConcurrency = 20
        )
    }

    @Value("\${queueConcurrency.container:5}")
    private val containerConcurrency: Int? = null

    /**
     * Job构建队列---- 并发一般
     */
    @Bean
    fun pipelineBuildContainerQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_CONTAINER)

    @Bean
    fun pipelineBuildContainerQueueBind(
        @Autowired pipelineBuildContainerQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildContainerQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_CONTAINER)
    }

    @Bean
    fun pipelineContainerBuildListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildContainerQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired pipelineContainerBuildListener: PipelineContainerBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildContainerQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = pipelineContainerBuildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = containerConcurrency!!,
            maxConcurrency = 20
        )
    }

    @Value("\${queueConcurrency.task:10}")
    private val taskConcurrency: Int? = null

    /**
     * 任务队列---- 并发要大
     */
    @Bean
    fun pipelineBuildTaskQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_TASK_START)

    @Bean
    fun pipelineBuildTaskQueueBind(
        @Autowired pipelineBuildTaskQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildTaskQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_BUILD_TASK_START)
    }

    @Bean
    fun pipelineAtomTaskBuildListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildTaskQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineAtomTaskBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildTaskQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 1,
            concurrency = taskConcurrency!!,
            maxConcurrency = 50
        )
    }

    /**
     * 本机专属任务队列
     */
    @Bean
    fun localPipelineBuildTaskQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_TASK_START + CommonUtils.getInnerIP())

    @Bean
    fun localPipelineBuildTaskQueueBind(
        @Autowired localPipelineBuildTaskQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(localPipelineBuildTaskQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_TASK_START + CommonUtils.getInnerIP())
    }

    @Bean
    fun localPipelineAtomTaskBuildListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired localPipelineBuildTaskQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineAtomTaskBuildListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = localPipelineBuildTaskQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 5000,
            consecutiveActiveTrigger = 1,
            concurrency = 1,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.buildFinish:5}")
    private val buildFinishConcurrency: Int? = null

    /**
     * 构建结束队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildFinishQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_FINISH)

    @Bean
    fun pipelineBuildFinishQueueBind(
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildFinishQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_FINISH)
    }

    @Bean
    fun pipelineBuildFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildFinishQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildFinishQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 15000,
            consecutiveActiveTrigger = 5,
            concurrency = buildFinishConcurrency!!,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.buildCancel:5}")
    private val buildCancelConcurrency: Int? = null

    /**
     * 构建取消队列--- 并发一般，与Stage一致
     */
    @Bean
    fun pipelineBuildCancelQueue() = Queue(MQ.QUEUE_PIPELINE_BUILD_CANCEL)

    @Bean
    fun pipelineBuildCancelQueueBind(
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildCancelQueue)
            .to(pipelineCoreExchange)
            .with(MQ.ROUTE_PIPELINE_BUILD_CANCEL)
    }

    @Bean
    fun pipelineBuildCancelListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildCancelQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildListener: PipelineBuildCancelListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineBuildCancelQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = buildListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 15000,
            consecutiveActiveTrigger = 5,
            concurrency = buildCancelConcurrency!!,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.pipelineCreate:5}")
    private val pipelineCreateConcurrency: Int? = null

    /**
     * 流水线创建队列--- 并发小
     */
    @Bean
    fun pipelineCreateQueue() = Queue(MQ.QUEUE_PIPELINE_CREATE)

    @Bean
    fun pipelineCreateQueueBind(
        @Autowired pipelineCreateQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineCreateQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_CREATE)
    }

    @Bean
    fun pipelineCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineCreateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired createListener: MQPipelineCreateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineCreateQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = createListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineCreateConcurrency!!,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.pipelineDelete:2}")
    private val pipelineDeleteConcurrency: Int? = null

    /**
     * 流水线删除队列--- 并发小
     */
    @Bean
    fun pipelineDeleteQueue() = Queue(MQ.QUEUE_PIPELINE_DELETE)

    @Bean
    fun pipelineDeleteQueueBind(
        @Autowired pipelineDeleteQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineDeleteQueue)
            .to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_DELETE)
    }

    @Bean
    fun pipelineDeleteListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineDeleteQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired deleteListener: MQPipelineDeleteListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineDeleteQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = deleteListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineDeleteConcurrency!!,
            maxConcurrency = 10
        )
    }

    @Value("\${queueConcurrency.pipelineUpdate:5}")
    private val pipelineUpdateConcurrency: Int? = null

    /**
     * 流水线更新队列--- 并发一般
     */
    @Bean
    fun pipelineUpdateQueue() = Queue(MQ.QUEUE_PIPELINE_UPDATE)

    @Bean
    fun pipelineUpdateQueueBind(
        @Autowired pipelineUpdateQueue: Queue,
        @Autowired pipelineCoreExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(pipelineUpdateQueue).to(pipelineCoreExchange).with(MQ.ROUTE_PIPELINE_UPDATE)
    }

    @Bean
    fun pipelineUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineUpdateQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired updateListener: MQPipelineUpdateListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {

        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineUpdateQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = updateListener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = pipelineUpdateConcurrency!!,
            maxConcurrency = 10
        )
    }

    /**
     * 声明 流水线设置变更 事件交换机
     */
    @Bean
    fun pipelineSettingChangeExchange(): FanoutExchange {
        val pipelineSettingChangeExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_SETTING_CHANGE_FANOUT, true, false)
        pipelineSettingChangeExchange.isDelayed = true
        return pipelineSettingChangeExchange
    }
}