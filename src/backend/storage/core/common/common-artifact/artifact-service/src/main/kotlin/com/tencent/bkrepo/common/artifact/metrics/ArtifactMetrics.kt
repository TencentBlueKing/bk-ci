/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Suppress("LateinitUsage")
@Component
class ArtifactMetrics(
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor
) : MeterBinder {

    var uploadingCount = AtomicInteger(0)
    var downloadingCount = AtomicInteger(0)

    lateinit var uploadedSizeCounter: Counter
    lateinit var uploadedConsumeTimer: Timer
    lateinit var downloadedSizeCounter: Counter
    lateinit var downloadedConsumeTimer: Timer

    override fun bindTo(meterRegistry: MeterRegistry) {
        Gauge.builder(ARTIFACT_UPLOADING_COUNT, uploadingCount, { it.get().toDouble() })
            .description(ARTIFACT_UPLOADING_COUNT_DESC)
            .register(meterRegistry)

        Gauge.builder(ARTIFACT_DOWNLOADING_COUNT, downloadingCount, { it.get().toDouble() })
            .description(ARTIFACT_DOWNLOADING_COUNT_DESC)
            .register(meterRegistry)

        uploadedSizeCounter = Counter.builder(ARTIFACT_UPLOADED_CONSUME)
            .description(ARTIFACT_UPLOADED_CONSUME_DESC)
            .tag("type", "size")
            .register(meterRegistry)

        uploadedConsumeTimer = Timer.builder(ARTIFACT_UPLOADED_CONSUME)
            .description(ARTIFACT_UPLOADED_CONSUME_DESC)
            .tag("type", "timer")
            .register(meterRegistry)

        downloadedSizeCounter = Counter.builder(ARTIFACT_DOWNLOADED_CONSUME)
            .description(ARTIFACT_DOWNLOADED_CONSUME_DESC)
            .tag("type", "size")
            .register(meterRegistry)

        downloadedConsumeTimer = Timer.builder(ARTIFACT_DOWNLOADED_CONSUME)
            .description(ARTIFACT_DOWNLOADED_CONSUME_DESC)
            .tag("type", "timer")
            .register(meterRegistry)

        Gauge.builder(ASYNC_TASK_ACTIVE_COUNT, threadPoolTaskExecutor.threadPoolExecutor, { it.activeCount.toDouble() })
            .description(ASYNC_TASK_ACTIVE_COUNT_DESC)
            .register(meterRegistry)

        Gauge.builder(ASYNC_TASK_QUEUE_SIZE, threadPoolTaskExecutor.threadPoolExecutor, { it.queue.size.toDouble() })
            .description(ASYNC_TASK_QUEUE_SIZE_DESC)
            .register(meterRegistry)
    }
}
