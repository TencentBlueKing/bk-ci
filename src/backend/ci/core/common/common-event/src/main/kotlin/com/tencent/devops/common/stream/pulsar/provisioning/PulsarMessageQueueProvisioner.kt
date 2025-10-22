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

package com.tencent.devops.common.stream.pulsar.provisioning

import com.tencent.devops.common.stream.pulsar.properties.PulsarConsumerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProducerProperties
import com.tencent.devops.common.stream.pulsar.util.PulsarTopicUtils
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties
import org.springframework.cloud.stream.binder.ExtendedProducerProperties
import org.springframework.cloud.stream.provisioning.ConsumerDestination
import org.springframework.cloud.stream.provisioning.ProducerDestination
import org.springframework.cloud.stream.provisioning.ProvisioningProvider
import org.springframework.util.StringUtils

class PulsarMessageQueueProvisioner :
    ProvisioningProvider<
        ExtendedConsumerProperties<PulsarConsumerProperties>,
        ExtendedProducerProperties<PulsarProducerProperties>
        > {

    override fun provisionProducerDestination(
        name: String,
        properties: ExtendedProducerProperties<PulsarProducerProperties>
    ): ProducerDestination {
        PulsarTopicUtils.validateTopicName(name)
        return PulsarProducerDestination(name, properties.partitionCount)
    }

    override fun provisionConsumerDestination(
        name: String,
        group: String?,
        properties: ExtendedConsumerProperties<PulsarConsumerProperties>
    ): ConsumerDestination {
        if (!properties.isMultiplex) {
            doProvisionConsumerDestination(name, properties)
        } else {
            val destinations = StringUtils.commaDelimitedListToStringArray(name)
            for (element in destinations) {
                doProvisionConsumerDestination(element.trim(), properties)
            }
        }
        return PulsarConsumerDestination(name)
    }

    private fun doProvisionConsumerDestination(
        name: String,
        properties: ExtendedConsumerProperties<PulsarConsumerProperties>
    ): ConsumerDestination {
        PulsarTopicUtils.validateTopicName(name)
        require(properties.instanceCount != 0) { "Instance count cannot be zero" }
        return PulsarConsumerDestination(name)
    }

    class PulsarProducerDestination(
        private val producerDestinationName: String,
        private val partitions: Int
    ) : ProducerDestination {

        override fun getName(): String {
            return producerDestinationName
        }

        override fun getNameForPartition(partition: Int): String {
            return producerDestinationName
        }

        override fun toString(): String {
            return (
                "PulsarProducerDestination{" + "producerDestinationName='" +
                    producerDestinationName + '\'' + ", partitions=" + partitions + '}'
                )
        }
    }

    class PulsarConsumerDestination(
        private val consumerDestinationName: String,
        private val partitions: Int = 0,
        private val dlqName: String? = null
    ) : ConsumerDestination {

        override fun getName(): String {
            return consumerDestinationName
        }
        override fun toString(): String {
            return (
                "PulsarConsumerDestination{" + "consumerDestinationName='" +
                    consumerDestinationName + '\'' + ", partitions=" + partitions +
                    ", dlqName='" + dlqName + '\'' + '}'
                )
        }
    }
}
