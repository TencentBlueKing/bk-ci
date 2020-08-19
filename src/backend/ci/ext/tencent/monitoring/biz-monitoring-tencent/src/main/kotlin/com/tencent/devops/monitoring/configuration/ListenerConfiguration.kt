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

package com.tencent.devops.monitoring.configuration

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.web.mq.EXTEND_CONNECTION_FACTORY_NAME
import com.tencent.devops.common.web.mq.EXTEND_RABBIT_ADMIN_NAME
import com.tencent.devops.monitoring.consumer.AtomMonitorReportListener
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.Integer.max

@Configuration
class ListenerConfiguration {

    @Bean
    fun atomMonitorDataReportQueue() = Queue(QUEUE_ATOM_MONITOR_DATA_REPORT)

    /**
     * 插件监控数据上报广播交换机
     */
    @Bean
    fun atomMonitorDataReportFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_ATOM_MONITOR_DATA_REPORT_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun atomMonitorDataReportQueueBind(
        @Autowired atomMonitorDataReportQueue: Queue,
        @Autowired atomMonitorDataReportFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(atomMonitorDataReportQueue)
            .to(atomMonitorDataReportFanoutExchange)
    }

    @Bean
    fun atomMonitorDataReportListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired atomMonitorDataReportQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: AtomMonitorReportListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(listener, listener::execute.name)
        adapter.setMessageConverter(messageConverter)
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(atomMonitorDataReportQueue.name)
        container.setConcurrentConsumers(1)
        container.setMaxConcurrentConsumers(max(10, 1))
        container.setRabbitAdmin(rabbitAdmin)
        container.setStartConsumerMinInterval(5000)
        container.setConsecutiveActiveTrigger(10)
        container.setMismatchedQueuesFatal(true)
        container.messageListener = adapter
        return container
    }

    companion object {
        private const val QUEUE_ATOM_MONITOR_DATA_REPORT = "q.monitoring.atom.report"
    }
}