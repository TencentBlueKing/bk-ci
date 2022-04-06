package com.tencent.devops.common.pulsar.consumer

import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerInterceptor
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.LoggerFactory

class DefaultConsumerInterceptor<T : Any?> : ConsumerInterceptor<T> {
    override fun close() {
        logger.debug("DefaultConsumerInterceptor closed")
    }

    override fun beforeConsume(consumer: Consumer<T>, message: Message<T>): Message<T> {
        logger.debug(
            "[Pulsar consumer log:BeforeConsume] ProducerName[{}], ConsumerName:[{}], Topic:[{}], msgID:[{}]," +
                " MessageKey:[{}], PublishTime:[{}], RedeliveryCount:[{}], GetReplicatedFrom:[{}]",
            message.producerName, consumer.consumerName, message.topicName, message.messageId,
            message.key, message.publishTime, message.redeliveryCount, message.replicatedFrom
        )
        return message
    }

    override fun onAcknowledge(consumer: Consumer<T>, messageId: MessageId, exception: Throwable) {
        logger.debug("[Pulsar consumer log:OnAcknowledge] ConsumerName:[{}], msgID:[{}], exception:[{}]", consumer.consumerName, messageId, exception)
        return
    }

    override fun onAcknowledgeCumulative(consumer: Consumer<T>, messageId: MessageId, exception: Throwable) {
        logger.debug("[Pulsar consumer log:OnAcknowledgeCumulative] ConsumerName:[{}], msgID:[{}], exception:[{}]", consumer.consumerName, messageId, exception)
        return
    }

    override fun onNegativeAcksSend(consumer: Consumer<T>, messageIds: Set<MessageId>) {
        logger.debug("[Pulsar consumer log:OnNegativeAcksSend] ConsumerName:[{}], msgID:[{}]", consumer.consumerName, messageIds)
    }

    override fun onAckTimeoutSend(consumer: Consumer<T>, messageIds: Set<MessageId>) {
        logger.debug("[Pulsar consumer log:OnAckTimeoutSend] ConsumerName:[{}], msgID:[{}]", consumer.consumerName, messageIds)
    }

    override fun onPartitionsChange(topicName: String, partitions: Int) {
        logger.debug("[Pulsar consumer log:OnPartitionsChange] Topic:[{}], Partitions:[{}]", topicName, partitions)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultConsumerInterceptor::class.java)
    }
}
