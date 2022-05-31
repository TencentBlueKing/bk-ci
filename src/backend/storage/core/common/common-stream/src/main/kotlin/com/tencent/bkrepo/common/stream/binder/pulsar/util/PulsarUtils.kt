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

package com.tencent.bkrepo.common.stream.binder.pulsar.util

import com.tencent.bkrepo.common.stream.binder.pulsar.constant.DLQ
import com.tencent.bkrepo.common.stream.binder.pulsar.constant.PATH_SPLIT
import com.tencent.bkrepo.common.stream.binder.pulsar.constant.RETRY
import java.io.UnsupportedEncodingException
import java.util.StringJoiner
import javax.validation.constraints.NotBlank
import org.apache.pulsar.client.api.DeadLetterPolicy

object PulsarUtils {

    /**
     * Validate topic name. Allowed chars are ASCII alphanumerics, '.', '_' and '-'.
     * @param topicName name of the topic
     */
    fun validateTopicName(topicName: String) {
        try {
            val utf8 = topicName.toByteArray(charset("UTF-8"))
            for (b: Byte in utf8) {
                if (!validateByte(b)) {
                    throw IllegalArgumentException(
                        "Topic name can only have ASCII alphanumerics, '.', '_' and '-', but was: '$topicName'"
                    )
                }
            }
        } catch (ex: UnsupportedEncodingException) {
            throw AssertionError(ex) // Can't happen
        }
    }

    private fun validateByte(b: Byte): Boolean {
        return (
            b >= 'a'.toByte() && b <= 'z'.toByte() ||
                b >= 'A'.toByte() && b <= 'Z'.toByte() ||
                b >= '0'.toByte() && b <= '9'.toByte() ||
                b == '.'.toByte() || b == '-'.toByte() ||
                b == '_'.toByte()
            )
    }

    /**
     * 拼接topic
     * @return 完整topic路径
     */
    fun generateTopic(
        tenant: @NotBlank String,
        namespace: @NotBlank String,
        topic: @NotBlank String
    ): String {
        val stringJoiner = StringJoiner(PATH_SPLIT)
        stringJoiner.add(tenant).add(namespace).add(topic)
        return stringJoiner.toString()
    }

    /**
     * 拼接deadletter 相关topic
     * @return 完整topic路径
     */
    fun generateDeadLetterTopics(
        tenant: @NotBlank String,
        namespace: @NotBlank String,
        group: String? = null,
        subscriptionName: String,
        deadLetterTopic: String? = null,
        retryLetterTopic: String? = null
    ): Pair<String, String> {
        val dlTopic = buildDeadLetterRelatedTopic(
            tenant = tenant,
            namespace = namespace,
            topic = deadLetterTopic,
            group = group,
            subscriptionName = subscriptionName,
            suffix = DLQ
        )
        val rlTopic = buildDeadLetterRelatedTopic(
            tenant = tenant,
            namespace = namespace,
            topic = retryLetterTopic,
            group = group,
            subscriptionName = subscriptionName,
            suffix = RETRY
        )
        return Pair(dlTopic, rlTopic)
    }

    private fun buildDeadLetterRelatedTopic(
        tenant: @NotBlank String,
        namespace: @NotBlank String,
        topic: String?,
        group: String?,
        subscriptionName: String,
        suffix: String
    ): String {
        val temp = if (!topic.isNullOrEmpty()) {
            topic
        } else {
            if (group.isNullOrEmpty()) {
                subscriptionName + suffix
            } else {
                group + suffix
            }
        }
        return generateTopic(
            tenant = tenant,
            namespace = namespace,
            topic = temp
        )
    }

    /**
     * 生成对应deadLetterpolicy
     */
    fun buildDeadLetterPolicy(
        deadLetterMaxRedeliverCount: Int,
        retryLetterTopic: String? = null,
        deadLetterTopic: String? = null
    ): DeadLetterPolicy? {
        var deadLetterBuilder: DeadLetterPolicy.DeadLetterPolicyBuilder? = null
        if (deadLetterMaxRedeliverCount >= 0) {
            deadLetterBuilder = DeadLetterPolicy.builder().maxRedeliverCount(deadLetterMaxRedeliverCount)
        }
        if (deadLetterBuilder != null && !deadLetterTopic.isNullOrBlank()) {
            deadLetterBuilder = deadLetterBuilder.deadLetterTopic(deadLetterTopic)
        }
        if (deadLetterBuilder != null && !retryLetterTopic.isNullOrBlank()) {
            deadLetterBuilder = deadLetterBuilder.retryLetterTopic(retryLetterTopic)
        }
        return deadLetterBuilder?.build()
    }
}
