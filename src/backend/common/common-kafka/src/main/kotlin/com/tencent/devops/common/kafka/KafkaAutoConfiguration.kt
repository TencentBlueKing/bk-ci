package com.tencent.devops.common.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class KafkaAutoConfiguration {

    @Bean
    fun producerConfigs(@Autowired kafkaProperties: KafkaProperties): Map<String, Any> {
        val props = HashMap<String, Any>(kafkaProperties.buildProducerProperties())
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return props
    }

    @Bean
    fun producerFactory(@Autowired kafkaProperties: KafkaProperties): ProducerFactory<String, Any> {
        return DefaultKafkaProducerFactory(producerConfigs(kafkaProperties))
    }

    @Bean
    fun kafkaTemplate(@Autowired kafkaProperties: KafkaProperties): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory(kafkaProperties))
    }

    @Bean
    fun consumerFactory(@Autowired kafkaProperties: KafkaProperties): ConsumerFactory<String, Any> {
        val jsonDeserializer = JsonDeserializer(Any::class.java)
        return DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(), StringDeserializer(), jsonDeserializer
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(@Autowired kafkaProperties: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory(kafkaProperties)

        return factory
    }
}