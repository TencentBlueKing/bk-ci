package com.tencent.devops.common.pulsar

import org.apache.pulsar.client.api.MessageId

class IEvent {
    var properties: Map<String, String>? = null
    var topicName: String? = null
    var key: String? = null
    var messageId: MessageId? = null
    var sequenceId: Long = 0
    var producerName: String? = null
    var publishTime: Long = 0
    var eventTime: Long = 0
}
