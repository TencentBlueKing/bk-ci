/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.job

import com.tencent.bkrepo.common.redis.RedisLock
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.BLOCKED
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.CREATED
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.ENQUEUED
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.EXECUTING
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.PULLED
import com.tencent.bkrepo.scanner.dao.SubScanTaskDao
import com.tencent.bkrepo.scanner.metrics.ScannerMetrics
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 矫正metric中的数据
 */
@Component
class CorrectMetricsJob(
    private val redisOperation: RedisOperation,
    private val subScanTaskDao: SubScanTaskDao,
    private val scannerMetrics: ScannerMetrics
) {
    private val jobLock by lazy {
        RedisLock(redisOperation, JOB_LOCK_KEY, FIXED_DELAY / 1000L)
    }

    @Scheduled(fixedDelay = FIXED_DELAY)
    fun correct() {
        // 不释放锁，等待锁过期，避免其他服务实例重复执行任务
        if (!jobLock.tryLock()) {
            return
        }

        // 由于扫描任务数量统计使用了redis，状态变更时涉及多个非原子的任务状态数量增减操作，需要与数据库的数据同步
        CORRECT_SUBTASK_STATUS.forEach {
            val count = subScanTaskDao.countStatus(it)
            scannerMetrics.setSubtaskCount(it, count)
            logger.info("correct subtaskStatus[$it] count[$count] success")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CorrectMetricsJob::class.java)
        private val JOB_LOCK_KEY = "job:lock:${CorrectMetricsJob::class.java.name}"
        private val CORRECT_SUBTASK_STATUS = listOf(BLOCKED, CREATED, PULLED, ENQUEUED, EXECUTING)
        private const val FIXED_DELAY = 60 * 60 * 1000L
    }
}
