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
 *
 */

package com.tencent.devops.auth.cron

import com.tencent.devops.auth.dao.AuthUserDailyDao
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MeasureEventDispatcher
import com.tencent.devops.common.event.pojo.measure.ProjectUserCountDailyEvent
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AuthUserMetricsScheduler @Autowired constructor(
    private val dslContext: DSLContext,
    private val authUserDailyDao: AuthUserDailyDao,
    private val redisOperation: RedisOperation,
    private val measureEventDispatcher: MeasureEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthUserMetricsScheduler::class.java)
        private const val PROJECT_USER_METRICS_LOCK_KEY = "project_user_metrics_lock"
    }

    /**
     * 每天定时刷新项目用户数
     */
    @Scheduled(cron = "0 0 2 * * ? ")
    fun execute() {
        logger.info("execute auth user metrics")
        val redisLock = RedisLock(redisOperation, PROJECT_USER_METRICS_LOCK_KEY, 300L)
        redisLock.use {
            if (redisLock.tryLock()) {
                pushProjectUserMetrics()
            }
        }
    }

    fun pushProjectUserMetrics() {
        var offset = 0
        val limit = 100
        val theDate = LocalDate.now().minusDays(1)
        try {
            do {
                val projectUserCountList = authUserDailyDao.listProjectUserCountDaily(
                    dslContext = dslContext,
                    theDate = theDate,
                    offset = offset,
                    limit = limit
                )
                projectUserCountList.forEach {
                    measureEventDispatcher.dispatch(
                        ProjectUserCountDailyEvent(
                            projectId = it.projectId,
                            userCount = it.userCount,
                            theDate = theDate
                        )
                    )
                }
                offset += limit
            } while (projectUserCountList.size == limit)
        } catch (ignored: Throwable) {
            logger.error("push project user metrics error", ignored)
        }
    }
}
