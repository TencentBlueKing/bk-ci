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

package com.tencent.devops.common.stream.pulsar.integration.inbound

import com.tencent.devops.common.stream.pulsar.constant.Serialization
import com.tencent.devops.common.stream.pulsar.properties.PulsarConsumerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProperties
import com.tencent.devops.common.stream.pulsar.util.PulsarClientUtils
import com.tencent.devops.common.stream.pulsar.util.PulsarSchemaUtils
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.RegexSubscriptionMode
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionMode
import org.apache.pulsar.client.api.SubscriptionType
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties
import java.util.concurrent.TimeUnit

object PulsarConsumerFactory {

    /**
     * init for the consumer,including convert consumer params.
     * @param topic topic
     * @param consumerProperties consumerProperties
     * @return DefaultMQConsumer
     */
    @Suppress("LongParameterList", "LongMethod", "MagicNumber")
    fun initPulsarConsumer(
        topic: String,
        group: String? = null,
        consumerProperties: ExtendedConsumerProperties<PulsarConsumerProperties>,
        messageListener: (Consumer<*>, Message<*>) -> Unit,
        pulsarProperties: PulsarProperties,
        concurrency: Int? = null,
        pulsarClient: PulsarClient? = null
    ): Consumer<Any> {
        with(consumerProperties.extension) {
            val topics = mutableListOf<String>()
            topics.addAll(topicNames)
            topics.add(topic)
            val client = pulsarClient ?: PulsarClientUtils.pulsarClient(pulsarProperties, concurrency)
            val serialType = runCatching {
                Serialization.valueOf(serialType)
            }.getOrNull() ?: Serialization.BYTE
            val consumer = client.newConsumer(
                PulsarSchemaUtils.getSchema(serialType, serialClass)
            ).topics(topics)
            if (!topicsPattern.isNullOrEmpty()) {
                consumer.topicsPattern(topicsPattern)
            }
            if (group.isNullOrEmpty() && subscriptionName.isNotBlank()) {
                consumer.subscriptionName(subscriptionName)
            } else {
                consumer.subscriptionName(group)
            }
            val subscriptionType = runCatching {
                SubscriptionType.valueOf(subscriptionType)
            }.getOrNull() ?: SubscriptionType.Shared
            val subscriptionMode = runCatching {
                SubscriptionMode.valueOf(subscriptionMode)
            }.getOrNull() ?: SubscriptionMode.Durable
            consumer.subscriptionType(subscriptionType)
                .subscriptionMode(subscriptionMode)
                .receiverQueueSize(receiverQueueSize)
                .acknowledgmentGroupTime(acknowledgementsGroupTimeMicros, TimeUnit.MILLISECONDS)
                .negativeAckRedeliveryDelay(negativeAckRedeliveryDelayMicros, TimeUnit.MILLISECONDS)
                .maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions)
            if (!consumerName.isNullOrBlank()) {
                consumer.consumerName(consumerName)
            }
            val cryptoFailureAction = runCatching {
                ConsumerCryptoFailureAction.valueOf(cryptoFailureAction)
            }.getOrNull() ?: ConsumerCryptoFailureAction.FAIL
            consumer.ackTimeout(ackTimeoutMillis, TimeUnit.MILLISECONDS)
                .ackTimeoutTickTime(tickDurationMillis, TimeUnit.MILLISECONDS)
                .priorityLevel(priorityLevel)
                .cryptoFailureAction(cryptoFailureAction)
            if (properties.isNotEmpty()) {
                consumer.properties(properties)
            }
            val subscriptionInitialPosition = runCatching {
                SubscriptionInitialPosition.valueOf(subscriptionInitialPosition)
            }.getOrNull() ?: SubscriptionInitialPosition.Latest
            val regexSubscriptionMode = runCatching {
                RegexSubscriptionMode.valueOf(regexSubscriptionMode)
            }.getOrNull() ?: RegexSubscriptionMode.PersistentOnly
            consumer.readCompacted(readCompacted)
                .subscriptionInitialPosition(subscriptionInitialPosition)
                .patternAutoDiscoveryPeriod(patternAutoDiscoveryPeriod)
                .subscriptionTopicsMode(regexSubscriptionMode)
                .autoUpdatePartitions(autoUpdatePartitions)
                .replicateSubscriptionState(replicateSubscriptionState)
            consumer.messageListener(messageListener)
            return consumer.subscribe() as Consumer<Any>
        }
    }
}
