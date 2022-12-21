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

package com.tencent.bkrepo.job.executor

import com.google.common.util.concurrent.RateLimiter
import com.tencent.bkrepo.common.api.util.HumanReadable
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * 任务信息
 * */
@Suppress("UnstableApiUsage")
data class IdentityTaskInfo(
    val id: String,
    @Volatile
    var complete: Boolean = false,
    // 目前任务数
    var count: AtomicInteger = AtomicInteger(),
    // 已做任务数
    var doneCount: AtomicInteger = AtomicInteger(),
    val monitor: ReentrantLock = ReentrantLock(),
    val flag: Condition = monitor.newCondition(),
    val permitsPerSecond: Double
) {
    private var rateLimiter: RateLimiter? = null
    var waitTime: AtomicInteger = AtomicInteger()
    var executeTime: AtomicInteger = AtomicInteger()

    init {
        if (permitsPerSecond > 0) {
            rateLimiter = RateLimiter.create(permitsPerSecond)
        }
    }

    /**
     * 等待
     * */
    fun await(duration: Duration): Boolean {
        try {
            monitor.lock()
            return flag.await(duration.seconds, TimeUnit.SECONDS)
        } finally {
            monitor.unlock()
        }
    }

    /**
     * 通知所有等待线程
     * */
    fun signalAll() {
        try {
            monitor.lock()
            flag.signalAll()
        } finally {
            monitor.unlock()
        }
    }

    /**
     * 获取许可证
     * */
    fun acquire() {
        rateLimiter?.acquire()
    }

    /**
     * 平均任务等待时间
     * */
    fun avgWaitTime(): Int {
        if (doneCount.get() <= 0) {
            return 0
        }
        return waitTime.get() / doneCount.get()
    }

    /**
     * 平均任务执行时长
     * */
    fun avgExecuteTime(): Int {
        if (doneCount.get() <= 0) {
            return 0
        }
        return executeTime.get() / doneCount.get()
    }

    override fun toString(): String {
        val avgWaitTime = Duration.ofMillis(avgWaitTime().toLong()).toNanos()
        val avgExecuteTime = Duration.ofMillis(avgExecuteTime().toLong()).toNanos()
        return "Task[$id] state: complete[$complete]," +
            "remain[$count],done[$doneCount],tps[$permitsPerSecond]," +
            "avgWaitTime ${HumanReadable.time(avgWaitTime)}," +
            "avgExecuteTime ${HumanReadable.time(avgExecuteTime)}"
    }
}
