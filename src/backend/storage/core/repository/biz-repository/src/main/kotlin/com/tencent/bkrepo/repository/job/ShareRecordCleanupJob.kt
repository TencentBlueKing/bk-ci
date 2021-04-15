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
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.job

import com.tencent.bkrepo.common.api.util.executeAndMeasureTime
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.repository.dao.ShareRecordDao
import com.tencent.bkrepo.repository.model.TShareRecord
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Share record 清理任务
 */
@Component
class ShareRecordCleanupJob(
    private val shareRecordDao: ShareRecordDao
) {

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    @SchedulerLock(name = "ShareTokenCleanupJob", lockAtMostFor = "PT1H")
    fun cleanup() {
        logger.info("Starting to clean up expired share record.")
        executeAndMeasureTime {
            val expireDate = LocalDateTime.now().minusDays(RESERVE_DAYS)
            val query = Query.query(where(TShareRecord::expireDate).lt(expireDate))
            shareRecordDao.remove(query)
        }.apply {
            logger.info(
                "[${first.deletedCount}] expired share record has been clean up, elapse [${second.seconds}] s."
            )
        }
    }

    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val RESERVE_DAYS = 7L
    }
}
