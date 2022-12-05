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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.common.web.mq.EXTEND_CONNECTION_FACTORY_NAME
import com.tencent.devops.common.web.mq.EXTEND_RABBIT_ADMIN_NAME
import com.tencent.devops.metrics.listener.CodeCheckDailyMessageListener
import com.tencent.devops.metrics.listener.QualityReportDailyMessageListener
import com.tencent.devops.metrics.listener.TurboDailyReportMessageListener
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
class MetricsThirdPartyListenerConfiguration {

    @Bean
    fun receiveCodeCheckDailyMessageQueue() = Queue(QUEUE_METRICS_STATISTIC_CODECC_DAILY)

    @Bean
    fun receiveCodeCheckDailyMessageFanoutExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_METRICS_STATISTIC_CODECC_DAILY, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun receiveCodeCheckDailyMessageQueueBind(
        @Autowired receiveCodeCheckDailyMessageQueue: Queue,
        @Autowired receiveCodeCheckDailyMessageFanoutExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(receiveCodeCheckDailyMessageQueue)
            .to(receiveCodeCheckDailyMessageFanoutExchange)
    }

    @Bean
    fun metricsMessageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun receiveCodeCheckDailyMessageListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired receiveCodeCheckDailyMessageQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: CodeCheckDailyMessageListener,
        @Autowired metricsMessageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = receiveCodeCheckDailyMessageQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = metricsMessageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = 5,
            maxConcurrency = 20
        )
    }

    @Bean
    fun metricsQualityDailyReportQueue() = Queue(QUEUE_QUALITY_DAILY_EVENT)

    @Bean
    fun metricsQualityDailyReportExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_QUALITY_DAILY_FANOUT, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun metricsQualityDailyQueueBind(
        @Autowired metricsQualityDailyReportQueue: Queue,
        @Autowired metricsQualityDailyReportExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(metricsQualityDailyReportQueue).to(metricsQualityDailyReportExchange)
    }

    @Bean
    fun metricsQualityDailyReportListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired metricsQualityDailyReportQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: QualityReportDailyMessageListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = metricsQualityDailyReportQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = messageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = 5,
            maxConcurrency = 20
        )
    }

    @Bean
    fun metricsTurboDailyReportQueue() = Queue(QUEUE_METRICS_STATISTIC_TURBO_DAILY)

    @Bean
    fun metricsTurboDailyReportExchange(): FanoutExchange {
        val fanoutExchange = FanoutExchange(EXCHANGE_METRICS_STATISTIC_TURBO_DAILY, true, false)
        fanoutExchange.isDelayed = true
        return fanoutExchange
    }

    @Bean
    fun metricsTurboDailyReportQueueBind(
        @Autowired metricsTurboDailyReportQueue: Queue,
        @Autowired metricsTurboDailyReportExchange: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(metricsTurboDailyReportQueue).to(metricsTurboDailyReportExchange)
    }

    @Bean
    fun metricsTurboDailyReportListenerContainer(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) @Autowired connectionFactory: ConnectionFactory,
        @Autowired metricsTurboDailyReportQueue: Queue,
        @Qualifier(value = EXTEND_RABBIT_ADMIN_NAME) @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired listener: TurboDailyReportMessageListener,
        @Autowired metricsMessageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        return Tools.createSimpleMessageListenerContainer(
            connectionFactory = connectionFactory,
            queue = metricsTurboDailyReportQueue,
            rabbitAdmin = rabbitAdmin,
            buildListener = listener,
            messageConverter = metricsMessageConverter,
            startConsumerMinInterval = 1000,
            consecutiveActiveTrigger = 5,
            concurrency = 5,
            maxConcurrency = 20
        )
    }

    companion object {
        private const val QUEUE_QUALITY_DAILY_EVENT = "q.metrics.quality.daily.exchange.queue"
        private const val EXCHANGE_QUALITY_DAILY_FANOUT = "e.metrics.quality.daily.exchange.fanout"
        private const val QUEUE_METRICS_STATISTIC_CODECC_DAILY = "q.metrics.statistic.codecc.daily"
        private const val EXCHANGE_METRICS_STATISTIC_CODECC_DAILY = "e.metrics.statistic.codecc.daily"
        private const val QUEUE_METRICS_STATISTIC_TURBO_DAILY = "q.metrics.statistic.turbo.daily"
        private const val EXCHANGE_METRICS_STATISTIC_TURBO_DAILY = "e.metrics.statistic.turbo.daily"
    }
}
