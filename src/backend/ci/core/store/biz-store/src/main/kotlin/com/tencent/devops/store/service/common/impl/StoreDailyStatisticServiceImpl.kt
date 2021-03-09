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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.pojo.common.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.StoreDailyStatisticRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDailyStatisticService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.Executors

@Service
class StoreDailyStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao
) : StoreDailyStatisticService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreDailyStatisticServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 50
    }

    override fun getDailyStatisticListByCode(
        userId: String,
        storeCode: String,
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<StoreDailyStatistic>? {
        logger.info("getDailyStatisticListByCode $userId,$storeCode,$storeType,$startTime,$endTime")
        val dailyStatisticRecordList = storeStatisticDailyDao.getDailyStatisticListByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            startTime = startTime,
            endTime = endTime
        )
        val storeDailyStatisticList = mutableListOf<StoreDailyStatistic>()
        dailyStatisticRecordList?.forEach { dailyStatisticRecord ->
            val dailySuccessNum = dailyStatisticRecord.dailySuccessNum
            val dailyFailNum = dailyStatisticRecord.dailyFailNum
            // 计算插件运行成功和失败比率
            val totalNum = dailySuccessNum + dailyFailNum
            val dailySuccessRate =
                if (totalNum > 0) String.format("%.2f", dailySuccessNum.toDouble() * 100 / totalNum)
                    .toDouble() else 100.00
            val dailyFailRate = 100 - dailySuccessRate
            storeDailyStatisticList.add(
                StoreDailyStatistic(
                    totalDownloads = dailyStatisticRecord.totalDownloads,
                    dailyDownloads = dailyStatisticRecord.dailyDownloads,
                    dailySuccessNum = dailySuccessNum,
                    dailySuccessRate = dailySuccessRate,
                    dailyFailNum = dailyFailNum,
                    dailyFailRate = dailyFailRate,
                    dailyFailDetail = if (dailyStatisticRecord.dailyFailDetail != null) JsonUtil.toMap(
                        dailyStatisticRecord.dailyFailDetail!!
                    ) else null,
                    statisticsTime = DateTimeUtil.toDateTime(dailyStatisticRecord.statisticsTime)
                )
            )
        }
        return storeDailyStatisticList
    }

    override fun asyncUpdateDailyDownloads(date: String): Boolean {
        Executors.newFixedThreadPool(1).submit {
            val statisticsTime = DateTimeUtil.stringToLocalDateTime(date, "yyyy-MM-dd")
            logger.info("begin asyncUpdateDailyDownloads!!")
            StoreTypeEnum.values().forEach { storeType ->
                updateDailyDownloads(storeType, statisticsTime)
            }
            logger.info("end asyncUpdateDailyDownloads!!")
        }
        return true
    }

    private fun updateDailyDownloads(
        storeType: StoreTypeEnum,
        statisticsTime: LocalDateTime
    ) {
        var offset = 0
        do {
            val dailyStatistics = storeStatisticDailyDao.getDailyStatisticList(
                dslContext = dslContext,
                storeType = storeType.type.toByte(),
                statisticsTime = statisticsTime,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE,
                timeDescFlag = false
            )
            // 更新流水线任务表的插件版本
            dailyStatistics?.forEach { dailyStatistic ->
                val storeCode = dailyStatistic.storeCode
                val endTime = DateTimeUtil.convertDateToLocalDateTime(
                    DateTimeUtil.getFutureDate(
                        localDateTime = statisticsTime,
                        unit = Calendar.DAY_OF_MONTH,
                        timeSpan = 1
                    )
                )
                // 统计到当天为止组件的安装量
                val totalDownloads = storeProjectRelDao.countInstallNumByCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    endTime = endTime
                )
                // 统计当天组件的安装量
                val dailyDownloads = storeProjectRelDao.countInstallNumByCode(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    startTime = statisticsTime,
                    endTime = endTime
                )
                val storeDailyStatisticRequest = StoreDailyStatisticRequest(
                    totalDownloads = totalDownloads,
                    dailyDownloads = dailyDownloads
                )
                storeStatisticDailyDao.updateDailyStatisticData(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    storeDailyStatisticRequest = storeDailyStatisticRequest
                )
            }
            offset += DEFAULT_PAGE_SIZE
        } while (dailyStatistics?.size == DEFAULT_PAGE_SIZE)
    }
}
