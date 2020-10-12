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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.sdk.jmx

import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.docker.sdk.listener.BuildListener
import com.tencent.devops.dispatch.docker.sdk.service.DispatchService
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
@ManagedResource(objectName = "com.tencent.devops.dispatcher.sdk:type=builds",
        description = "dispatcher sdk build jmx metrics")
class BuildBean {

    private val start = AtomicLong(0)
    private val startFailure = AtomicLong(0)

    private val shutdown = AtomicLong(0)
    private val shutdownFailure = AtomicLong(0)

    fun start(success: Boolean) {
        start.incrementAndGet()
        if (!success) {
            startFailure.incrementAndGet()
        }
    }

    fun shutdown(success: Boolean) {
        shutdown.incrementAndGet()
        if (!success) {
            shutdownFailure.incrementAndGet()
        }
    }

    @ManagedAttribute
    fun getStartCount(): Long {
        return start.get()
    }

    @ManagedAttribute
    fun getStartFailureCount(): Long {
        return startFailure.get()
    }

    @ManagedAttribute
    fun getShutdownCount() = shutdown.get()

    @ManagedAttribute
    fun getShutdownFailureCount() = shutdownFailure.get()

    private var startQueue: String? = null
    private var dispatchService: DispatchService? = null

    @ManagedAttribute
    fun getExecuteCount(): Long {
        if (startQueue == null) {
            synchronized(this) {
                if (startQueue == null) {
                    startQueue = SpringContextUtil.getBean(BuildListener::class.java).getStartupQueue()
                }
            }
        }

        if (dispatchService == null) {
            synchronized(this) {
                if (dispatchService == null) {
                    dispatchService = SpringContextUtil.getBean(DispatchService::class.java)
                }
            }
        }
        return dispatchService!!.getExecuteCount(startQueue!!)
    }
}
