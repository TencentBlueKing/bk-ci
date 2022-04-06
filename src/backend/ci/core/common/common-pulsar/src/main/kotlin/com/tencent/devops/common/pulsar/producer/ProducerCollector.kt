package com.tencent.devops.common.pulsar.producer

import com.tencent.devops.common.pulsar.IEvent
import com.tencent.devops.common.pulsar.annotation.PulsarProducer
import com.tencent.devops.common.pulsar.collector.ProducerHolder
import com.tencent.devops.common.pulsar.utils.SchemaUtils
import com.tencent.devops.common.pulsar.utils.UrlBuildService
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.api.Schema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap


@Component
class ProducerCollector  @Autowired constructor(
    private val pulsarClient: PulsarClient,
    private val urlBuildService: UrlBuildService
) : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(ProducerCollector::class.java)
    private val producers: MutableMap<String, Producer<String>> = ConcurrentHashMap()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (bean.javaClass.isAnnotationPresent(PulsarProducer::class.java) && bean is PulsarProducerFactory) {
            producers.putAll(
                bean.getTopicsInfo().map { info ->
                    val holder = ProducerHolder(
                        topic = info.key,
                        clazz = info.value.clazz,
                        serialization = info.value.serialization,
                        namespace = info.value.namespace
                    )
                    holder.topic to buildProducer(holder)
                }
            )
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        return bean
    }

    private fun buildProducer(holder: ProducerHolder): Producer<IEvent> {
        return try {
            pulsarClient.newProducer(getSchema(holder))
                .topic(
                    urlBuildService.buildTopicUrl(holder.topic, holder.namespace)
                ).create()
        } catch (e: PulsarClientException) {
            logger.error("Failed to init producer.", e)
            throw e
        }
    }

    private fun getSchema(holder: ProducerHolder): Schema<IEvent> {
        return SchemaUtils.getSchema(holder.serialization, holder.clazz)
    }

    fun getProducer(topic: String): Producer<*> {
        return producers[topic]!!
    }
}
