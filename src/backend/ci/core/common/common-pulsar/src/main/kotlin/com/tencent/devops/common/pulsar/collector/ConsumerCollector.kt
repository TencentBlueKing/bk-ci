package com.tencent.devops.common.pulsar.collector


import com.beust.jcommander.internal.Sets
import com.tencent.devops.common.pulsar.annotation.PulsarConsumer
import com.tencent.devops.common.pulsar.utils.UrlBuildService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Configuration
import java.util.Arrays
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap


@Configuration
class ConsumerCollector @Autowired constructor(
    private val urlBuildService: UrlBuildService
) : BeanPostProcessor {

    private val consumers: MutableMap<String, ConsumerHolder> = ConcurrentHashMap<String, ConsumerHolder>()
    private val nonAnnotatedClasses = Sets.newHashSet<Class<*>>()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        val beanClass = bean.javaClass
        if (nonAnnotatedClasses.contains(beanClass)) {
            return bean
        }
        consumers.putAll(
            beanClass.declaredMethods.filter { method ->
                if (!method.isAnnotationPresent(PulsarConsumer::class.java)) {
                    nonAnnotatedClasses.add(beanClass)
                    logger.trace("No @PulsarConsumer annotations found on bean type: " + bean.javaClass)
                    return@filter false
                }
                true
            }.map { method ->
                urlBuildService.buildConsumerName(beanClass, method) to ConsumerHolder(
                    method.getAnnotation(PulsarConsumer::class.java), method, bean,
                    method.parameterTypes[0]
                )
            }
        )
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        return bean
    }

    fun getConsumers(): Map<String, ConsumerHolder> {
        return consumers
    }

    fun getConsumer(methodDescriptor: String): Optional<ConsumerHolder> {
        return Optional.ofNullable<ConsumerHolder>(consumers[methodDescriptor])
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConsumerCollector::class.java)
    }
}
