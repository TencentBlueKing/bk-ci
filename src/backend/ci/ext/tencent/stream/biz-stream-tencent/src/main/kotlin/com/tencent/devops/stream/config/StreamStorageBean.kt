package com.tencent.devops.stream.config

import java.util.concurrent.atomic.AtomicLong
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component

@Component
@ManagedResource(objectName = "com.tencent.devops.stream.v2:type=time", description = "stream trigger times")
class StreamStorageBean {
    private val saveRequestTimeCount = AtomicLong(0)
    private val saveRequestTime = AtomicLong(0)

    private val pipelineAndConflictTimeCount = AtomicLong(0)
    private val pipelineAndConflictTime = AtomicLong(0)

    private val conflictTimeCount = AtomicLong(0)
    private val conflictTime = AtomicLong(0)

    private val yamlListCheckTimeCount = AtomicLong(0)
    private val yamlListCheckTime = AtomicLong(0)

    private val triggerCheckTimeCount = AtomicLong(0)
    private val triggerCheckTime = AtomicLong(0)
    private val triggerCheckCount = AtomicLong(0)

    private val prepareYamlTimeCount = AtomicLong(0)
    private val prepareYamlTime = AtomicLong(0)
    private val prepareYamlCount = AtomicLong(0)

    private val buildTime = AtomicLong(0)
    private val buildTimeCount = AtomicLong(0)
    private val buildCount = AtomicLong(0)

    @Synchronized
    fun saveRequestTime(time: Long) {
        saveRequestTimeCount.incrementAndGet()
        saveRequestTime.addAndGet(time)
    }

    @Synchronized
    fun pipelineAndConflictTime(time: Long) {
        pipelineAndConflictTimeCount.incrementAndGet()
        pipelineAndConflictTime.addAndGet(time)
    }

    @Synchronized
    fun conflictTime(time: Long) {
        conflictTimeCount.incrementAndGet()
        conflictTime.addAndGet(time)
    }

    @Synchronized
    fun yamlListCheckTime(time: Long) {
        yamlListCheckTimeCount.incrementAndGet()
        yamlListCheckTime.addAndGet(time)
    }

    @Synchronized
    fun triggerCheckTime(time: Long) {
        triggerCheckTimeCount.incrementAndGet()
        triggerCheckTime.addAndGet(time)
        triggerCheckCount.incrementAndGet()
    }

    @Synchronized
    fun prepareYamlTime(time: Long) {
        prepareYamlTimeCount.incrementAndGet()
        prepareYamlTime.addAndGet(time)
        prepareYamlCount.incrementAndGet()
    }

    @Synchronized
    fun buildTime(time: Long) {
        buildTimeCount.incrementAndGet()
        buildTime.addAndGet(time)
        buildCount.incrementAndGet()
    }

    @Synchronized
    @ManagedAttribute
    fun getSaveRequestTime() = saveRequestTime.getAndSet(0).average(saveRequestTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getPipelineAndConflictTime() =
        pipelineAndConflictTime.getAndSet(0).average(pipelineAndConflictTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getConflictTime() = conflictTime.getAndSet(0).average(conflictTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getYamlListCheckTime() = yamlListCheckTime.getAndSet(0).average(yamlListCheckTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getTriggerCheckTime() = triggerCheckTime.getAndSet(0).average(triggerCheckTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getTriggerCheckCount() = triggerCheckCount.getAndSet(0)

    @Synchronized
    @ManagedAttribute
    fun getPrepareYamlTime() = prepareYamlTime.getAndSet(0).average(prepareYamlTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getPrepareYamlCount() = prepareYamlCount.getAndSet(0)

    @Synchronized
    @ManagedAttribute
    fun getBuildTime() = buildTime.getAndSet(0).average(buildTimeCount.getAndSet(0))

    @Synchronized
    @ManagedAttribute
    fun getBuildCount() = buildCount.getAndSet(0)
}

private fun Long.average(count: Long): Double = if (count == 0L) {
    0.0
} else {
    toDouble() / count
}
