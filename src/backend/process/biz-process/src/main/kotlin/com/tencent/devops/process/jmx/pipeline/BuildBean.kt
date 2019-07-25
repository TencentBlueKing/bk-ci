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

package com.tencent.devops.process.jmx.pipeline

import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
@ManagedResource(
    objectName = "com.tencent.devops.process:type=builds",
    description = "build jmx metrics"
)
class BuildBean @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    private val execute = AtomicInteger(0)
    private val executeFailure = AtomicInteger(0)
    private val heartbeatFailure = AtomicInteger(0)

    // 启动类型统计
    private val manualStart = AtomicInteger(0)
    private val timerStart = AtomicInteger(0)
    private val hookStart = AtomicInteger(0)
    private val otherStart = AtomicInteger(0)

    fun executeFailure() {
        executeFailure.incrementAndGet()
    }

    fun heartbeatTimeout() {
        heartbeatFailure.incrementAndGet()
    }

    fun manualStart() {
        manualStart.incrementAndGet()
        execute.incrementAndGet()
    }

    fun timerStart() {
        timerStart.incrementAndGet()
        execute.incrementAndGet()
    }

    fun hookStart() {
        hookStart.incrementAndGet()
        execute.incrementAndGet()
    }

    fun otherStart() {
        otherStart.incrementAndGet()
        execute.incrementAndGet()
    }

    @ManagedAttribute
    fun getExecuteCount(): Int {
        return execute.get()
    }

    @ManagedAttribute
    fun getExecuteFailureCount(): Int {
        return executeFailure.get()
    }

    @ManagedAttribute
    fun getHeartbeatFailureCount() = heartbeatFailure.get()

    @ManagedAttribute
    fun getManualStartCount(): Int {
        return manualStart.get()
    }

    @ManagedAttribute
    fun getTimerStartCount() = timerStart.get()

    @ManagedAttribute
    fun getHookStartCount() = hookStart.get()

    @ManagedAttribute
    fun getOtherStartCount() = otherStart.get()

    @ManagedAttribute
    fun getActiveBuildCount(): Int {
        val epoch = System.currentTimeMillis()
        val runningCount: Int = pipelineRuntimeService.totalRunningBuildCount()
        logger.info("It took ${System.currentTimeMillis() - epoch}ms to list $runningCount build instances")
        return runningCount
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildBean::class.java)
    }
}
