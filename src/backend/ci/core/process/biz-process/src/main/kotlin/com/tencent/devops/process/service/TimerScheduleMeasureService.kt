package com.tencent.devops.process.service

import com.tencent.devops.process.constant.MeasureConstant.TAG_KEY_PIPELINE_ID
import com.tencent.devops.process.constant.MeasureConstant.TAG_KEY_PROJECT_ID
import com.tencent.devops.process.constant.MeasureConstant.TAG_KEY_TASK_ID
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerBuildEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class TimerScheduleMeasureService @Autowired constructor(
    val meterRegistry: MeterRegistry
) {
    companion object {
        val logger = LoggerFactory.getLogger(TimerScheduleMeasureService::class.java)
    }

    /**
     * 记录任务执行时间
     */
    fun recordTaskExecutionTime(
        name: String,
        context: JobExecutionContext,
        timeConsumingMills: Long,
        tags: List<Tag> = listOf()
    ) {
        Timer.builder(name)
                .description("pipeline timer trigger task execution time")
                .tags(parseContextTags(context).plus(tags))
                .publishPercentileHistogram(true)
                .minimumExpectedValue(Duration.ofMillis(10))
                .maximumExpectedValue(Duration.ofSeconds(30))
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
                    .publishPercentileHistogram(true)
                    .minimumExpectedValue(Duration.ofSeconds(1))
                    .maximumExpectedValue(Duration.ofSeconds(120))
                    .register(meterRegistry)
                    .record(timeConsumingMills, TimeUnit.MILLISECONDS)
        }
    }

    fun parseContextTags(context: JobExecutionContext): MutableList<Tag> {
        val jobKey = context.jobDetail?.key ?: return mutableListOf()
        val comboKey = jobKey.name
        // 格式：pipelineId_{md5}_{projectId}_{taskId}
        val comboKeys = comboKey.split(Regex("_"), 4)
        val pipelineId = comboKeys[0]
        val projectId = comboKeys[2]
        val taskId = comboKeys.getOrElse(3) { "" }
        return mutableListOf(
            Tag.of(TAG_KEY_PROJECT_ID, projectId),
            Tag.of(TAG_KEY_PIPELINE_ID, pipelineId),
            Tag.of(TAG_KEY_TASK_ID, taskId),
        )
    }
}
