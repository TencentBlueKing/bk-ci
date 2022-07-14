package com.tencent.devops.common.web.mq

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(RabbitAutoConfiguration::class)
@EnableRabbit
class CoreRabbitMQConfiguration {

    @Bean(name = [CORE_CONNECTION_FACTORY_NAME])
    @Primary
    fun coreConnectionFactory(
        @Value("\${spring.rabbitmq.core.username:#{null}}") userName: String,
        @Value("\${spring.rabbitmq.core.password:#{null}}") passWord: String,
        @Value("\${spring.rabbitmq.core.virtual-host:#{null}}") vHost: String,
        @Value("\${spring.rabbitmq.core.addresses:#{null}}") address: String
    ) : ConnectionFactory {
        val connectionFactory = CachingConnectionFactory()
        connectionFactory.username = userName
        connectionFactory.setPassword(passWord)
        connectionFactory.virtualHost = vHost
        connectionFactory.setAddresses(address)
        return connectionFactory
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean(name = [CORE_RABBIT_TEMPLATE_NAME])
    @Primary
    fun coreRabbitTemplate(
        @Qualifier(CORE_CONNECTION_FACTORY_NAME) connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper,
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }
}
