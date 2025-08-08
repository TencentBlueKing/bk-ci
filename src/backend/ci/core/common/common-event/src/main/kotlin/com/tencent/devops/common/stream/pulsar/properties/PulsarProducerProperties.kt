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

import java.util.concurrent.TimeUnit

data class PulsarProducerProperties(
    /**
     * Whether the producer is synchronous.
     */
    var sync: Boolean = true,
    var producerName: String? = null,
    /**
     * Message send timeout in ms.
     * If a message is not acknowledged by a server before the sendTimeout expires, an error occurs.
     */
    var sendTimeoutMs: Int = 30000,
    /**
     * 	If it is set to true, when the outgoing message queue is full,
     * 	the Send and SendAsync methods of producer block, rather than failing and throwing errors.
     * 	If it is set to false, when the outgoing message queue is full,
     * 	the Send and SendAsync methods of producer fail and ProducerQueueIsFullError exceptions occur.
     */
    var blockIfQueueFull: Boolean = false,
    /**
     * The maximum size of a queue holding pending messages.
     * For example, a message waiting to receive an acknowledgment from a broker.
     * By default, when the queue is full, all calls to the Send and
     * SendAsync methods fail unless you set BlockIfQueueFull to true.
     */
    var maxPendingMessages: Int = 1000,
    /**
     * The maximum number of pending messages across partitions.
     * Use the setting to lower the max pending messages for
     * each partition ({@link #setMaxPendingMessages(int)}) if the total number exceeds the configured value.
     */
    var maxPendingMessagesAcrossPartitions: Int = 50000,
    /**
     * Message routing logic for producers on partitioned topics.
     * Apply the logic only when setting no key on messages.
     * Available options are as follows:
     * pulsar.RoundRobinDistribution: round robin
     * pulsar.UseSinglePartition: publish all messages to a single partition
     * pulsar.CustomPartition: a custom partitioning scheme
     */
    var messageRoutingMode: String = "RoundRobinPartition",
    /**
     * Hashing function determining the partition where you publish a particular message (partitioned topics only).
     * Available options are as follows:
     * pulsar.JavastringHash: the equivalent of string.hashCode() in Java
     * pulsar.Murmur3_32Hash: applies the Murmur3 hashing function
     * pulsar.BoostHash: applies the hashing function from C++'s Boost library
     */
    var hashingScheme: String = "JavaStringHash",
    /**
     * Producer should take action when encryption fails.
     * FAIL: if encryption fails, unencrypted messages fail to send.
     * SEND: if encryption fails, unencrypted messages are sent.
     */
    var cryptoFailureAction: String = "FAIL",
    /**
     * Batching time period of sending messages.
     */
    var batchingMaxPublishDelayMicros: Long = TimeUnit.MILLISECONDS.toMicros(1),
    /**
     * The maximum number of messages permitted in a batch.
     */
    var batchingMaxMessages: Int = 1000,
    /**
     * Enable batching of messages.
     */
    var batchingEnabled: Boolean = true,
    /**
     * Message data compression type used by a producer.
     * Available options:
     * LZ4
     * ZLIB
     * ZSTD
     * SNAPPY
     */
    var compressionType: String = "NONE"
) : PulsarCommonProperties()
