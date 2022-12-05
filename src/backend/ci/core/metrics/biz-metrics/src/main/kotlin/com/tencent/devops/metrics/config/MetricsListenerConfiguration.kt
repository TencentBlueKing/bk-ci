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

package com.tencent.devops.metrics.config

import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.web.mq.EXTEND_CONNECTION_FACTORY_NAME
import com.tencent.devops.common.web.mq.EXTEND_RABBIT_ADMIN_NAME
import com.tencent.devops.metrics.listener.BuildEndMetricsDataReportListener
import com.tencent.devops.metrics.listener.LabelChangeMetricsDataSyncListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsListenerConfiguration {

    @Bean
    fun buildEndMetricsDataReportQueue() = Queue(QUEUE_BUILD_END_METRICS_DATA_REPORT)

    /**
     * 插件监控数据上报广播交换机
     */
    @Bean
    fun buildEndMetricsDataReportFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_BUILD_END_METRICS_DATA_REPORT_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun buildEndMetricsDataReportQueueBind(
        @Autowired buildEndMetricsDataReportQueue: Queue,
        @Autowired buildEndMetricsDataReportFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(buildEndMetricsDataReportQueue)
            .to(buildEndMetricsDataReportFanoutExchange)
    }

    @Bean
    fun buildEndMetricsDataReportListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildEndMetricsDataReportQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: BuildEndMetricsDataReportListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = buildEndMetricsDataReportQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = 5,
            maxConcurrency = 50
        )
    }

    @Bean
    fun pipelineLabelChangeMetricsDataSyncQueue() = Queue(QUEUE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC)

    /**
     * 流水线标签变化数据同步广播交换机
     */
    @Bean
    fun pipelineLabelChangeMetricsDataSyncFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(MQ.EXCHANGE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun pipelineLabelChangeMetricsDataSyncQueueBind(
        @Autowired pipelineLabelChangeMetricsDataSyncQueue: Queue,
        @Autowired pipelineLabelChangeMetricsDataSyncFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(pipelineLabelChangeMetricsDataSyncQueue)
            .to(pipelineLabelChangeMetricsDataSyncFanoutExchange)
    }

    @Bean
    fun pipelineLabelChangeMetricsDataSyncListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired pipelineLabelChangeMetricsDataSyncQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: LabelChangeMetricsDataSyncListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = pipelineLabelChangeMetricsDataSyncQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = 1,
            maxConcurrency = 10
        )
    }

    companion object {
        private const val QUEUE_BUILD_END_METRICS_DATA_REPORT = "q.build.end.metrics.data.report.queue"
        private const val QUEUE_PIPELINE_LABEL_CHANGE_METRICS_DATA_SYNC =
            "q.pipeline.label.change.metrics.data.sync.queue"
    }
}
