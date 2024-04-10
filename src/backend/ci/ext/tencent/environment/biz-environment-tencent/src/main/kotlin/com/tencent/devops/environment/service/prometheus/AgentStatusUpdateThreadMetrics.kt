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

package com.tencent.devops.environment.service.prometheus

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder

class AgentStatusUpdateThreadMetrics : MeterBinder {
    companion object {
        private const val THREAD_METRICS_PREFIX = "agent_status_update_"
        private const val GAUGE_NAME_ACTIVE_THREAD_COUNT = "activeThreadCount" // 活跃线程数
        private const val GAUGE_NAME_CORE_THREAD_COUNT = "coreThreadCount" // 核心线程数
        private const val GAUGE_NAME_MAX_THREAD_COUNT = "maxThreadCount" // 最大线程数
    }

    val map = hashMapOf<String, Double>()

    override fun bindTo(registry: MeterRegistry) {
        buildAndRegisterGauge(
            GAUGE_NAME_ACTIVE_THREAD_COUNT,
            "[AgentStatusUpdate] active thread count",
            registry
        )
        buildAndRegisterGauge(
            GAUGE_NAME_CORE_THREAD_COUNT,
            "[AgentStatusUpdate] core thread count",
            registry
        )
        buildAndRegisterGauge(
            GAUGE_NAME_MAX_THREAD_COUNT,
            "[AgentStatusUpdate] max thread count",
            registry
        )
    }

    private fun buildAndRegisterGauge(
        name: String,
        description: String,
        registry: MeterRegistry
    ) {
        Gauge.builder(
            THREAD_METRICS_PREFIX.plus(name),
            map
        ) { m -> m[name] ?: -1.0 }
            .description(description)
            .register(registry)
    }
}