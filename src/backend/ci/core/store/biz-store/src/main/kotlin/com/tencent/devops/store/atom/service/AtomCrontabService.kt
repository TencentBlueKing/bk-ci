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

package com.tencent.devops.store.atom.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Calendar
import java.util.Date

/**
 * 插件市场定时任务
 * since: 2019-01-24
 */

@Service
class AtomCrontabService @Autowired constructor(
    private val marketAtomStatisticService: MarketAtomStatisticService,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomCrontabService::class.java)
    }

    @Scheduled(cron = "0 10 0 * * ?") // 每天零点十分执行一次
    fun syncAtomDailyStatisticInfo() {
        val format = DateTimeUtil.YYYY_MM_DD
        val endTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = Date(),
            format = format
        )
        val startTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = DateTimeUtil.getFutureDateFromNow(Calendar.DAY_OF_MONTH, -1),
            format = format
        )
        val storeType = StoreTypeEnum.ATOM.type.toByte()
        val lock = RedisLock(redisOperation, "atomDailyStatistic", 36000L)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            marketAtomStatisticService.asyncAtomDailyStatisticInfo(storeType, startTime, endTime)
        } catch (ignored: Throwable) {
            logger.warn("atomDailyStatistic failed", ignored)
        } finally {
            lock.unlock()
        }
    }
}
