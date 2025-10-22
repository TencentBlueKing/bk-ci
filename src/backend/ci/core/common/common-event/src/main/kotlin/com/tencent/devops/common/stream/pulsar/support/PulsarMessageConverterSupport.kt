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

package com.tencent.devops.common.stream.pulsar.support

import com.tencent.devops.common.stream.pulsar.constant.MESSAGE_ID
import com.tencent.devops.common.stream.pulsar.constant.PRODUCER_NAME
import com.tencent.devops.common.stream.pulsar.constant.PUBLISH_TIME
import com.tencent.devops.common.stream.pulsar.constant.TOPIC_NAME
import com.tencent.devops.common.stream.pulsar.convert.PulsarMessageConverter
import com.tencent.devops.common.stream.pulsar.custom.PulsarBeanContainerCache
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.messaging.support.MessageBuilder
import org.springframework.util.CollectionUtils
import org.springframework.util.MimeTypeUtils
import java.nio.charset.Charset
import java.util.Objects

object PulsarMessageConverterSupport {

    fun <T> convertMessage2Spring(message: org.apache.pulsar.client.api.Message<T>): Message<*> {
        val messageBuilder = MessageBuilder.withPayload(message.data)
        if (!message.key.isNullOrEmpty()) {
            messageBuilder.setHeader(toPulsarHeaderKey(TypedMessageBuilder.CONF_KEY), message.key)
        }
        messageBuilder.setHeader(toPulsarHeaderKey(TypedMessageBuilder.CONF_EVENT_TIME), message.eventTime)
            .setHeader(toPulsarHeaderKey(TypedMessageBuilder.CONF_SEQUENCE_ID), message.sequenceId)
            .setHeader(toPulsarHeaderKey(MESSAGE_ID), message.messageId)
            .setHeader(toPulsarHeaderKey(PUBLISH_TIME), message.publishTime)
            .setHeader(toPulsarHeaderKey(PRODUCER_NAME), message.producerName)
            .setHeader(toPulsarHeaderKey(TOPIC_NAME), message.topicName)
        addUserProperties(message.properties, messageBuilder)
        return messageBuilder.build()
    }

    fun toPulsarHeaderKey(rawKey: String): String {
        return "PULSAR_$rawKey"
    }

    private fun addUserProperties(
        properties: Map<String, String>,
        messageBuilder: MessageBuilder<*>
    ) {
        if (!CollectionUtils.isEmpty(properties)) {
            properties.forEach { (key: String?, value: String?) ->
                if (MessageHeaders.ID != key &&
                    MessageHeaders.TIMESTAMP != key
                ) {
                    messageBuilder.setHeader(key, value)
                }
            }
        }
    }

    fun convertMessage2Pulsar(
        destination: String,
        source: Message<*>
    ): Pair<Map<String, String>, ByteArray?> {
        var message: Message<*> = MESSAGE_CONVERTER.toMessage(
            source.payload,
            source.headers
        )!!
        val builder = MessageBuilder.fromMessage(message)
        builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
        message = builder.build()
        return doConvert(destination, message)
    }

    private fun doConvert(
        topic: String,
        message: Message<*>
    ): Pair<Map<String, String>, ByteArray?> {
        val charset = Charset.defaultCharset()
        val payloadObj = message.payload
        val payloads: ByteArray = try {
            if (payloadObj is String) {
                payloadObj.toByteArray(charset)
            } else if (payloadObj is ByteArray) {
                message.payload as ByteArray
            } else {
                val jsonObj = MESSAGE_CONVERTER.fromMessage(
                    message,
                    payloadObj.javaClass
                ) as String
                jsonObj.toByteArray(charset)
            }
        } catch (e: Exception) {
            throw RuntimeException("convert to pulsar message failed.", e)
        }
        return getAndWrapMessage(topic, message.headers, payloads)
    }

    private fun getAndWrapMessage(
        topic: String?,
        headers: MessageHeaders,
        payloads: ByteArray?
    ): Pair<Map<String, String>, ByteArray?> {
        if (topic.isNullOrEmpty()) {
            throw RuntimeException("Topic can not be empty to empty")
        }
        val properties = mutableMapOf<String, String>()
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            headers.forEach { key: String, value: Any ->
                properties[key] = value.toString()
            }
        }
        return Pair(properties, payloads)
    }

    private val MESSAGE_CONVERTER: CompositeMessageConverter = PulsarBeanContainerCache
        .getBean(
            PulsarMessageConverter.DEFAULT_NAME,
            CompositeMessageConverter::class.java,
            PulsarMessageConverter().getMessageConverter()
        )
}
