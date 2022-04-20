/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.integration

import com.tencent.devops.common.stream.pulsar.constant.Serialization
import com.tencent.devops.common.stream.pulsar.properties.PulsarProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProducerProperties
import com.tencent.devops.common.stream.pulsar.support.PulsarMessageConverterSupport
import com.tencent.devops.common.stream.pulsar.util.PulsarUtils
import com.tencent.devops.common.stream.pulsar.util.SchemaUtils
import org.apache.pulsar.client.api.CompressionType
import org.apache.pulsar.client.api.HashingScheme
import org.apache.pulsar.client.api.MessageRoutingMode
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerCryptoFailureAction
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.springframework.cloud.stream.binder.ExtendedProducerProperties
import org.springframework.cloud.stream.provisioning.ProducerDestination
import org.springframework.context.Lifecycle
import org.springframework.integration.handler.AbstractMessageHandler
import org.springframework.messaging.Message
import java.util.concurrent.TimeUnit

class PulsarProducerMessageHandler(
    private val destination: ProducerDestination,
    private val producerProperties: ExtendedProducerProperties<PulsarProducerProperties>? = null,
    private val pulsarProperties: PulsarProperties
) : AbstractMessageHandler(), Lifecycle {
    private var producer: Producer<Any>? = null

    @Volatile
    private var running = false
    private var topic: String = ""

    override fun onInit() {
        if (null == producerProperties) {
            return
        }
        super.onInit()
        topic = PulsarUtils.generateTopic(
            tenant = pulsarProperties.tenant,
            namespace = pulsarProperties.namespace,
            topic = destination.name
        )
        producer = generatePulsarProducer(
            topic = topic,
            pulsarProperties = pulsarProperties,
            producerProperties = producerProperties
        )
    }

    override fun start() {
//        val instrumentation = Instrumentation(
//            topic = topic,
//            actuator = this
//        )
        running = true
//        instrumentation.markStartedSuccessfully()
//        InstrumentationManager.addHealthInstrumentation(instrumentation)
    }

    override fun handleMessageInternal(message: Message<*>) {
        // TODO 需要格式转换、 异步同步处理等、消息失败处理、消息确认等
        if (logger.isDebugEnabled) {
            logger.debug("Message's header is ${message.headers} and payload is ${message.payload}")
        }
        val (properties, payload) = PulsarMessageConverterSupport.convertMessage2Pulsar(destination.name, message)
        val key = properties[PulsarMessageConverterSupport.toPulsarHeaderKey(TypedMessageBuilder.CONF_KEY)]
        if (key.isNullOrEmpty()) {
            producer!!.newMessage()
                .value(payload).properties(properties).sendAsync()
        } else {
            producer!!.newMessage().key(key)
                .value(payload).properties(properties).sendAsync()
        }
    }

    override fun stop() {
        if (running && null != producer) {
            producer!!.close()
        }
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    private fun generatePulsarProducer(
        topic: String,
        pulsarProperties: PulsarProperties,
        producerProperties: ExtendedProducerProperties<PulsarProducerProperties>
    ): Producer<Any> {
        val pulsarProducerProperties = producerProperties.extension
        with(pulsarProducerProperties) {
            val builder = PulsarUtils.getClientBuilder(pulsarProperties)

            // TODO 消息序列化方式需要调整， producer需要缓存
            val producer = builder.build().newProducer(
                SchemaUtils.getSchema(Serialization.valueOf(serialType), serialClass)
            ).topic(topic)
            if (!producerName.isNullOrBlank()) {
                producer.producerName(producerName)
            }
            producer.sendTimeout(sendTimeoutMs, TimeUnit.MILLISECONDS)
                .blockIfQueueFull(blockIfQueueFull)
                .maxPendingMessages(maxPendingMessages)
                .maxPendingMessagesAcrossPartitions(maxPendingMessagesAcrossPartitions)
                .messageRoutingMode(MessageRoutingMode.valueOf(messageRoutingMode))
                .hashingScheme(HashingScheme.valueOf(hashingScheme))
                .cryptoFailureAction(ProducerCryptoFailureAction.valueOf(cryptoFailureAction))
                .batchingMaxPublishDelay(batchingMaxPublishDelayMicros, TimeUnit.MILLISECONDS)
                .batchingMaxMessages(batchingMaxMessages)
                .enableBatching(batchingEnabled)
                .compressionType(CompressionType.valueOf(compressionType))
            return producer.create() as Producer<Any>
        }
    }
}
