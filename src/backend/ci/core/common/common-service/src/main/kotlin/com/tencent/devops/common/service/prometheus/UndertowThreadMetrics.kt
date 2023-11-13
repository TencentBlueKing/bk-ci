package com.tencent.devops.common.service.prometheus

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.MalformedObjectNameException
import javax.management.ObjectName

class UndertowThreadMetrics : MeterBinder {
    private val platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
    override fun bindTo(registry: MeterRegistry) {
        buildAndRegisterGauge(
            GAUGE_NAME_WORKER_QUEUE_SIZE,
            ATTR_WORKER_QUEUE_SIZE,
            "Undertow worker queue size",
            registry
        )
        buildAndRegisterGauge(
            GAUGE_NAME_MAX_WORKER_POOL_SIZE,
            ATTR_MAX_WORKER_POOL_SIZE,
            "Undertow max worker pool size",
            registry
        )
        buildAndRegisterGauge(
            GAUGE_NAME_IO_THREAD_COUNT,
            ATTR_IO_THREAD_COUNT,
            "Undertow IO thread count",
            registry
        )
        buildAndRegisterGauge(
            GAUGE_NAME_WORKER_POOL_SIZE,
            ATTR_WORKER_POOL_SIZE,
            "Undertow worker pool size",
            registry
        )
    }

    private fun buildAndRegisterGauge(
        name: String,
        attributeName: String,
        description: String,
        registry: MeterRegistry
    ) {
        Gauge.builder(
            name,
            platformMBeanServer
        ) { mBeanServer -> getWorkerAttribute(mBeanServer, attributeName) }
            .description(description)
            .register(registry)
    }

    private fun getWorkerAttribute(mBeanServer: MBeanServer, attributeName: String): Double {
        var attributeValueObj: Any? = null
        try {
            attributeValueObj = mBeanServer.getAttribute(workerObjectName(), attributeName)
        } catch (e: Exception) {
            logger.warn("Unable to get {} from JMX", ATTR_WORKER_QUEUE_SIZE, e)
        }
        return NumberUtils.toDouble(attributeValueObj.toString(), 0.0)
    }

    @Throws(MalformedObjectNameException::class)
    private fun workerObjectName(): ObjectName {
        return ObjectName(OBJECT_NAME)
    }

    companion object {
        private const val OBJECT_NAME = "org.xnio:type=Xnio,provider=\"nio\",worker=\"XNIO-2\""
        private const val GAUGE_NAME_WORKER_QUEUE_SIZE = "undertow.worker.queue.size"
        private const val GAUGE_NAME_MAX_WORKER_POOL_SIZE = "undertow.worker.pool.max"
        private const val GAUGE_NAME_IO_THREAD_COUNT = "undertow.io.thread-count"
        private const val GAUGE_NAME_WORKER_POOL_SIZE = "undertow.worker.pool.size"
        private const val ATTR_WORKER_QUEUE_SIZE = "WorkerQueueSize"
        private const val ATTR_MAX_WORKER_POOL_SIZE = "MaxWorkerPoolSize"
        private const val ATTR_IO_THREAD_COUNT = "IoThreadCount"
        private const val ATTR_WORKER_POOL_SIZE = "WorkerPoolSize"
        private val logger = LoggerFactory.getLogger(UndertowThreadMetrics::class.java)
    }
}
