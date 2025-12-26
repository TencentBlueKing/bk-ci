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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * RabbitMQ 匿名队列健康检查配置类
 *
 * 配置条件：
 * 1. 类路径中存在 SimpleMessageListenerContainer 类
 * 2. management.endpoint.health.group.readinessState.include 或 livenessState.include 中显式包含 "anonQueue"
 *
 * 使用方式：
 * 在服务配置中添加：
 * management:
 *   endpoint:
 *     health:
 *       group:
 *         readinessState:
 *           include: "readinessState,binders,db,kubernetes,redis,process,anonQueue"
 *
 * 注意：此配置会自动注册健康检查端点 /actuator/health/anonQueue
 */
@Configuration
@ConditionalOnClass(name = ["org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer"])
@Conditional(AnonymousQueueHealthConfiguration.AnonQueueIncludedCondition::class)
class AnonymousQueueHealthConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(AnonymousQueueHealthConfiguration::class.java)

        /**
         * 健康检查指标名称，用于配置和端点访问
         */
        const val HEALTH_INDICATOR_NAME = "anonQueue"
    }

    /**
     * 注册匿名队列健康检查指标
     * 这个 Bean 会被 Spring Boot Actuator 自动发现并注册到健康检查端点
     * Bean 名称 "anonQueue" 会作为健康检查端点名称: /actuator/health/anonQueue
     */
    @Bean(HEALTH_INDICATOR_NAME)
    fun anonQueueHealthIndicator(): AnonymousRabbitHealthIndicator {
        logger.info("[AnonymousQueueHealthConfiguration] Registering AnonymousQueueHealthIndicator bean")
        return AnonymousRabbitHealthIndicator.getInstance()
    }

    /**
     * 自定义条件：检查 management.endpoint.health.group 配置中是否包含 anonQueue
     *
     * 支持以下配置路径：
     * - management.endpoint.health.group.readinessState.include
     * - management.endpoint.health.group.livenessState.include
     */
    class AnonQueueIncludedCondition : Condition {

        companion object {
            private val logger = LoggerFactory.getLogger(AnonQueueIncludedCondition::class.java)

            /**
             * 需要检查的配置属性路径列表
             */
            private val HEALTH_GROUP_INCLUDE_PROPERTIES = listOf(
                "management.endpoint.health.group.readinessState.include",
                "management.endpoint.health.group.livenessState.include"
            )
        }

        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
            val environment = context.environment

            for (propertyKey in HEALTH_GROUP_INCLUDE_PROPERTIES) {
                val includeValue = environment.getProperty(propertyKey)
                if (!includeValue.isNullOrBlank()) {
                    // 解析 include 配置值，支持逗号分隔的多个值
                    val indicators = includeValue.split(",").map { it.trim() }
                    if (indicators.contains(HEALTH_INDICATOR_NAME)) {
                        logger.info(
                            "[AnonQueueIncludedCondition] Found '$HEALTH_INDICATOR_NAME' in $propertyKey, " +
                                "AnonymousQueueHealthIndicator will be loaded"
                        )
                        return true
                    }
                }
            }

            logger.info(
                "[AnonQueueIncludedCondition] '$HEALTH_INDICATOR_NAME' not found in health group include configs, " +
                    "AnonymousQueueHealthIndicator will NOT be loaded. " +
                    "To enable, add '$HEALTH_INDICATOR_NAME' to management.endpoint.health.group.readinessState.include"
            )
            return false
        }
    }
}
