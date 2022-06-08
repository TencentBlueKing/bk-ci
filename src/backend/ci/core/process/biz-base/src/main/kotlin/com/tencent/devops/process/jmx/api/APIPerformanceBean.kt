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

package com.tencent.devops.process.jmx.api

import com.google.common.util.concurrent.AtomicDouble
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@ManagedResource
class APIPerformanceBean() {
    private val executeCount = AtomicInteger(0)
    private val executeElapse = AtomicLong(0)
    private val calculateCount = AtomicInteger(0)
    private val executePerformance = AtomicDouble(0.0)

    constructor(meterRegistry: PrometheusMeterRegistry, api: String) : this() {
        Gauge.builder("jvm_process_api_performance") {
            executePerformance
        }.tags("api", api, "paths", "ExecutePerformance").register(meterRegistry)
    }

    @Synchronized
    fun execute(elapse: Long) {
        executeElapse.addAndGet(elapse)
        executeCount.incrementAndGet()
        calculateCount.incrementAndGet()
    }

    @Synchronized
    @ManagedAttribute
    fun getExecutePerformance(): Double {
        val elapse = executeElapse.getAndSet(0)
        val count = calculateCount.getAndSet(0)
        executePerformance.set(
            if (count == 0) {
                0.0
            } else {
                elapse.toDouble() / count
            }
        )
        return executePerformance.get()
    }

    @ManagedAttribute
    fun getExecuteCount() = executeCount.get()
}
