package com.tencent.devops.misc.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Configuration
class ScheduleConfig : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler = ScheduledThreadPoolExecutor(20)
        scheduler.maximumPoolSize = 500
        scheduler.setKeepAliveTime(60, TimeUnit.SECONDS)
        scheduler.rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()
        taskRegistrar.setScheduler(scheduler)
    }
}