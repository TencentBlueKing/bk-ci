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

package com.tencent.devops.common.stream.pulsar.properties

import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.TimeUnit

data class PulsarConsumerProperties(
    var topicNames: Set<String> = emptySet(),
    var topicsPattern: String? = null,
    /**
     * Subscription name
     */
    var subscriptionName: String = "subscription",
    /**
     * 	Subscription type
     * 	Four subscription types are available:
     * 	Exclusive
     * 	Failover
     * 	Shared
     * 	Key_Shared
     */
    var subscriptionType: String = "Shared",

    var subscriptionMode: String = "Durable",

    /**
     * Size of a consumer's receiver queue.
     * For example, the number of messages accumulated by a consumer before an application calls Receive.
     * A value higher than the default value increases consumer throughput,
     * though at the expense of more memory utilization.
     */
    var receiverQueueSize: Int = 1000,
    /**
     * Group a consumer acknowledgment for a specified time.
     * By default, a consumer uses 100ms grouping time to send out acknowledgments to a broker.
     * Setting a group time of 0 sends out acknowledgments immediately.
     * A longer ack group time is more efficient at the expense of a
     * slight increase in message re-deliveries after a failure.
     */
    var acknowledgementsGroupTimeMicros: Long = TimeUnit.MILLISECONDS.toMicros(100),
    /**
     * Delay to wait before redelivering messages that failed to be processed.
     * When an application uses {@link Consumer#negativeAcknowledge(Message)},
     * failed messages are redelivered after a fixed timeout.
     */
    var negativeAckRedeliveryDelayMicros: Long = TimeUnit.MINUTES.toMicros(1),
    /**
     * The max total receiver queue size across partitions.
     * This setting reduces the receiver queue size for individual partitions
     * if the total receiver queue size exceeds this value.
     */
    var maxTotalReceiverQueueSizeAcrossPartitions: Int = 50000,
    /**
     * Consumer name
     */
    var consumerName: String? = null,
    /**
     * Timeout of unacked messages
     */
    var ackTimeoutMillis: Long = 0,
    /**
     * Granularity of the ack-timeout redelivery.
     * Using an higher tickDurationMillis reduces the memory overhead
     * to track messages when setting ack-timeout to a bigger value (for example, 1 hour).
     */
    var tickDurationMillis: Long = 1000,
    /**
     * Priority level for a consumer to which a broker gives more priority
     * while dispatching messages in the shared subscription mode.
     *
     * The broker follows descending priorities. For example, 0=max-priority, 1, 2,...
     *
     * In shared subscription mode, the broker first dispatches messages to the max priority level consumers
     * if they have permits. Otherwise, the broker considers next priority level consumers.
     *
     * Example 1
     * If a subscription has consumerA with priorityLevel 0 and consumerB with priorityLevel 1,
     * then the broker only dispatches messages to consumerA until
     * it runs out permits and then starts dispatching messages to consumerB.
     *
     * Example 2
     * Consumer Priority, Level, Permits
     * C1, 0, 2
     * C2, 0, 1
     * C3, 0, 1
     * C4, 1, 2
     * C5, 1, 1
     *
     * Order in which a broker dispatches messages to consumers is: C1, C2, C3, C1, C4, C5, C4.
     */
    var priorityLevel: Int = 0,
    /**
     * Consumer should take action when it receives a message that can not be decrypted.
     * FAIL: this is the default option to fail messages until crypto succeeds.
     * DISCARD:silently acknowledge and not deliver message to an application.
     * CONSUME: deliver encrypted messages to applications.
     * It is the application's responsibility to decrypt the message.
     *
     * The decompression of message fails.
     *
     * If messages contain batch messages, a client is not be able to retrieve individual messages in batch.
     *
     * Delivered encrypted message contains {@link EncryptionContext}
     * which contains encryption and compression information in it using
     * which application can decrypt consumed message payload.
     */
    var cryptoFailureAction: String = "FAIL",
    /**
     * A name or value property of this consumer.
     * properties is application defined metadata attached to a consumer.
     * When getting a topic stats, associate this metadata with the consumer stats for easier identification.
     */
    var properties: SortedMap<String, String> = TreeMap<String, String>(),
    /**
     * If enabling readCompacted, a consumer reads messages from a compacted topic
     * rather than reading a full message backlog of a topic.
     *
     * A consumer only sees the latest value for each key in the compacted topic,
     * up until reaching the point in the topic message when compacting backlog.
     * Beyond that point, send messages as normal.
     *
     * Only enabling readCompacted on subscriptions to persistent topics,
     * which have a single active consumer (like failure or exclusive subscriptions).
     *
     * Attempting to enable it on subscriptions to non-persistent topics
     * or on shared subscriptions leads to a subscription call throwing a PulsarClientException.
     */
    var readCompacted: Boolean = false,
    /**
     * Initial position at which to set cursor when subscribing to a topic at first time.
     */
    var subscriptionInitialPosition: String = "Latest",
    /**
     * Topic auto discovery period when using a pattern for topic's consumer.
     * The default and minimum value is 1 minute.
     */
    var patternAutoDiscoveryPeriod: Int = 1,
    /**
     * When subscribing to a topic using a regular expression, you can pick a certain type of topics.
     * PersistentOnly: only subscribe to persistent topics.
     * NonPersistentOnly: only subscribe to non-persistent topics.
     * AllTopics: subscribe to both persistent and non-persistent topics.
     */
    var regexSubscriptionMode: String = "PersistentOnly",
    /**
     * Dead letter policy for consumers.
     * By default, some messages are probably redelivered many times, even to the extent that it never stops.
     * By using the dead letter mechanism, messages have the max redelivery count.
     * When exceeding the maximum number of redeliveries,
     * messages are sent to the Dead Letter Topic and acknowledged automatically.
     * You can enable the dead letter mechanism by setting deadLetterPolicy.
     *
     * Example
     * client.newConsumer()
     * .deadLetterPolicy(DeadLetterPolicy.builder().maxRedeliverCount(10).build())
     * .subscribe();
     *
     * Default dead letter topic name is {TopicName}-{Subscription}-DLQ.
     *
     * To set a custom dead letter topic name:
     * client.newConsumer()
     * .deadLetterPolicy(DeadLetterPolicy.builder().maxRedeliverCount(10)
     * .deadLetterTopic("your-topic-name").build())
     * .subscribe();
     *
     * When specifying the dead letter policy while not specifying ackTimeoutMillis,
     * you can set the ack timeout to 30000 millisecond.
     */
    var deadLetterMaxRedeliverCount: Int = 3,
    var retryLetterTopic: String? = null,
    var deadLetterTopic: String? = null,
    /**
     * 	If autoUpdatePartitions is enabled, a consumer subscribes to partition increasement automatically.
     * 	Note: this is only for partitioned consumers.
     */
    var autoUpdatePartitions: Boolean = true,
    /**
     * If replicateSubscriptionState is enabled, a subscription state is replicated to geo-replicated clusters.
     */
    var replicateSubscriptionState: Boolean = false
) : PulsarCommonProperties()
