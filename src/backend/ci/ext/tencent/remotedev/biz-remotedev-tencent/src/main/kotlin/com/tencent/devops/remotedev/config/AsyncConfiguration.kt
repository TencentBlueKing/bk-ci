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
}
