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

package com.tencent.devops.common.stream.pulsar.integration.outbound

import com.tencent.devops.common.stream.pulsar.constant.Serialization
import com.tencent.devops.common.stream.pulsar.properties.PulsarProducerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProperties
import com.tencent.devops.common.stream.pulsar.util.PulsarClientUtils
import com.tencent.devops.common.stream.pulsar.util.PulsarSchemaUtils
import org.apache.pulsar.client.api.CompressionType
import org.apache.pulsar.client.api.HashingScheme
import org.apache.pulsar.client.api.MessageRoutingMode
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerCryptoFailureAction
import java.util.concurrent.TimeUnit

object PulsarProducerFactory {

    /**
     * init for the producer,including convert producer params.
     * @param topic topic
     * @param producerProperties producerProperties
     * @return DefaultMQProducer
     */
    @Suppress("UNCHECKED_CAST")
    fun initPulsarProducer(
        topic: String,
        producerProperties: PulsarProducerProperties,
        pulsarProperties: PulsarProperties
    ): Producer<Any> {
        with(producerProperties) {
            // TODO 消息序列化方式需要调整， producer需要缓存
            val serialType = runCatching {
                Serialization.valueOf(serialType)
            }.getOrNull() ?: Serialization.BYTE
            val producer = PulsarClientUtils.pulsarClient(pulsarProperties).newProducer(
                PulsarSchemaUtils.getSchema(serialType, serialClass)
            ).topic(topic)
            if (!producerName.isNullOrBlank()) {
                producer.producerName(producerName)
            }
            val messageRoutingMode = runCatching {
                MessageRoutingMode.valueOf(messageRoutingMode)
            }.getOrNull() ?: MessageRoutingMode.RoundRobinPartition
            val hashingScheme = runCatching {
                HashingScheme.valueOf(hashingScheme)
            }.getOrNull() ?: HashingScheme.JavaStringHash
            val cryptoFailureAction = runCatching {
                ProducerCryptoFailureAction.valueOf(cryptoFailureAction)
            }.getOrNull() ?: ProducerCryptoFailureAction.FAIL
            val compressionType = runCatching {
                CompressionType.valueOf(compressionType)
            }.getOrNull() ?: CompressionType.NONE
            producer.sendTimeout(sendTimeoutMs, TimeUnit.MILLISECONDS)
                .blockIfQueueFull(blockIfQueueFull)
                .maxPendingMessages(maxPendingMessages)
                .maxPendingMessagesAcrossPartitions(maxPendingMessagesAcrossPartitions)
                .messageRoutingMode(messageRoutingMode)
                .hashingScheme(hashingScheme)
                .cryptoFailureAction(cryptoFailureAction)
                .batchingMaxPublishDelay(batchingMaxPublishDelayMicros, TimeUnit.MILLISECONDS)
                .batchingMaxMessages(batchingMaxMessages)
                .enableBatching(batchingEnabled)
                .compressionType(compressionType)
            return producer.create() as Producer<Any>
        }
    }
}
