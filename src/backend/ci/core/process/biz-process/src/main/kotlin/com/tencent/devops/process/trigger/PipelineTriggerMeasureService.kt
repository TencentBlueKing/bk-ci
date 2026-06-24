package com.tencent.devops.process.trigger

import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@SuppressWarnings("SpreadOperator")
class PipelineTriggerMeasureService @Autowired constructor(
    val meterRegistry: MeterRegistry
) {
    /**
     * 记录任务执行时间
     */
    fun recordTaskExecutionTime(
        name: String,
        timeConsumingMills: Long,
        tags: List<Tag> = listOf()
    ) {
        Timer.builder(name)
                .description("pipeline trigger task execution time")
                .tags(tags)
                .register(meterRegistry)
                .record(timeConsumingMills, TimeUnit.MILLISECONDS)
    }

    /**
     * 记录任务实际执行耗时
     * 定时任务触发 ~ 流水线触发
     */
    fun recordActualExecutionTime(
        name: String,
        event: PipelineTimerBuildEvent,
        tags: List<Tag> = listOf()
    ) {
        event.expectedStartTime?.let {
            val timeConsumingMills = System.currentTimeMillis() - it
            Timer.builder(name)
                    .description("pipeline timer trigger actual execution time")
                    .tags(tags)
                    .register(meterRegistry)
                    .record(timeConsumingMills, TimeUnit.MILLISECONDS)
        }
    }
}
