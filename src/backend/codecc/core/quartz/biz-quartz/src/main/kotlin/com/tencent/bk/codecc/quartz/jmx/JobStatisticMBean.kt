package com.tencent.bk.codecc.quartz.jmx

import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
@ManagedResource(
    objectName = "com.tencent.bk.codecc.quartz:type=job", description = "job execute info"
)
class JobStatisticMBean @Autowired constructor(
    private val scheduler: Scheduler
) {

    private val successJob = AtomicLong(0)
    private val failJob = AtomicLong(0)

    fun executeStatistic(success: Boolean) {
        if (success)
            successJob.incrementAndGet()
        else
            failJob.incrementAndGet()
    }

    @ManagedAttribute
    fun getSuccessCount() = successJob.get()

    @ManagedAttribute
    fun getFailCount() = failJob.get()

    @ManagedAttribute
    fun getExistingJobCount(): Int {
        return scheduler.getJobKeys(GroupMatcher.groupEquals(CustomSchedulerManager.jobGroup)).size
    }
}