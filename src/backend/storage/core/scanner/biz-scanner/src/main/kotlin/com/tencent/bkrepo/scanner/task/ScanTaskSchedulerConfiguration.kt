/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.task

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy

@Configuration(proxyBeanMethods = false)
class ScanTaskSchedulerConfiguration {
    @Bean(SCAN_TASK_SCHEDULER_THREAD_POOL_BEAN_NAME)
    fun scanTaskSchedulerThreadPool(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = Runtime.getRuntime().availableProcessors() + 1
            maxPoolSize = corePoolSize
            setQueueCapacity(DEFAULT_QUEUE_CAPACITY)
            setAllowCoreThreadTimeOut(true)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(DEFAULT_AWAIT_TERMINATION_SECONDS)
            threadNamePrefix = SCAN_TASK_SCHEDULER_THREAD_NAME_PREFIX
            setRejectedExecutionHandler(DiscardPolicy())
        }
    }

    companion object {
        const val SCAN_TASK_SCHEDULER_THREAD_POOL_BEAN_NAME = "scanTaskSchedulerThreadPool"
        private const val DEFAULT_AWAIT_TERMINATION_SECONDS = 300
        private const val DEFAULT_QUEUE_CAPACITY = 200
        private const val SCAN_TASK_SCHEDULER_THREAD_NAME_PREFIX = "scanner-task-scheduler-"
    }
}
