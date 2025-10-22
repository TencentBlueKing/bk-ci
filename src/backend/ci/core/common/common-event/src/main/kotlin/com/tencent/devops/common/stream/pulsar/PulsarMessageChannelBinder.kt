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

package com.tencent.devops.common.stream.pulsar

import com.tencent.devops.common.stream.pulsar.integration.inbound.PulsarInboundChannelAdapter
import com.tencent.devops.common.stream.pulsar.integration.outbound.PulsarProducerMessageHandler
import com.tencent.devops.common.stream.pulsar.properties.PulsarBinderConfigurationProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarConsumerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarExtendedBindingProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProducerProperties
import com.tencent.devops.common.stream.pulsar.provisioning.PulsarMessageQueueProvisioner
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties
import org.springframework.cloud.stream.binder.ExtendedProducerProperties
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder
import org.springframework.cloud.stream.provisioning.ConsumerDestination
import org.springframework.cloud.stream.provisioning.ProducerDestination
import org.springframework.integration.core.MessageProducer
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler

class PulsarMessageChannelBinder(
    messageBinderProvisioner: PulsarMessageQueueProvisioner,
    private val extendedBindingProperties: PulsarExtendedBindingProperties,
    private val pulsarProperties: PulsarBinderConfigurationProperties
) : AbstractMessageChannelBinder<
    ExtendedConsumerProperties<PulsarConsumerProperties>,
    ExtendedProducerProperties<PulsarProducerProperties>, PulsarMessageQueueProvisioner
    >(
    arrayOf(),
    messageBinderProvisioner
),
    ExtendedPropertiesBinder<MessageChannel, PulsarConsumerProperties, PulsarProducerProperties> {

    override fun createProducerMessageHandler(
        destination: ProducerDestination?,
        producerProperties: ExtendedProducerProperties<PulsarProducerProperties>?,
        errorChannel: MessageChannel?
    ): MessageHandler {
        throw IllegalStateException(
            "The abstract binder should not call this method"
        )
    }

    override fun createProducerMessageHandler(
        destination: ProducerDestination,
        producerProperties: ExtendedProducerProperties<PulsarProducerProperties>,
        channel: MessageChannel,
        errorChannel: MessageChannel?
    ): MessageHandler {
        val messageHandler = PulsarProducerMessageHandler(
            destination = destination,
            producerProperties = producerProperties.extension,
            pulsarProperties = pulsarProperties.pulsarProperties!!
        )
        messageHandler.setApplicationContext(this.applicationContext)
        if (errorChannel != null) {
            // TODO 需要处理
        }
//        val partitioningInterceptor = (channel as AbstractMessageChannel)
//            .interceptors.stream()
//            .filter { channelInterceptor: ChannelInterceptor? -> channelInterceptor is PartitioningInterceptor }
//            .map { channelInterceptor: ChannelInterceptor? -> channelInterceptor as PartitioningInterceptor? }
//            .findFirst().orElse(null)
        // TODO 分区处理
//        messageHandler.partitioningInterceptor = partitioningInterceptor
        messageHandler.setBeanFactory(applicationContext.beanFactory)
        // TODO 错误消息策略
        // messageHandler.setErrorMessageStrategy(this.errorMessageStrategy)
        return messageHandler
    }

    override fun createConsumerEndpoint(
        destination: ConsumerDestination?,
        group: String?,
        properties: ExtendedConsumerProperties<PulsarConsumerProperties>
    ): MessageProducer {

        val inboundChannelAdapter = PulsarInboundChannelAdapter(
            destination = destination!!.name,
            extendedConsumerProperties = properties,
            pulsarProperties = pulsarProperties.pulsarProperties!!,
            group = group
        )
        val errorInfrastructure = registerErrorInfrastructure(
            destination,
            group, properties
        )
        if (properties.maxAttempts > 1) {
            inboundChannelAdapter.retryTemplate = buildRetryTemplate(properties)
            inboundChannelAdapter.recoveryCallback = errorInfrastructure.recoverer
        } else {
            inboundChannelAdapter.errorChannel = errorInfrastructure.errorChannel
        }
        return inboundChannelAdapter
    }

// TODO Polled Consumer 定时拉取处理

    override fun getExtendedConsumerProperties(channelName: String?): PulsarConsumerProperties {
        return extendedBindingProperties.getExtendedConsumerProperties(channelName)
    }

    override fun getExtendedProducerProperties(channelName: String?): PulsarProducerProperties {
        return extendedBindingProperties.getExtendedProducerProperties(channelName)
    }

    override fun getDefaultsPrefix(): String {
        return this.extendedBindingProperties.defaultsPrefix
    }

    override fun getExtendedPropertiesEntryClass(): Class<out BinderSpecificPropertiesProvider> {
        return extendedBindingProperties.extendedPropertiesEntryClass
    }
}
