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

package com.tencent.bkrepo.common.stream.binder.pulsar.integration.inbound

import com.tencent.bkrepo.common.stream.binder.pulsar.constant.Serialization
import com.tencent.bkrepo.common.stream.binder.pulsar.properties.PulsarConsumerProperties
import com.tencent.bkrepo.common.stream.binder.pulsar.util.PulsarUtils
import com.tencent.bkrepo.common.stream.binder.pulsar.util.SchemaUtils
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.RegexSubscriptionMode
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType

object PulsarConsumerFactory {

    // TODO 考虑缓存
    /**
     * init for the consumer,including convert consumer params.
     * @param topic topic
     * @param consumerProperties consumerProperties
     * @return DefaultMQConsumer
     */
    @Suppress("UNCHECKED_CAST")
    fun initPulsarConsumer(
        topic: String,
        group: String? = null,
        consumerProperties: PulsarConsumerProperties,
        pulsarClient: PulsarClient,
        messageListener: (Consumer<*>, Message<*>) -> Unit,
        retryLetterTopic: String,
        deadLetterTopic: String
    ): Consumer<Any> {
        with(consumerProperties) {
            val topics = mutableListOf<String>()
            topics.addAll(topicNames)
            topics.add(topic)
            val consumer = pulsarClient.newConsumer(
                SchemaUtils.getSchema(Serialization.valueOf(serialType), serialClass)
            ).topics(topics)
            if (!topicsPattern.isNullOrEmpty()) {
                consumer.topicsPattern(topicsPattern)
            }
            if (group.isNullOrEmpty()) {
                consumer.subscriptionName(subscriptionName)
            } else {
                consumer.subscriptionName(group)
            }
            consumer.subscriptionType(SubscriptionType.valueOf(subscriptionType))
                .receiverQueueSize(receiverQueueSize)
                .acknowledgmentGroupTime(acknowledgementsGroupTimeMicros, TimeUnit.MILLISECONDS)
                .negativeAckRedeliveryDelay(negativeAckRedeliveryDelayMicros, TimeUnit.MILLISECONDS)
                .maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions)
            if (!consumerName.isNullOrBlank()) {
                consumer.consumerName(consumerName)
            }
            consumer.ackTimeout(ackTimeoutMillis, TimeUnit.MILLISECONDS)
                .ackTimeoutTickTime(tickDurationMillis, TimeUnit.MILLISECONDS)
                .priorityLevel(priorityLevel)
                .cryptoFailureAction(ConsumerCryptoFailureAction.valueOf(cryptoFailureAction))
            if (properties.isNotEmpty()) {
                consumer.properties(properties)
            }
            consumer.readCompacted(readCompacted)
                .subscriptionInitialPosition(SubscriptionInitialPosition.valueOf(subscriptionInitialPosition))
                .patternAutoDiscoveryPeriod(patternAutoDiscoveryPeriod)
                .subscriptionTopicsMode(RegexSubscriptionMode.valueOf(regexSubscriptionMode))
                .deadLetterPolicy(
                    PulsarUtils.buildDeadLetterPolicy(deadLetterMaxRedeliverCount, retryLetterTopic, deadLetterTopic)
                )
                .autoUpdatePartitions(autoUpdatePartitions)
                .replicateSubscriptionState(replicateSubscriptionState)
            consumer.messageListener(messageListener)
            return consumer.subscribe() as Consumer<Any>
        }
    }
}
