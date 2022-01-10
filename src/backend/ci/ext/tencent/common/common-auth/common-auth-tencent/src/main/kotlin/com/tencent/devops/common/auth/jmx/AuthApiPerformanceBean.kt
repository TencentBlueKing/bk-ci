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

package com.tencent.devops.common.auth.jmx

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@ManagedResource
class AuthApiPerformanceBean {

    private val executeCount = AtomicInteger(0)
    private val executeElapse = AtomicLong(0)
    private val calculateCount = AtomicInteger(0)
    private val executeFailure = AtomicInteger(0)

    @Synchronized
    fun execute(elapse: Long, success: Boolean) {
        executeElapse.addAndGet(elapse)
        executeCount.incrementAndGet()
        calculateCount.incrementAndGet()
        if (!success) {
            executeFailure.incrementAndGet()
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getExecutePerformance(): Double {
        val elapse = executeElapse.getAndSet(0)
        val count = calculateCount.getAndSet(0)
        return if (count == 0) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @ManagedAttribute
    fun getExecuteCount() = executeCount.get()

    @ManagedAttribute
    fun getExecuteFailure() = executeFailure.get()
}
