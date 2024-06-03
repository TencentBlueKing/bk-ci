package com.tencent.devops.common.stream.pulsar.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PulsarTopicUtilsTest {

    @Test
    fun validateTopicName() {
        PulsarTopicUtils.validateTopicName("e.build.log.status.event.log-service")
        PulsarTopicUtils.validateTopicName("build_log_status_event")
        try {
            PulsarTopicUtils.validateTopicName("build/log/status/event")
        } catch (e: Exception) {
            Assertions.assertEquals(e.javaClass, IllegalArgumentException::class.java)
        }
    }

    @Test
    fun generateTopic() {
        val topic = PulsarTopicUtils.generateTopic(
            tenant = "tenant1",
            namespace = "namespace1",
            topic = "topic1"
        )
        Assertions.assertEquals(topic, "tenant1/namespace1/topic1")
    }
}
