package com.tencent.devops.common.pulsar.producer

import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor
import org.slf4j.LoggerFactory

class DefaultProducerInterceptor : ProducerInterceptor {
    override fun close() {
        logger.debug("DefaultProducerInterceptor closed")
    }

    override fun eligible(message: Message<*>?): Boolean {
        return true
    }

    override fun beforeSend(producer: Producer<*>, message: Message<*>): Message<*> {
        logger.debug(
            "[Pulsar producer log:BeforeSend] ProducerName:[{}], Topic:[{}]",
            producer.producerName,
            producer.topic
        )
        return message
    }

    override fun onSendAcknowledgement(producer: Producer<*>, message: Message<*>, msgId: MessageId, exception: Throwable) {
        logger.error(
            "[Pulsar producer log:OnSendAcknowledgement] Producer:[{}], Topic:[{}], Payload:[{}], msgID:[{}], exception:[{}]",
            producer.producerName, producer.topic, message.value.toString(), msgId.toString(), exception
        )
        return
    }

    override fun onPartitionsChange(topicName: String, partitions: Int) {
        logger.debug("[Pulsar producer log:OnPartitionsChange] Topic:[{}], Partitions:[{}]", topicName, partitions)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultProducerInterceptor::class.java)
    }
}
