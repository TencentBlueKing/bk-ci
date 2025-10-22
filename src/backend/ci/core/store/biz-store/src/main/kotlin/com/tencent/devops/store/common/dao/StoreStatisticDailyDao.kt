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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.model.store.tables.TStoreStatisticsDaily
import com.tencent.devops.model.store.tables.records.TStoreStatisticsDailyRecord
import com.tencent.devops.store.pojo.common.BK_SUM_DAILY_FAIL_NUM
import com.tencent.devops.store.pojo.common.BK_SUM_DAILY_SUCCESS_NUM
import com.tencent.devops.store.pojo.common.statistic.StoreDailyStatisticRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class StoreStatisticDailyDao {

    fun insertDailyStatisticData(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        storeDailyStatisticRequest: StoreDailyStatisticRequest
    ) {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            dslContext.insertInto(this).columns(
                ID,
                STORE_CODE,
                STORE_TYPE,
                TOTAL_DOWNLOADS,
                DAILY_DOWNLOADS,
                DAILY_SUCCESS_NUM,
                DAILY_FAIL_NUM,
                DAILY_FAIL_DETAIL,
                DAILY_ACTIVE_DURATION,
                STATISTICS_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                UUIDUtil.generate(),
                storeCode,
                storeType,
                storeDailyStatisticRequest.totalDownloads ?: 0,
                storeDailyStatisticRequest.dailyDownloads ?: 0,
                storeDailyStatisticRequest.dailySuccessNum ?: 0,
                storeDailyStatisticRequest.dailyFailNum ?: 0,
                if (storeDailyStatisticRequest.dailyFailDetail != null)
                    JsonUtil.toJson(storeDailyStatisticRequest.dailyFailDetail!!)
                else null,
                storeDailyStatisticRequest.dailyActiveDuration?.toBigDecimal(),
                storeDailyStatisticRequest.statisticsTime,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
                .execute()
        }
    }

    fun updateDailyStatisticData(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        storeDailyStatisticRequest: StoreDailyStatisticRequest
    ) {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            val baseStep = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
            storeDailyStatisticRequest.totalDownloads?.let {
                baseStep.set(TOTAL_DOWNLOADS, it)
            }
            storeDailyStatisticRequest.dailyDownloads?.let {
                baseStep.set(DAILY_DOWNLOADS, it)
            }
            storeDailyStatisticRequest.dailySuccessNum?.let {
                baseStep.set(DAILY_SUCCESS_NUM, it)
            }
            storeDailyStatisticRequest.dailyFailNum?.let {
                baseStep.set(DAILY_FAIL_NUM, it)
            }
            storeDailyStatisticRequest.dailyFailDetail?.let {
                baseStep.set(DAILY_FAIL_DETAIL, JsonUtil.toJson(it))
            }
            storeDailyStatisticRequest.dailyActiveDuration?.let {
                baseStep.set(DAILY_ACTIVE_DURATION, it.toBigDecimal())
            }
            baseStep.where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .and(STATISTICS_TIME.eq(storeDailyStatisticRequest.statisticsTime))
                .execute()
        }
    }

    fun deleteDailyStatisticData(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }

    fun getDailyStatisticByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        statisticsTime: LocalDateTime
    ): TStoreStatisticsDailyRecord? {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            return dslContext.selectFrom(this).where(conditions).fetchOne()
        }
    }

    fun getDailyStatisticListByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Result<TStoreStatisticsDailyRecord>? {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(STATISTICS_TIME.ge(startTime))
            conditions.add(STATISTICS_TIME.le(endTime))
            return dslContext.selectFrom(this).where(conditions).orderBy(STATISTICS_TIME.asc()).fetch()
        }
    }

    fun getStoreExecuteCountByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Record2<BigDecimal, BigDecimal>? {
        with(TStoreStatisticsDaily.T_STORE_STATISTICS_DAILY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            conditions.add(STATISTICS_TIME.ge(startTime))
            conditions.add(STATISTICS_TIME.le(endTime))
            return dslContext.select(
                sum(DAILY_SUCCESS_NUM).`as`(BK_SUM_DAILY_SUCCESS_NUM),
                sum(DAILY_FAIL_NUM).`as`(BK_SUM_DAILY_FAIL_NUM)
            )
                .from(this)
                .where(conditions).fetchOne()
        }
    }
}
