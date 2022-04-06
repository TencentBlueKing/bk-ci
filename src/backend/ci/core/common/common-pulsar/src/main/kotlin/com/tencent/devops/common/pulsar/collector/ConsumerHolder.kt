package com.tencent.devops.common.pulsar.collector

import com.tencent.devops.common.pulsar.IEvent
import com.tencent.devops.common.pulsar.annotation.PulsarConsumer
import java.lang.reflect.Method


data class ConsumerHolder internal constructor(
    val annotation: PulsarConsumer,
    val handler: Method,
    val bean: Any,
    private val type: Class<*>
) {
    val isWrapped = type.isAssignableFrom(IEvent::class.java)
}
