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

package com.tencent.bkrepo.job.metrics

import com.tencent.bkrepo.job.ASYNC_TASK_ACTIVE_COUNT
import com.tencent.bkrepo.job.ASYNC_TASK_ACTIVE_COUNT_DESC
import com.tencent.bkrepo.job.ASYNC_TASK_QUEUE_SIZE
import com.tencent.bkrepo.job.ASYNC_TASK_QUEUE_SIZE_DESC
import com.tencent.bkrepo.job.JOB_AVG_EXECUTE_TIME_CONSUME_DESC
import com.tencent.bkrepo.job.JOB_AVG_TIME_CONSUME
import com.tencent.bkrepo.job.JOB_AVG_WAIT_TIME_CONSUME_DESC
import com.tencent.bkrepo.job.JOB_TASK_COUNT
import com.tencent.bkrepo.job.JOB_TASK_COUNT_DESC
import com.tencent.bkrepo.job.RUNNING_TASK_JOB_COUNT
import com.tencent.bkrepo.job.RUNNING_TASK_JOB_DESC
import com.tencent.bkrepo.job.executor.BlockThreadPoolTaskExecutorDecorator
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

/**
 * Job服务度量指标
 * */
@Component
class JobMetrics(
    val threadPoolTaskExecutor: BlockThreadPoolTaskExecutorDecorator
) : MeterBinder {

    lateinit var jobTasksCounter: Counter
    lateinit var jobAvgWaitTimeConsumeTimer: Timer
    lateinit var jobAvgExecuteTimeConsumeTimer: Timer

    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder(ASYNC_TASK_ACTIVE_COUNT, threadPoolTaskExecutor) { it.activeCount().toDouble() }
            .description(ASYNC_TASK_ACTIVE_COUNT_DESC)
            .register(registry)

        Gauge.builder(ASYNC_TASK_QUEUE_SIZE, threadPoolTaskExecutor) { it.queueSize().toDouble() }
            .description(ASYNC_TASK_QUEUE_SIZE_DESC)
            .register(registry)

        Gauge.builder(RUNNING_TASK_JOB_COUNT, threadPoolTaskExecutor) { it.activeTaskCount().toDouble() }
            .description(RUNNING_TASK_JOB_DESC)
            .register(registry)
        jobTasksCounter = Counter.builder(JOB_TASK_COUNT)
            .description(JOB_TASK_COUNT_DESC)
            .register(registry)
        jobAvgWaitTimeConsumeTimer = Timer.builder(JOB_AVG_TIME_CONSUME)
            .description(JOB_AVG_WAIT_TIME_CONSUME_DESC)
            .tag("type", "waitTime")
            .register(registry)
        jobAvgExecuteTimeConsumeTimer = Timer.builder(JOB_AVG_TIME_CONSUME)
            .description(JOB_AVG_EXECUTE_TIME_CONSUME_DESC)
            .tag("type", "executeTime")
            .register(registry)
    }
}
