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

package com.tencent.devops.common.web.mq

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

/**
 *
 * Powered By Tencent
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(RabbitAutoConfiguration::class)
@EnableRabbit
class RabbitMQAutoConfiguration {

    @Bean
    @Primary
    fun rabbitTemplate(connectionFactory: ConnectionFactory, objectMapper: ObjectMapper): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)
// 默认配置从application.yml中配置
//    @Bean
//    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory, objectMapper: ObjectMapper): SimpleRabbitListenerContainerFactory {
//        val factory = SimpleRabbitListenerContainerFactory()
//        factory.setConnectionFactory(connectionFactory)
//        factory.setConcurrentConsumers(5)
//        factory.setMessageConverter(messageConverter(objectMapper))
//        return factory
//    }
}
