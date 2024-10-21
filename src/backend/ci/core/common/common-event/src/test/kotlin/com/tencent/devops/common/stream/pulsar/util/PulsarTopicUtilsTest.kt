package com.tencent.devops.common.stream.pulsar.util

import com.tencent.devops.common.event.annotation.Event
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

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

    @Test
    fun testReflections() {
        val reflections = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                .setExpandSuperTypes(true)
        )
        val re = reflections.getTypesAnnotatedWith(Event::class.java)
        println(re)
        re.forEach { clazz ->
            val event = clazz.getAnnotation(Event::class.java)
            println(
                "Found StreamEvent class: ${clazz.canonicalName}, " +
                    "with destination[${event.destination}]"
            )
        }
    }
}
