package com.tencent.devops.sign.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy

@Configuration
@EnableAsync
class SignExecutorConfig {

    @Bean(name = ["asyncSignExecutor"])
    fun asyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 15
        executor.setQueueCapacity(25)
        executor.keepAliveSeconds = 200
        executor.threadNamePrefix = "asyncSign-"
        executor.setRejectedExecutionHandler(CallerRunsPolicy())
        // 等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(true)
        // 线程池中任务的等待时间，如果超过这个时候还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住
        executor.setAwaitTerminationSeconds(60)
        executor.initialize()
        return executor
    }
}