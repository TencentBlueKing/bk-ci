package com.tencent.devops.common.pulsar.producer

import com.tencent.devops.common.pulsar.annotation.PulsarProducer
import com.tencent.devops.common.pulsar.enum.Serialization


@PulsarProducer
class ProducerFactory : PulsarProducerFactory {

    private val topics: MutableMap<String, TopicInfo> = HashMap()

    fun addProducer(topic: String): ProducerFactory {
        return addProducer(topic, ByteArray::class.java, Serialization.BYTE)
    }

    fun addProducer(topic: String, clazz: Class<*>): ProducerFactory {
        topics[topic] = TopicInfo(clazz, Serialization.JSON, null)
        return this
    }

    fun addProducer(topic: String, clazz: Class<*>, serialization: Serialization): ProducerFactory {
        topics[topic] = TopicInfo(clazz, serialization, null)
        return this
    }

    fun addProducer(topic: String, namespace: String, clazz: Class<*>, serialization: Serialization): ProducerFactory {
        topics[topic] = TopicInfo(clazz, serialization, namespace)
        return this
    }

    fun addProducer(topic: String, namespace: String, clazz: Class<*>): ProducerFactory {
        topics[topic] = TopicInfo(clazz, Serialization.JSON, namespace)
        return this
    }

    override fun getTopicsInfo(): Map<String, TopicInfo> {
        return topics
    }
}
