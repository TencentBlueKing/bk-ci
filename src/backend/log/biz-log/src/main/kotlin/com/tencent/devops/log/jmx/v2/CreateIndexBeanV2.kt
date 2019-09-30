package com.tencent.devops.log.jmx.v2

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * deng
 * 2019-02-28
 */
@Component
@ManagedResource(objectName = "com.tencent.devops.log.v2:type=index,operation=create", description = "log create index performance")
class CreateIndexBeanV2 {

    private val executeCount = AtomicLong(0)
    private val executeElapse = AtomicLong(0)
    private val calculateCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)

    @Synchronized
    fun execute(elapse: Long, success: Boolean) {
        executeCount.incrementAndGet()
        calculateCount.incrementAndGet()
        executeElapse.addAndGet(elapse)
        if (!success) {
            failureCount.incrementAndGet()
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getCreateIndexPerformance(): Double {
        val elapse = executeElapse.getAndSet(0)
        val count = calculateCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @ManagedAttribute
    fun getExecuteCount() = executeCount.get()

    @ManagedAttribute
    fun getFailureCount() = failureCount.get()
}