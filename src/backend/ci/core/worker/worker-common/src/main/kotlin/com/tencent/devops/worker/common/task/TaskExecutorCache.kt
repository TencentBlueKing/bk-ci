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

package com.tencent.devops.worker.common.task

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.process.engine.common.Timeout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 心跳按 taskId 查找当前执行以发起取消，TaskDaemon 负责注册和移除。
 * taskId 在重试中可能复用，进程清理必须使用 Execution.id 区分每一次实际执行。
 */
object TaskExecutorCache {
    // TaskDaemon.call 绑定/清除，派生线程继承；不能按 taskId 重新查缓存，否则迟到线程可能拿到重试的 ID。
    val currentExecution = InheritableThreadLocal<Execution>()
    /** 子进程继承的保留环境变量，不能由用户同名参数覆盖；旧 run 可直接继承，新 run 会显式保留。 */
    const val EXECUTION_ID_ENV = "BK_CI_EXECUTION_ID"

    class Execution(val executor: ExecutorService, val id: String = UUID.randomUUID().toString()) {
        @Volatile var cancelled = false
            private set
        private var future: Future<*>? = null

        @Synchronized
        fun <T> submit(task: Callable<T>): Future<T> {
            // 与 cancel 共用同一把锁，消除“已取消但尚未绑定 Future”以及 shutdown 后仍提交的竞态。
            if (cancelled) throw CancellationException("Task cancelled before execution")
            return executor.submit(task).also { attach(it) }
        }

        @Synchronized
        fun attach(future: Future<*>) {
            // 取消可能早于 Future 注册；绑定时必须补发取消，不能只依赖一次线程池中断。
            this.future = future
            if (cancelled) future.cancel(true)
        }

        @Synchronized
        fun cancel() {
            // 同一取消可能被多轮心跳重复下发；先取消 Future，唤醒等待方，再中断任务线程。
            // shutdownNow 本身不能让一个忽略中断的 Callable 的 Future 立即完成。
            if (cancelled) return
            cancelled = true
            future?.cancel(true)
            executor.shutdownNow()
        }
    }

    private val taskExecutorCache = Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(Timeout.MAX_JOB_RUN_DAYS, TimeUnit.DAYS)
        .build<String, Execution>()

    fun invalidate(taskId: String) { taskExecutorCache.invalidate(taskId) }
    fun put(taskId: String, execution: Execution) { taskExecutorCache.put(taskId, execution) }
    fun getExecution(taskId: String): Execution? = taskExecutorCache.getIfPresent(taskId)
    fun cancel(taskId: String) { getExecution(taskId)?.cancel() }
}