package com.tencent.devops.buildless.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 线程池封装类
 *
 */
@Configuration
class ThreadPoolConfig {

    /**
     * cpu数量
     * */
    private val cpuCount = Runtime.getRuntime().availableProcessors()

    /**
     * 核心线程数为手机CPU数量+1
     * */
    private val coolPoolSize = cpuCount + 1

    /**
     * 最大线程数为手机CPU数量×2+1
     * */
    private val maxPoolSize = cpuCount * 2 + 1

    /**
     * 线程活跃时间 秒，超时线程会被回收
     * */
    private val keepAliveTime: Int = 60

    /**
     * 等待队列大小
     * */
    private val queueSize = 128

    @Bean("taskExecutor")
    fun taskExecutor(): Executor {
        val taskExecutor = ThreadPoolTaskExecutor()
        taskExecutor.corePoolSize = coolPoolSize
        taskExecutor.maxPoolSize = maxPoolSize
        taskExecutor.keepAliveSeconds = keepAliveTime
        taskExecutor.setQueueCapacity(queueSize)
        taskExecutor.setAllowCoreThreadTimeOut(true)
        taskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())

        taskExecutor.initialize()

        return taskExecutor
    }
}
