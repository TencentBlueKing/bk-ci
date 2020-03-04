package com.tencent.devops.misc.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar

import java.util.concurrent.Executors

/**
 * @Description
 * @Date 2020/2/24
 * @Version 1.0
 */

// 设定一个长度3的定时任务线程池
@Configuration
class ScheduleConfig : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(3))
    }
}
