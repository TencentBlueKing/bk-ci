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

package com.tencent.devops.process.engine.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.StreamEventConsumer
import com.tencent.devops.common.event.dispatcher.mq.MQRoutableEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.engine.listener.run.PipelineAtomTaskBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineBuildStartListener
import com.tencent.devops.process.engine.listener.run.PipelineContainerBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineStageBuildListener
import com.tencent.devops.process.engine.listener.run.PipelineTaskPauseListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildCancelListener
import com.tencent.devops.process.engine.listener.run.finish.PipelineBuildFinishListener
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.slf4j.MDC
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.Message
import java.util.function.Consumer

/**
 * 流水线构建核心配置
 */
@Configuration
class BuildEngineCoreBuildConfiguration {

    companion object {
        private const val STREAM_CONSUMER_GROUP = "engine-service"
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) =
        Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun rabbitAdmin(
        connectionFactory: ConnectionFactory
    ): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        rabbitTemplate.addBeforePublishPostProcessors(MessagePostProcessor { message ->
            val traceId = MDC.get(TraceTag.BIZID)?.ifBlank { TraceTag.buildBiz() }
            message.messageProperties.setHeader(TraceTag.X_DEVOPS_RID, traceId)
            message
        })
        return rabbitTemplate
    }

    @Bean
    fun routableEventDispatcher(
        @Autowired rabbitTemplate: RabbitTemplate
    ) = MQRoutableEventDispatcher(
        rabbitTemplate = rabbitTemplate
    )

    /**
     * 入口：整个构建开始队列---- 并发一般
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_START, STREAM_CONSUMER_GROUP)
    fun buildStartListener(
        @Autowired buildListener: PipelineBuildStartListener
    ): Consumer<Message<PipelineBuildStartEvent>> {
        return Consumer { event: Message<PipelineBuildStartEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 构建结束队列--- 并发一般，与Stage一致
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_FINISH, STREAM_CONSUMER_GROUP)
    fun buildFinishListener(
        @Autowired buildListener: PipelineBuildFinishListener
    ): Consumer<Message<PipelineBuildFinishEvent>> {
        return Consumer { event: Message<PipelineBuildFinishEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 构建取消队列--- 并发一般，与Stage一致
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_CANCEL, STREAM_CONSUMER_GROUP)
    fun buildCancelListener(
        @Autowired buildListener: PipelineBuildCancelListener
    ): Consumer<Message<PipelineBuildCancelEvent>> {
        return Consumer { event: Message<PipelineBuildCancelEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * Stage构建队列---- 并发一般
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_STAGE, STREAM_CONSUMER_GROUP)
    fun stageBuildListener(
        @Autowired buildListener: PipelineStageBuildListener
    ): Consumer<Message<PipelineBuildStageEvent>> {
        return Consumer { event: Message<PipelineBuildStageEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * Job构建队列---- 并发一般
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_CONTAINER, STREAM_CONSUMER_GROUP)
    fun containerBuildListener(
        @Autowired buildListener: PipelineContainerBuildListener
    ): Consumer<Message<PipelineBuildContainerEvent>> {
        return Consumer { event: Message<PipelineBuildContainerEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 任务队列---- 并发要大
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_BUILD_TASK_START, STREAM_CONSUMER_GROUP)
    fun taskBuildListener(
        @Autowired buildListener: PipelineAtomTaskBuildListener
    ): Consumer<Message<PipelineBuildAtomTaskEvent>> {
        return Consumer { event: Message<PipelineBuildAtomTaskEvent> ->
            buildListener.run(event.payload)
        }
    }

    /**
     * 流水线暂停操作队列
     */
    @StreamEventConsumer(StreamBinding.QUEUE_PIPELINE_PAUSE_TASK_EXECUTE, STREAM_CONSUMER_GROUP)
    fun taskPauseListener(
        @Autowired buildListener: PipelineTaskPauseListener
    ): Consumer<Message<PipelineTaskPauseEvent>> {
        return Consumer { event: Message<PipelineTaskPauseEvent> ->
            buildListener.run(event.payload)
        }
    }
}
