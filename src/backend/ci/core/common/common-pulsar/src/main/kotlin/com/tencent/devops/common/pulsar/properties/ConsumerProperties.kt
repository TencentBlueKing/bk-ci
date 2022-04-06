package com.tencent.devops.common.pulsar.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pulsar.consumer.default")
class ConsumerProperties {
    var deadLetterPolicyMaxRedeliverCount = -1
    var ackTimeoutMs = 0
    var subscriptionType = ""
}
