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

package com.tencent.devops.common.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableKafka
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
    fun stringProducerConfigs(@Autowired kafkaProperties: KafkaProperties): Map<String, Any> {
        val props = HashMap<String, Any>(kafkaProperties.buildProducerProperties())
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        return props
    }

    @Bean
    fun stringProducerFactory(@Autowired kafkaProperties: KafkaProperties): ProducerFactory<String, Any> {
        return DefaultKafkaProducerFactory(stringProducerConfigs(kafkaProperties))
    }

    @Bean
    fun stringKafkaTemplate(@Autowired kafkaProperties: KafkaProperties): KafkaTemplate<String, Any> {
        return KafkaTemplate(stringProducerFactory(kafkaProperties))
    }

    @Bean
    fun consumerFactory(@Autowired kafkaProperties: KafkaProperties): ConsumerFactory<String, Any> {
        val props = HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java

        val jsonDeserializer = JsonDeserializer(Any::class.java)
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), jsonDeserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        @Autowired kafkaProperties: KafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory(kafkaProperties)

        return factory
    }

    @Bean
    fun stringConsumerFactory(@Autowired kafkaProperties: KafkaProperties): ConsumerFactory<String, String> {
        val props = HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

        return DefaultKafkaConsumerFactory(
            props, StringDeserializer(), StringDeserializer()
        )
    }

    @Bean
    fun stringKafkaListenerContainerFactory(
        @Autowired kafkaProperties: KafkaProperties
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = stringConsumerFactory(kafkaProperties)

        return factory
    }
}
