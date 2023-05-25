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

package com.tencent.devops.common.event.dispatcher.pipeline.mq

import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import kotlin.math.max

object Tools {

    private val logger = LoggerFactory.getLogger(Tools::class.java)!!

    fun <T> createSimpleMessageListenerContainer(
        connectionFactory: ConnectionFactory,
        queue: Queue,
        rabbitAdmin: RabbitAdmin,
        buildListener: Listener<T>,
        messageConverter: Jackson2JsonMessageConverter,
        startConsumerMinInterval: Long,
        consecutiveActiveTrigger: Int,
        concurrency: Int,
        maxConcurrency: Int,
        prefetchCount: Int = 1
    ): SimpleMessageListenerContainer {
        logger.info(
            "createMQListener|queue=${queue.name}|listener=${buildListener::class.java.name}|concurrency=$concurrency" +
                "|max=$maxConcurrency|trigger=$consecutiveActiveTrigger|interval=$startConsumerMinInterval"
        )
        val adapter = MessageListenerAdapter(buildListener, buildListener::execute.name)
        adapter.setMessageConverter(messageConverter)
        return createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = queue,
            rabbitAdmin = rabbitAdmin,
            adapter = adapter,
            startConsumerMinInterval = startConsumerMinInterval,
            consecutiveActiveTrigger = consecutiveActiveTrigger,
            concurrency = concurrency,
            maxConcurrency = maxConcurrency,
            prefetchCount = prefetchCount
        )
    }

    fun createSimpleMessageListenerContainerByAdapter(
        connectionFactory: ConnectionFactory,
        queue: Queue,
        rabbitAdmin: RabbitAdmin,
        adapter: MessageListenerAdapter,
        startConsumerMinInterval: Long,
        consecutiveActiveTrigger: Int,
        concurrency: Int,
        maxConcurrency: Int,
        prefetchCount: Int = 1
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(queue.name)
        container.setConcurrentConsumers(concurrency)
        container.setMaxConcurrentConsumers(max(maxConcurrency, concurrency))
        container.setAmqpAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(startConsumerMinInterval)
        container.setConsecutiveActiveTrigger(consecutiveActiveTrigger)
        container.setMismatchedQueuesFatal(true)
        container.setMessageListener(adapter)
        container.setPrefetchCount(prefetchCount)
        container.addAfterReceivePostProcessors(traceMessagePostProcessor)
        return container
    }

    private fun mdcFromMessage(message: Message?) {
        (message ?: return).messageProperties.getHeader<String?>(TraceTag.X_DEVOPS_RID)?.let {
            MDC.put(TraceTag.BIZID, it)
        }
    }

    private val traceMessagePostProcessor = MessagePostProcessor { message ->
        mdcFromMessage(message)
        message
    }
}
