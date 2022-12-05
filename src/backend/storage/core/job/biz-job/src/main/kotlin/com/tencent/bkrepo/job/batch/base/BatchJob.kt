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

package com.tencent.bkrepo.job.batch.base

import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.api.util.executeAndMeasureTime
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.job.config.BatchJobProperties
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/**
 * 抽象批处理作业Job
 * */
abstract class BatchJob(private val batchJobProperties: BatchJobProperties) {
    /**
     * 锁名称
     */
    open fun getLockName(): String = getJobName()

    /**
     * 返回任务名称
     */
    open fun getJobName(): String = javaClass.simpleName

    open fun createJobContext(): JobContext = JobContext()

    /**
     * Job停止标志
     * */
    @Volatile
    private var stop = true

    /**
     * 是否排他执行，如果是则会加分布式锁
     * */
    protected open val isExclusive: Boolean = true

    /**
     * 任务id
     * 唯一标识
     * */
    protected val taskId: String = javaClass.simpleName

    /**
     * 最少加锁时间
     */
    open fun getLockAtLeastFor(): Duration = Duration.ofSeconds(1)

    /**
     * 最长加锁时间
     */
    open fun getLockAtMostFor(): Duration = Duration.ofMinutes(1)

    /**
     * 任务执行进度
     * */
    open fun report(jobContext: JobContext) {}

    @Autowired
    private lateinit var lockingTaskExecutor: LockingTaskExecutor

    open fun start(): Boolean {
        if (!batchJobProperties.enabled) {
            logger.info("Job[${getJobName()}] not enabled,exit job.")
            return false
        }
        logger.info("Start to execute async job[${getJobName()}]")
        stop = false
        val jobContext = createJobContext()
        executeAndMeasureTime {
            if (isExclusive) {
                val task = LockingTaskExecutor.TaskWithResult { doStart(jobContext) }
                val result = lockingTaskExecutor.executeWithLock(task, getLockConfiguration())
                result.wasExecuted()
            } else {
                doStart(jobContext)
                true
            }
        }.apply {
            val (wasExecuted, elapseNano) = this
            if (stop) {
                logger.info("Job[${getJobName()}] stop execution.Execute result: $jobContext")
                return true
            }
            if (wasExecuted) {
                val elapsedTime = HumanReadable.time(elapseNano.toNanos())
                logger.info("Job[${getJobName()}] execution completed, elapse $elapsedTime.Execute result: $jobContext")
            } else {
                logger.info("Job[${getJobName()}] already execution.")
            }
            stop = true
            return first
        }
    }

    /**
     * 启动任务的具体实现
     * */
    abstract fun doStart(jobContext: JobContext)

    /**
     * 停止任务
     * */
    fun stop() {
        stop = true
    }

    /**
     * 任务是否在运行
     * */
    fun isRunning(): Boolean {
        return !stop
    }

    /**
     * 获取分布式锁需要的锁配置
     * */
    private fun getLockConfiguration(): LockConfiguration {
        return LockConfiguration(getLockName(), getLockAtMostFor(), getLockAtLeastFor())
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
    }
}
