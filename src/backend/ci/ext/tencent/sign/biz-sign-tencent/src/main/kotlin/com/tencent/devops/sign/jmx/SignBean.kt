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

package com.tencent.devops.sign.jmx

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
@ManagedResource(objectName = "com.tencent.devops.log.v2:type=logs", description = "log performance")
class SignBean {

    private val signTaskCount = AtomicLong(0)
    private val resignElapse = AtomicLong(0)
    private val failureCount = AtomicLong(0)

    private val activeCount = AtomicInteger(0)
    private val taskCount = AtomicLong(0)
    private val queueSize = AtomicInteger(0)

    @Synchronized
    fun signTaskFinish(elapse: Long, success: Boolean) {
        signTaskCount.incrementAndGet()
        resignElapse.addAndGet(elapse)
        if (!success) {
            failureCount.incrementAndGet()
        }
    }

    @Synchronized
    fun flushStatus(activeCount: Int, taskCount: Long, queueSize: Int) {
        this.activeCount.set(activeCount)
        this.taskCount.set(taskCount)
        this.queueSize.set(queueSize)
    }

    @Synchronized
    @ManagedAttribute
    fun getLogPerformance(): Double {
        val elapse = resignElapse.getAndSet(0)
        val count = signTaskCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @ManagedAttribute
    fun getSignTaskCount() = signTaskCount.get()

    @ManagedAttribute
    fun getSignElapse() = resignElapse.get()

    @ManagedAttribute
    fun getFailureCount() = failureCount.get()

    @ManagedAttribute
    fun getTaskCount() = taskCount.get()

    @ManagedAttribute
    fun getActiveTaskCount() = activeCount.get()

    @ManagedAttribute
    fun getQueueSizeCount() = queueSize.get()
}
