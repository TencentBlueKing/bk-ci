/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.integration.outbound

import com.tencent.devops.common.stream.pulsar.constant.X_DELAY
import com.tencent.devops.common.stream.pulsar.properties.PulsarProducerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProperties
import com.tencent.devops.common.stream.pulsar.support.PulsarMessageConverterSupport
import com.tencent.devops.common.stream.pulsar.util.PulsarTopicUtils
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.springframework.cloud.stream.provisioning.ProducerDestination
import org.springframework.context.Lifecycle
import org.springframework.integration.handler.AbstractMessageHandler
import org.springframework.messaging.Message
import org.springframework.messaging.MessagingException
import org.springframework.messaging.support.MessageBuilder
import java.util.concurrent.TimeUnit

class PulsarProducerMessageHandler(
    private val destination: ProducerDestination,
    private val producerProperties: PulsarProducerProperties? = null,
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
        topic = PulsarTopicUtils.generateTopic(
            tenant = pulsarProperties.tenant,
            namespace = pulsarProperties.namespace,
            topic = destination.name
        )
        producer = PulsarProducerFactory.initPulsarProducer(
            topic = topic,
            producerProperties = producerProperties,
            pulsarProperties = pulsarProperties
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
        sendMessage(message)
    }

    private fun sendMessage(message: Message<*>) {
        val (properties, payload) = PulsarMessageConverterSupport.convertMessage2Pulsar(destination.name, message)
        val key = properties[PulsarMessageConverterSupport.toPulsarHeaderKey(TypedMessageBuilder.CONF_KEY)]
        val deliveryAfterMillis = properties[X_DELAY]
        val deliveryAt = properties[
            PulsarMessageConverterSupport.toPulsarHeaderKey(TypedMessageBuilder.CONF_DELIVERY_AT)
        ]
        try {
            val msg = producer!!.newMessage()
                .value(payload).properties(properties)
            key?.let { msg.key(key) }
            deliveryAfterMillis?.let { msg.deliverAfter(deliveryAfterMillis.toLong(), TimeUnit.MILLISECONDS) }
            deliveryAt?.let { msg.deliverAt(deliveryAt.toLong()) }
            msg.sendAsync()
        } catch (e: Exception) {
            throw MessagingException(
                MessageBuilder.withPayload(
                    "Error occurred whiling producing message " + e.message
                ).build(),
                e
            )
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
}
