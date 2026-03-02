/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.service

import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.filter.RequestChannelFilter
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.prometheus.BkTimedAspect
import com.tencent.devops.common.service.prometheus.UndertowThreadMetrics
import com.tencent.devops.common.service.trace.TraceFilter
import com.tencent.devops.common.service.utils.SpringContextUtil
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.config.MeterFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.consul.ConsulAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered
import org.springframework.core.env.Environment

/**
 *
 * Powered By Tencent
 */
@Configuration
@PropertySource("classpath:/common-service.properties")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(ConsulAutoConfiguration::class)
@EnableDiscoveryClient
class ServiceAutoConfiguration {
    @Bean
    fun profile(environment: Environment) = Profile(environment)

    @Bean
    fun springContextUtil() = SpringContextUtil()

    @Bean
    fun gray() = Gray()

    @Bean
    @ConditionalOnMissingBean(CommonConfig::class)
    fun commonConfig() = CommonConfig()

    @Bean
    fun traceFilter() = TraceFilter()

    @Bean
    fun requestChannelFilter() = RequestChannelFilter()

    @Bean
    fun bkTimedAspect(meterRegistry: MeterRegistry) = BkTimedAspect(meterRegistry)

    @Bean
    fun undertowThreadMetrics() = UndertowThreadMetrics()

    /**
     * 通过 MeterRegistryCustomizer 注册 MeterFilter，在 MeterRegistry 初始化阶段（早于任何 Meter 注册）
     * 过滤掉 RabbitMQ 的 delivery_tag，防止 preFilterIdToMeterMap 因高基数 tag 无限膨胀导致内存泄漏。
     */
    @Bean
    fun rabbitMqDeliveryTagMeterRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config().meterFilter(object : MeterFilter {
                override fun map(id: Meter.Id): Meter.Id {
                    if (id.name.startsWith("spring.rabbit.listener")) {
                        val filteredTags = id.tags.filter {
                            it.key != "messaging.rabbitmq.message.delivery_tag"
                        }
                        return Meter.Id(id.name, Tags.of(filteredTags), id.baseUnit, id.description, id.type)
                    }
                    return id
                }
            })
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceAutoConfiguration::class.java)
    }
}
