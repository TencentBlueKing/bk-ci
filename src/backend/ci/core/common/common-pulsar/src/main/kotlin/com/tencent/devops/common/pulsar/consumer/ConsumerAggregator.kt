package com.tencent.devops.common.pulsar.consumer

import com.tencent.devops.common.pulsar.IEvent
import com.tencent.devops.common.pulsar.collector.ConsumerCollector
import com.tencent.devops.common.pulsar.collector.ConsumerHolder
import com.tencent.devops.common.pulsar.properties.ConsumerProperties
import com.tencent.devops.common.pulsar.properties.PulsarProperties
import com.tencent.devops.common.pulsar.utils.SchemaUtils
import com.tencent.devops.common.pulsar.utils.UrlBuildService
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.DependsOn
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import kotlin.streams.toList


@Component
@DependsOn("pulsarClient", "consumerCollector")
class ConsumerAggregator @Autowired constructor(
    private val consumerCollector: ConsumerCollector,
    private val pulsarClient: PulsarClient,
    private val consumerProperties: ConsumerProperties,
    private val pulsarProperties: PulsarProperties,
    private val urlBuildService: UrlBuildService
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ConsumerAggregator::class.java)
    }

    private var consumers: List<Consumer<*>>? = null

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        if (pulsarProperties.isAutoStart) {
            consumers = consumerCollector.getConsumers().entries.stream()
                .filter { holder -> holder.value.annotation.autoStart }
                .map { holder -> subscribe(holder.key, holder.value) }
                .toList()
        }
    }

    private fun subscribe(generatedConsumerName: String, holder: ConsumerHolder): Consumer<*> {
        return try {

            val consumerName = holder.annotation.consumerName
            val subscriptionName = holder.annotation.subscriptionName
            val topicName = holder.annotation.topic
            val namespace = holder.annotation.namespace
            val subscriptionType: SubscriptionType = urlBuildService.getSubscriptionType(holder)

            val consumerBuilder: ConsumerBuilder<*> = pulsarClient
                .newConsumer(
                    SchemaUtils.getSchema(
                        holder.annotation.serialization,
                        holder.annotation.clazz.java
                    )
                )
                .consumerName(urlBuildService.buildPulsarConsumerName(consumerName, generatedConsumerName))
                .subscriptionName(urlBuildService.buildPulsarSubscriptionName(subscriptionName, generatedConsumerName))
                .topic(urlBuildService.buildTopicUrl(topicName, namespace))
                .subscriptionType(subscriptionType)
                .subscriptionInitialPosition(holder.annotation.initialPosition)
                .messageListener { consumer, msg ->
                    try {
                        val method: Method = holder.handler
                        method.isAccessible = true
                        if (holder.isWrapped) {
                            method.invoke(holder.bean, wrapMessage(msg))
                        } else {
                            method.invoke(holder.bean, msg.value)
                        }
                    } catch (e: Exception) {
                        logger.error("Message listener is negative with error.", e)
                        consumer.negativeAcknowledge(msg)
                    }
                }
            if (consumerProperties.ackTimeoutMs > 0) {
                consumerBuilder.ackTimeout(consumerProperties.ackTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
            }
            urlBuildService.buildDeadLetterPolicy(
                holder.annotation.maxRedeliverCount,
                holder.annotation.deadLetterTopic,
                consumerBuilder
            )
            consumerBuilder.subscribe()
        } catch (t: Throwable) {

            throw t
        }
    }

    fun <T> wrapMessage(message: Message<T>): IEvent<T> {
        val IEvent = IEvent<T>()
        IEvent.setValue(message.value)
        IEvent.messageId = message.messageId
        IEvent.sequenceId = message.sequenceId
        IEvent.properties = message.properties
        IEvent.topicName = message.topicName
        IEvent.key = message.key
        IEvent.eventTime = message.eventTime
        IEvent.publishTime = message.publishTime
        IEvent.producerName = message.producerName
        return IEvent
    }
}
