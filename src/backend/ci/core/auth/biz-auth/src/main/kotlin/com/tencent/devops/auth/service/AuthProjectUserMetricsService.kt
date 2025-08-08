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
 *
 */

package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.measure.ProjectUserDailyEvent
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsEvent
import com.tencent.devops.common.event.pojo.measure.UserOperateCounterData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
@Suppress("UnstableApiUsage")
class AuthProjectUserMetricsService @Autowired constructor(
    private val measureEventDispatcher: SampleEventDispatcher
) {
    private val userOperateCounterData = UserOperateCounterData()

    companion object {
        private val logger = LoggerFactory.getLogger(AuthProjectUserMetricsService::class.java)

        // 期待的用户数10w
        private const val EXPECTED_USER_COUNT = 100000

        // 错误率0.1%
        private const val EXPECTED_FPP = 0.001
        private val bloomFilterMap = CacheBuilder.newBuilder()
            .maximumSize(2)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<LocalDate, BloomFilter<String>>()

        private val executorService = Executors.newFixedThreadPool(5)
    }

    fun save(
        projectId: String,
        userId: String,
        operate: String
    ) {
        executorService.execute {
            val theDate = LocalDate.now()
            try {
                val bloomKey = "${projectId}_$userId"
                val bloomFilter = getBloomFilter(theDate)
                if (!bloomFilter.mightContain(bloomKey)) {
                    measureEventDispatcher.dispatch(
                        ProjectUserDailyEvent(
                            projectId = projectId,
                            userId = userId,
                            theDate = theDate
                        )
                    )
                    bloomFilter.put(bloomKey)
                }
                saveProjectUserOperateMetrics(
                    projectId = projectId,
                    userId = userId,
                    operate = operate,
                    theDate = theDate
                )
            } catch (ignored: Throwable) {
                logger.error("save auth user error", ignored)
            }
        }
    }

    private fun getBloomFilter(theDate: LocalDate): BloomFilter<String> {
        var bloomFilter = bloomFilterMap.getIfPresent(theDate)
        if (bloomFilter == null) {
            bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_USER_COUNT,
                EXPECTED_FPP
            )
            bloomFilterMap.put(theDate, bloomFilter)
        }
        return bloomFilter!!
    }

    private fun saveProjectUserOperateMetrics(
        projectId: String,
        userId: String,
        operate: String,
        theDate: LocalDate
    ) {
        val projectUserOperateMetricsKey = ProjectUserOperateMetricsData(
            projectId = projectId,
            userId = userId,
            theDate = theDate,
            operate = operate
        ).getProjectUserOperateMetricsKey()
        userOperateCounterData.increment(projectUserOperateMetricsKey)
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 20000)
    private fun uploadProjectUserOperateMetrics() {
        if (logger.isDebugEnabled) {
            logger.debug("upload project user operate metrics :$userOperateCounterData")
        }
        measureEventDispatcher.dispatch(
            ProjectUserOperateMetricsEvent(
                userOperateCounterData = userOperateCounterData
            )
        )
        userOperateCounterData.reset()
    }
}
