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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
@ManagedResource(objectName = "com.tencent.devops.log.print:type=logs", description = "log print performance")
class LogPrintBean {

    private val printTaskCount = AtomicLong(0)
    private val printActiveCount = AtomicInteger(0)
    private val printQueueSize = AtomicInteger(0)

    @Synchronized
    fun savePrintTaskCount(taskCount: Long) {
        printTaskCount.set(taskCount)
    }

    @Synchronized
    fun savePrintActiveCount(activeCount: Int) {
        printActiveCount.set(activeCount)
    }

    @Synchronized
    fun savePrintQueueSize(queueSize: Int) {
        printQueueSize.set(queueSize)
    }

    @ManagedAttribute
    fun getPrintTaskCount() = printTaskCount.get()

    @ManagedAttribute
    fun getPrintActiveCount() = printActiveCount.get()

    @ManagedAttribute
    fun getPrintQueueSize() = printQueueSize.get()
}
