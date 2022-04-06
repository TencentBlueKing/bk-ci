package com.tencent.devops.common.pulsar.producer

import com.tencent.devops.common.pulsar.enum.Serialization

interface PulsarProducerFactory {
    fun getTopicsInfo(): Map<String, TopicInfo>
}

data class TopicInfo(
    val clazz: Class<String>,
    val serialization: Serialization,
    val namespace: String?
)
