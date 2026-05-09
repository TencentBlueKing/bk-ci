package com.tencent.devops.remotedev.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 异步任务配置
 */
@Configuration
class AsyncConfiguration {

    /**
     * 截图上传任务线程池
     */
    @Bean("screenshotTaskExecutor")
    fun screenshotTaskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 100
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("screenshot-upload-")
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }

    @Bean("remoteDevIoExecutor")
    fun remoteDevIoExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 200
        executor.keepAliveSeconds = 60
        executor.setThreadNamePrefix("remotedev-io-")
        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        executor.initialize()
        return executor
    }
}
