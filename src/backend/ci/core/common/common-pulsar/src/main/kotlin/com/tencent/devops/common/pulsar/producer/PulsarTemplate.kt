package com.tencent.devops.common.pulsar.producer

import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class PulsarTemplate<T>(private val producerCollector: ProducerCollector) {

    @Throws(PulsarClientException::class)
    fun send(topic: String, msg: T): MessageId {
        return producerCollector.getProducer(topic).send(msg)
    }

    fun sendAsync(topic: String, message: T): CompletableFuture<MessageId> {
        return producerCollector.getProducer(topic).sendAsync(message)
    }

    fun createMessage(topic: String, message: T): TypedMessageBuilder<T> {
        return producerCollector.getProducer(topic).newMessage().value(message)
    }
}
