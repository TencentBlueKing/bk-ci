package com.tencent.devops.turbo.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * 蓝盾度量MQ
 */
@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration::class)
@ConditionalOnProperty(prefix = "rabbitmq.bkMetrics", name = ["addresses", "virtual-host", "username", "password"])
class BkMetricsMQAutoConfig {

    @Bean(name = ["bkMetricsConnectionFactory"])
    fun bkMetricsConnectionFactory(
        @Value("\${rabbitmq.bkMetrics.username:#{null}}") userName: String,
        @Value("\${rabbitmq.bkMetrics.password:#{null}}") passWord: String,
        @Value("\${rabbitmq.bkMetrics.virtual-host:#{null}}") vHost: String,
        @Value("\${rabbitmq.bkMetrics.addresses:#{null}}") address: String,
    ): ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.username = userName
        connectionFactory.setPassword(passWord)
        connectionFactory.virtualHost = vHost
        connectionFactory.setAddresses(address)

        return connectionFactory
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean(name= ["bkMetricsRabbitTemplate"])
    fun bkMetricsRabbitTemplate(
        @Qualifier("bkMetricsConnectionFactory") connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ) : RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }
}
