package com.tencent.devops.common.websocket.v2

import com.tencent.devops.common.api.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionFactoryBeanConfigurer
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "websocket.v2", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(WsV2Properties::class)
class WsV2RabbitConfiguration {
    @Bean("websocketV2ConnectionFactory", destroyMethod = "destroy")
    fun websocketV2ConnectionFactory(
        environment: Environment,
        resourceLoader: ResourceLoader,
        properties: WsV2Properties
    ): CachingConnectionFactory {
        properties.validate()
        val rabbit = Binder.get(environment).bind(
            "spring.cloud.stream.binders.core-rabbit.environment.spring.rabbitmq", RabbitProperties::class.java
        ).orElseThrow { IllegalStateException("Websocket v2 requires explicit core-rabbit connection settings") }
        val nativeFactory = RabbitConnectionFactoryBean()
        RabbitConnectionFactoryBeanConfigurer(resourceLoader, rabbit).configure(nativeFactory)
        nativeFactory.afterPropertiesSet()
        return CachingConnectionFactory(requireNotNull(nativeFactory.`object`)).apply {
            CachingConnectionFactoryConfigurer(rabbit).configure(this)
            setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED)
            setConnectionNameStrategy { "websocket-v2-${properties.namespace}" }
        }
    }

    @Bean
    fun websocketV2Publisher(
        @Qualifier("websocketV2ConnectionFactory") factory: CachingConnectionFactory,
        properties: WsV2Properties
    ): WsInvalidationPublisher {
        val template = RabbitTemplate(factory)
        val declared = java.util.concurrent.atomic.AtomicBoolean(false)
        template.setConfirmCallback { _, acknowledged, cause ->
            if (!acknowledged) logger.warn("Websocket v2 publish rejected: {}", cause)
        }
        return WsInvalidationPublisher { event ->
            val messageProperties = MessageProperties().apply {
                contentType = MessageProperties.CONTENT_TYPE_JSON
                deliveryMode = MessageDeliveryMode.NON_PERSISTENT
                expiration = properties.queueTtlMs.toString()
            }
            try {
                if (!declared.get()) synchronized(declared) {
                    if (!declared.get()) {
                        template.execute { channel ->
                            channel.exchangeDeclare(properties.exchangeName(), "direct", true)
                            true
                        }
                        declared.set(true)
                    }
                }
                template.send(
                    properties.exchangeName(), WsTopic.routingKey(event.topic),
                    Message(JsonUtil.toJson(event, false).toByteArray(Charsets.UTF_8), messageProperties)
                )
            } catch (exception: Exception) {
                declared.set(false)
                throw exception
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WsV2RabbitConfiguration::class.java)
    }
}
