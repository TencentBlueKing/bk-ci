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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.lambda.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.lambda.listener.LambdaBuildCommitFinishListener
import com.tencent.devops.lambda.listener.LambdaBuildTaskFinishListener
import com.tencent.devops.lambda.listener.LambdaBuildFinishListener
import com.tencent.devops.lambda.listener.LambdaPipelineModelListener
import com.tencent.devops.lambda.listener.LambdaProjectListener
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
class LambdaMQConfiguration {

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
        @Autowired lambdaBuildFinishListener: LambdaBuildFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildFinishLambdaQueue.name)
        container.setConcurrentConsumers(30)
        container.setMaxConcurrentConsumers(30)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(lambdaBuildFinishListener, lambdaBuildFinishListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
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
        @Autowired lambdaBuildTaskFinishListener: LambdaBuildTaskFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildElementFinishLambdaQueue.name)
        container.setConcurrentConsumers(30)
        container.setMaxConcurrentConsumers(30)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(lambdaBuildTaskFinishListener, lambdaBuildTaskFinishListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    /**
     * 构建project创建广播交换机
     */
    @Bean
    fun projectCreateFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_CREATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectCreateLambdaQueue() = Queue(LambdaMQ.QUEUE_PROJECT_CREATE_LAMBDA_EVENT)

    @Bean
    fun projectCreateLambdaQueueBind(
        @Autowired projectCreateLambdaQueue: Queue,
        @Autowired projectCreateFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(projectCreateLambdaQueue).to(projectCreateFanoutExchange)
    }

    @Bean
    fun projectCreateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectCreateLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired lambdaProjectListener: LambdaProjectListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectCreateLambdaQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(lambdaProjectListener, lambdaProjectListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    /**
     * 构建project更新广播交换机
     */
    @Bean
    fun projectUpdateFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PROJECT_UPDATE_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun projectUpdateLambdaQueue() = Queue(LambdaMQ.QUEUE_PROJECT_UPDATE_LAMBDA_EVENT)

    @Bean
    fun projectUpdateLambdaQueueBind(
        @Autowired projectUpdateLambdaQueue: Queue,
        @Autowired projectUpdateFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(projectUpdateLambdaQueue).to(projectUpdateFanoutExchange)
    }

    @Bean
    fun projectUpdateListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired projectUpdateLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired lambdaProjectListener: LambdaProjectListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(projectUpdateLambdaQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(10)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(lambdaProjectListener, lambdaProjectListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    /**
     * 构建model更新广播交换机
     */
    @Bean
    fun pipelineModelAnalysisFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_EXTENDS_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineModelAnalysisLambdaQueue() = Queue(LambdaMQ.QUEUE_PIPELINE_EXTENDS_MODEL_LAMBDA)

    @Bean
    fun pipelineModelAnalysisLambdaQueueBind(
        @Autowired pipelineModelAnalysisLambdaQueue: Queue,
        @Autowired pipelineModelAnalysisFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineModelAnalysisLambdaQueue).to(pipelineModelAnalysisFanoutExchange)
    }

    @Bean
    fun pipelineModelAnalysisListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineModelAnalysisLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired lambdaPipelineModelListener: LambdaPipelineModelListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineModelAnalysisLambdaQueue.name)
        container.setConcurrentConsumers(5)
        container.setMaxConcurrentConsumers(5)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(lambdaPipelineModelListener, lambdaPipelineModelListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }

    /**
     * webhook commits完成事件交换机
     */
    @Bean
    fun pipelineBuildCommitFinishFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(
            MQ.EXCHANGE_PIPELINE_BUILD_COMMIT_FINISH_FANOUT, true, false
        )
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineBuildCommitFinishLambdaQueue() = Queue(LambdaMQ.QUEUE_PIPELINE_BUILD_COMMIT_FINISH_LAMBDA)

    @Bean
    fun pipelineBuildCommitsFinishQueueBind(
        @Autowired pipelineBuildCommitFinishLambdaQueue: Queue,
        @Autowired pipelineBuildCommitFinishFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineBuildCommitFinishLambdaQueue).to(pipelineBuildCommitFinishFanoutExchange)
    }

    @Bean
    fun pipelineBuildCommitsFinishListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineBuildCommitFinishLambdaQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired lambdaBuildCommitFinishListener: LambdaBuildCommitFinishListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(pipelineBuildCommitFinishLambdaQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(30)
        container.setAmqpAdmin(rabbitAdmin)
        container.setPrefetchCount(1)

        val adapter = MessageListenerAdapter(
            lambdaBuildCommitFinishListener,
            LambdaBuildCommitFinishListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        container.setMessageListener(adapter)
        return container
    }
}
