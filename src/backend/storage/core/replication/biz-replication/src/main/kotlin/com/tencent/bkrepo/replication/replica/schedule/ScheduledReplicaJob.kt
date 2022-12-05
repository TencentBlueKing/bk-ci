/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.replica.schedule

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import java.time.Duration

/**
 * 调度类型同步任务job
 */
class ScheduledReplicaJob : InterruptableJob {

    private var currentThread: Thread? = null
    private val lockingTaskExecutor = SpringContextUtils.getBean(LockingTaskExecutor::class.java)
    private val scheduledReplicaJobExecutor = SpringContextUtils.getBean(ScheduledReplicaJobExecutor::class.java)

    override fun execute(context: JobExecutionContext) {
        currentThread = Thread.currentThread()
        val taskId = context.jobDetail.key.name
        val lockName = buildLockName(taskId)
        val lockConfiguration = LockConfiguration(lockName, lockAtMostFor, lockAtLeastFor)
        lockingTaskExecutor.executeWithLock(Runnable { scheduledReplicaJobExecutor.execute(taskId) }, lockConfiguration)
    }

    override fun interrupt() {
        currentThread?.interrupt()
    }

    private fun buildLockName(taskId: String): String {
        return REPLICA_LOCK_NAME_PREFIX + taskId
    }

    companion object {
        /**
         * 任务最短加锁时间
         */
        private val lockAtLeastFor = Duration.ofSeconds(1)

        /**
         * 任务最长加锁时间
         */
        private val lockAtMostFor = Duration.ofDays(1)

        private const val REPLICA_LOCK_NAME_PREFIX = "REPLICA_JOB_"
    }
}
