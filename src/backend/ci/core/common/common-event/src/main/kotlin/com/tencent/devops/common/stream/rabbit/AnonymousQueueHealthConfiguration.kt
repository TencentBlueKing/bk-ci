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

package com.tencent.devops.common.stream.rabbit

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ 匿名队列健康检查配置类
 *
 * 配置条件：
 * 1. 类路径中存在 SimpleMessageListenerContainer 类
 * 2. 配置属性 spring.rabbitmq.listener.simple.missing-queues-fatal 未设置或为 true（默认值）
 *
 * 注意：此配置会自动注册健康检查端点 /actuator/health/anonymousQueueHealth
 */
@Configuration
@ConditionalOnClass(name = ["org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer"])
@ConditionalOnProperty(
    prefix = "bkci.rabbitmq.anonymous-queue-health",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AnonymousQueueHealthConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(AnonymousQueueHealthConfiguration::class.java)
    }

    /**
     * 注册匿名队列健康检查指标
     * 这个 Bean 会被 Spring Boot Actuator 自动发现并注册到健康检查端点
     */
    @Bean("anonymousQueueHealthIndicator")
    fun anonymousQueueHealthIndicator(): AnonymousQueueHealthIndicator {
        logger.info("[AnonymousQueueHealthConfiguration] Registering AnonymousQueueHealthIndicator bean")
        return AnonymousQueueHealthIndicator.getInstance()
    }
}
