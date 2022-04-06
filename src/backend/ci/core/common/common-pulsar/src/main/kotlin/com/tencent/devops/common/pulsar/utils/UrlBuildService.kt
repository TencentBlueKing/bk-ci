package com.tencent.devops.common.pulsar.utils

import com.tencent.devops.common.pulsar.collector.ConsumerHolder
import com.tencent.devops.common.pulsar.properties.ConsumerProperties
import com.tencent.devops.common.pulsar.properties.PulsarProperties
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.DeadLetterPolicy
import org.apache.pulsar.client.api.DeadLetterPolicy.DeadLetterPolicyBuilder
import org.apache.pulsar.client.api.SubscriptionType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.Arrays
import java.util.stream.Collectors


@Service
class UrlBuildService private constructor(
    private val pulsarProperties: PulsarProperties,
    private val consumerProperties: ConsumerProperties
) {

    @Value("\${pulsar.consumerNameDelimiter:}")
    private val consumerNameDelimiter: String? = null

    fun buildTopicUrl(topic: String, namespace: String?): String {
        return if (namespace.isNullOrBlank()) {
            DEFAULT_PERSISTENCE + "://" + pulsarProperties.tenant + "/" + namespace + "/" + topic
        } else {
            DEFAULT_PERSISTENCE + "://" + pulsarProperties.tenant + "/" + pulsarProperties.namespace +
                "/" + topic
        }
    }

    fun buildPulsarConsumerName(customConsumerName: String?, generatedConsumerName: String): String? {
        return if (customConsumerName.isNullOrBlank()) {
            CONSUMER_NAME_PREFIX + consumerNameDelimiter + generatedConsumerName
        } else customConsumerName
    }

    fun buildPulsarSubscriptionName(customSubscriptionName: String?, consumerName: String): String? {
        return if (customSubscriptionName.isNullOrBlank()) {
            SUBSCRIPTION_NAME_PREFIX + consumerNameDelimiter + consumerName
        } else customSubscriptionName
    }

    fun getSubscriptionType(holder: ConsumerHolder): SubscriptionType {
        return getSubscriptionType(holder.annotation.subscriptionType.first())

    }

    fun getSubscriptionType(type: SubscriptionType): SubscriptionType {
        var result = type
        if (consumerProperties.subscriptionType.isBlank()) {
            result = DEFAULT_SUBSCRIPTION_TYPE
        } else if (consumerProperties.subscriptionType.isNotBlank()) {
            result = SubscriptionType.valueOf(consumerProperties.subscriptionType)
        }
        return result
    }

    fun buildDeadLetterPolicy(maxRedeliverCount: Int, deadLetterTopic: String, consumerBuilder: ConsumerBuilder<*>) {
        var deadLetterBuilder: DeadLetterPolicyBuilder? = null
        if (consumerProperties.deadLetterPolicyMaxRedeliverCount >= 0) {
            deadLetterBuilder = DeadLetterPolicy.builder()
                .maxRedeliverCount(consumerProperties.deadLetterPolicyMaxRedeliverCount)
        }
        if (maxRedeliverCount >= 0) {
            deadLetterBuilder = DeadLetterPolicy.builder().maxRedeliverCount(maxRedeliverCount)
        }
        if (deadLetterBuilder != null && deadLetterTopic.isNotEmpty()) {
            deadLetterBuilder.deadLetterTopic(buildTopicUrl(deadLetterTopic, null))
        }
        if (deadLetterBuilder != null) {
            consumerBuilder.deadLetterPolicy(deadLetterBuilder.build())
        }
    }

    fun buildConsumerName(clazz: Class<*>, method: Method): String {
        return clazz.name + consumerNameDelimiter + method.name + Arrays
            .stream(method.genericParameterTypes)
            .map { obj: Type -> obj.typeName }
            .collect(Collectors.joining(consumerNameDelimiter))
    }

    companion object {
        private const val PERSISTENT_PREFIX = "persistent"
        private const val NON_PERSISTENT_PREFIX = "non-persistent"
        private const val DEFAULT_PERSISTENCE = PERSISTENT_PREFIX
        private const val CONSUMER_NAME_PREFIX = "consumer"
        private const val SUBSCRIPTION_NAME_PREFIX = "subscription"
        private val DEFAULT_SUBSCRIPTION_TYPE = SubscriptionType.Exclusive
    }
}
