package com.tencent.devops.common.web.mq

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * 蓝盾度量MQ
 */
@Configuration
@AutoConfigureBefore(RabbitAutoConfiguration::class)
class ExtendRabbitMQConfiguration {

    @Bean(name = [EXTEND_CONNECTION_FACTORY_NAME])
    fun extendConnectionFactory(
        @Value("\${spring.rabbitmq.extend.username:#{null}}") userName: String,
        @Value("\${spring.rabbitmq.extend.password:#{null}}") passWord: String,
        @Value("\${spring.rabbitmq.extend.virtual-host:#{null}}") vHost: String,
        @Value("\${spring.rabbitmq.extend.addresses:#{null}}") address: String
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

    @Bean(name = [EXTEND_RABBIT_TEMPLATE_NAME])
    fun extendRabbitTemplate(
        @Qualifier(EXTEND_CONNECTION_FACTORY_NAME) connectionFactory: ConnectionFactory,
        objectMapper: ObjectMapper
    ) : RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter(objectMapper)
        return rabbitTemplate
    }
}
