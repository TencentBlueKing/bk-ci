package com.tencent.devops.dispatch.docker.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Configuration
class ScheduleConfig : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler = ScheduledThreadPoolExecutor(5)
        scheduler.maximumPoolSize = 5
        scheduler.setKeepAliveTime(60, TimeUnit.SECONDS)
        scheduler.rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()
        taskRegistrar.setScheduler(scheduler)
    }
}
