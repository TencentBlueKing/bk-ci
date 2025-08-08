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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.common.dao.StoreStatisticDailyDao
import com.tencent.devops.store.common.service.StoreDailyStatisticService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatistic
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatisticRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreDailyStatisticServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeStatisticDailyDao: StoreStatisticDailyDao
) : StoreDailyStatisticService {

    override fun getDailyStatisticListByCode(
        storeCode: String,
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<StoreDailyStatistic>? {
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
                    .toDouble() else null
            val dailyFailRate =
                if (dailySuccessRate != null) String.format("%.2f", 100 - dailySuccessRate).toDouble() else null
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
                    dailyActiveDuration = dailyStatisticRecord.dailyActiveDuration?.toDouble(),
                    statisticsTime = DateTimeUtil.toDateTime(
                        dailyStatisticRecord.statisticsTime,
                        DateTimeUtil.YYYY_MM_DD
                    )
                )
            )
        }
        return storeDailyStatisticList
    }

    override fun updateDailyStatisticInfo(
        storeType: StoreTypeEnum,
        storeCode: String,
        storeDailyStatisticRequest: StoreDailyStatisticRequest
    ): Boolean {
        val storeDailyStatistic = storeStatisticDailyDao.getDailyStatisticByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            statisticsTime = storeDailyStatisticRequest.statisticsTime
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            if (storeDailyStatistic != null) {
                storeStatisticDailyDao.updateDailyStatisticData(
                    dslContext = context,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    storeDailyStatisticRequest = storeDailyStatisticRequest
                )
            } else {
                storeStatisticDailyDao.insertDailyStatisticData(
                    dslContext = context,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte(),
                    storeDailyStatisticRequest = storeDailyStatisticRequest
                )
            }
        }
        return true
    }
}
