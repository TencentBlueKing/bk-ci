/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.service.async

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurerSupport
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy

@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AsyncProperties::class)
@Configuration
class AsyncConfiguration : AsyncConfigurerSupport() {

    @Autowired
    private lateinit var properties: AsyncProperties

    /**
     * Spring异步任务Executor
     */
    @Bean("taskAsyncExecutor")
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = properties.corePoolSize
        executor.maxPoolSize = properties.maxPoolSize
        executor.setQueueCapacity(properties.queueCapacity)
        executor.keepAliveSeconds = properties.keepAliveSeconds
        executor.setThreadNamePrefix(properties.threadNamePrefix)
        executor.setRejectedExecutionHandler(CallerRunsPolicy())
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(DEFAULT_AWAIT_TERMINATION_SECONDS)
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler() = AsyncUncaughtExceptionHandler { error, method, _ ->
        logger.error("Unexpected exception occurred invoking async method: {}", method, error)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncConfiguration::class.java)
        private const val DEFAULT_AWAIT_TERMINATION_SECONDS = 5 * 60
    }
}
