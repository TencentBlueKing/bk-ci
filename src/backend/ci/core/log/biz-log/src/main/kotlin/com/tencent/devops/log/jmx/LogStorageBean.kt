/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.log.jmx

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Suppress("TooManyFunctions")
@Component
@ManagedResource(objectName = "com.tencent.devops.log.v2:type=logs", description = "log performance")
class LogStorageBean {

    private val batchWriteCount = AtomicLong(0)
    private val batchWriteElapse = AtomicLong(0)
    private val calculateCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)

    private val bulkRequestCount = AtomicLong(0)
    private val bulkRequestElapse = AtomicLong(0)
    private val bulkRequestFailureCount = AtomicLong(0)

    private val queryLogCount = AtomicLong(0)
    private val queryLogElapse = AtomicLong(0)
    private val queryCalculateCount = AtomicLong(0)
    private val queryFailureCount = AtomicLong(0)

    private val downloadLogCount = AtomicLong(0)
    private val downloadLogElapse = AtomicLong(0)
    private val downloadCalculateCount = AtomicLong(0)
    private val downloadFailureCount = AtomicLong(0)

    @Synchronized
    fun download(elapse: Long, success: Boolean) {
        downloadLogCount.incrementAndGet()
        downloadCalculateCount.incrementAndGet()
        downloadLogElapse.addAndGet(elapse)
        if (!success) {
            downloadFailureCount.incrementAndGet()
        }
    }

    @Synchronized
    fun batchWrite(elapse: Long, success: Boolean) {
        batchWriteCount.incrementAndGet()
        calculateCount.incrementAndGet()
        batchWriteElapse.addAndGet(elapse)
        if (!success) {
            failureCount.incrementAndGet()
        }
    }

    @Synchronized
    fun bulkRequest(elapse: Long, success: Boolean) {
        bulkRequestCount.incrementAndGet()
        bulkRequestElapse.addAndGet(elapse)
        if (!success) {
            bulkRequestFailureCount.incrementAndGet()
        }
    }

    @Synchronized
    fun query(elapse: Long, success: Boolean) {
        queryLogCount.incrementAndGet()
        queryCalculateCount.incrementAndGet()
        queryLogElapse.addAndGet(elapse)
        if (!success) {
            queryFailureCount.incrementAndGet()
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getLogPerformance(): Double {
        val elapse = batchWriteElapse.getAndSet(0)
        val count = calculateCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getBulkPerformance(): Double {
        val elapse = bulkRequestElapse.getAndSet(0)
        val count = bulkRequestCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getQueryLogPerformance(): Double {
        val elapse = queryLogElapse.getAndSet(0)
        val count = queryCalculateCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @Synchronized
    @ManagedAttribute
    fun getDownloadLogPerformance(): Double {
        val elapse = downloadLogElapse.getAndSet(0)
        val count = downloadCalculateCount.getAndSet(0)
        return if (count == 0L) {
            0.0
        } else {
            elapse.toDouble() / count
        }
    }

    @ManagedAttribute
    fun getExecuteCount() = batchWriteCount.get()

    @ManagedAttribute
    fun getFailureCount() = failureCount.get()

    @ManagedAttribute
    fun getBulkFailureCount() = bulkRequestFailureCount.get()

    @ManagedAttribute
    fun getQueryCount() = queryLogCount.get()

    @ManagedAttribute
    fun getQueryFailureCount() = queryFailureCount.get()

    @ManagedAttribute
    fun getDownloadFailureCount() = downloadFailureCount.get()
}
