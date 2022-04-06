package com.tencent.devops.common.pulsar.collector

import com.tencent.devops.common.pulsar.IEvent
import com.tencent.devops.common.pulsar.enum.Serialization
import java.util.Optional


data class ProducerHolder (
    val topic: String,
    val clazz: Class<IEvent>,
    val serialization: Serialization,
    val namespace: String?
)
