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

package com.tencent.devops.common.web.mq

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.web.mq.factory.CustomSimpleRabbitListenerContainerFactory
import com.tencent.devops.common.web.mq.property.ExtendRabbitMQProperties
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration::class)
@EnableConfigurationProperties(ExtendRabbitMQProperties::class)
class ExtendRabbitMQConfiguration {

    @Value("\${spring.rabbitmq.extend.virtual-host}")
    private val virtualHost: String? = null

    @Value("\${spring.rabbitmq.extend.username}")
    private val username: String? = null

    @Value("\${spring.rabbitmq.extend.password}")
    private val password: String? = null

    @Value("\${spring.rabbitmq.extend.addresses}")
    private val addresses: String? = null

    @Value("\${spring.rabbitmq.extend.listener.simple.concurrency:#{null}}")
    private var concurrency: Int? = null

    @Value("\${spring.rabbitmq.extend.listener.simple.max-concurrency:#{null}}")
    private var maxConcurrency: Int? = null

    @Value("\${spring.rabbitmq.extend.cache.channel.size:#{null}}")
    private var channelCacheSize: Int? = null

    @Value("\${spring.rabbitmq.listener.simple.prefetch:#{null}}")
    private val preFetchCount: Int? = null

    @Bean(name = [EXTEND_CONNECTION_FACTORY_NAME])
    fun extendConnectionFactory(config: ExtendRabbitMQProperties): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.host = config.host
        connectionFactory.port = config.port
        connectionFactory.username = username!!
        connectionFactory.setPassword(password!!)
        connectionFactory.virtualHost = getVirtualHost()
        connectionFactory.setAddresses(addresses!!)
        if (channelCacheSize != null && channelCacheSize!! > 0) {
            connectionFactory.channelCacheSize = channelCacheSize!!
        }
        return connectionFactory
    }

    @Bean(name = [EXTEND_RABBIT_TEMPLATE_NAME])
    fun extendRabbitTemplate(
        @Qualifier(value = EXTEND_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        rabbitTemplate.addBeforePublishPostProcessors(CoreRabbitMQConfiguration.setTraceIdToMessageProcess)
        return rabbitTemplate
    }

    @Bean(value = [EXTEND_RABBIT_ADMIN_NAME])
    fun extendRabbitAdmin(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory
    ): RabbitAdmin? {
        return RabbitAdmin(connectionFactory)
    }

    @Bean(value = [EXTEND_LISTENER_CONTAINER_NAME])
    fun extendFactory(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME)
        connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ): SimpleRabbitListenerContainerFactory {
        val factory = CustomSimpleRabbitListenerContainerFactory(CoreRabbitMQConfiguration.traceMessagePostProcessor)
        factory.setMessageConverter(messageConverter(objectMapper))
        factory.setConnectionFactory(connectionFactory)
        if (concurrency != null) {
            factory.setConcurrentConsumers(concurrency)
        }
        if (maxConcurrency != null) {
            factory.setMaxConcurrentConsumers(maxConcurrency)
        }
        if (preFetchCount != null) {
            factory.setPrefetchCount(preFetchCount)
        }
        return factory
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) =
        Jackson2JsonMessageConverter(objectMapper)

    fun getVirtualHost(): String {
        return virtualHost + if (KubernetesUtils.isMultiCluster()) "-k8s" else ""
    }
}
