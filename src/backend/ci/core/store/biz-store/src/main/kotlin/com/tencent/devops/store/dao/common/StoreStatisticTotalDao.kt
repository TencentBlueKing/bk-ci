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

package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.store.pojo.common.KEY_HOT_FLAG
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreStatisticPipelineNumUpdate
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Query
import org.jooq.Record1
import org.jooq.Record7
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class StoreStatisticTotalDao {

    fun initStatisticData(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        downloads: Int? = null,
        comments: Int? = null,
        score: Int? = null,
        scoreAverage: Double? = null,
        recentExecuteNum: Int = 0,
        hotFlag: Boolean? = null
    ) {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            val record = dslContext.newRecord(this)
            record.id = UUIDUtil.generate()
            record.storeCode = storeCode
            record.storeType = storeType
            downloads?.let { record.downloads = downloads }
            comments?.let { record.commits = comments }
            score?.let { record.score = score }
            scoreAverage?.let { record.scoreAverage = scoreAverage.toBigDecimal() }
            record.recentExecuteNum = recentExecuteNum
            hotFlag?.let { record.hotFlag = hotFlag }
            dslContext.insertInto(this).set(record).execute()
        }
    }

    fun updateStatisticData(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        downloads: Int?,
        comments: Int?,
        score: Int?,
        scoreAverage: Double?,
        recentExecuteNum: Int,
        hotFlag: Boolean?
    ) {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            val baseStep = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(RECENT_EXECUTE_NUM, recentExecuteNum)
            downloads?.let { baseStep.set(DOWNLOADS, downloads) }
            comments?.let { baseStep.set(COMMITS, comments) }
            score?.let { baseStep.set(SCORE, score) }
            scoreAverage?.let { baseStep.set(SCORE_AVERAGE, scoreAverage.toBigDecimal()) }
            hotFlag?.let { baseStep.set(HOT_FLAG, hotFlag) }
            baseStep.where(STORE_TYPE.eq(storeType))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }

    fun updateStatisticDataHotFlag(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        hotFlag: Boolean
    ) {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            dslContext.update(this)
                .set(HOT_FLAG, hotFlag)
                .where(STORE_TYPE.eq(storeType).and(STORE_CODE.eq(storeCode)))
                .execute()
        }
    }

    fun batchUpdatePipelineNum(
        dslContext: DSLContext,
        pipelineNumUpdateList: List<StoreStatisticPipelineNumUpdate>,
        storeType: Byte
    ) {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            val list = mutableListOf<Query>()
            pipelineNumUpdateList.forEach { pipelineNumUpdate ->
                val baseStep = dslContext.update(this)
                    .set(UPDATE_TIME, LocalDateTime.now())
                val incrementFlag = pipelineNumUpdate.incrementFlag
                if (incrementFlag != null) {
                    if (incrementFlag) {
                        baseStep.set(PIPELINE_NUM, PIPELINE_NUM + 1)
                    } else {
                        baseStep.set(PIPELINE_NUM, PIPELINE_NUM - 1)
                    }
                }
                val num = pipelineNumUpdate.num
                if (num != null) {
                    baseStep.set(PIPELINE_NUM, num)
                }
                baseStep.where(STORE_CODE.eq(pipelineNumUpdate.storeCode).and(STORE_TYPE.eq(storeType)))
                list.add(baseStep)
            }
            dslContext.batch(list).execute()
        }
    }

    fun deleteStoreStatisticTotal(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }

    fun getStatisticByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ): Record7<Int, Int, BigDecimal, Int, Int, String, Boolean>? {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            return dslContext.select(
                DOWNLOADS,
                COMMITS,
                SCORE_AVERAGE,
                PIPELINE_NUM,
                RECENT_EXECUTE_NUM,
                STORE_CODE,
                HOT_FLAG.`as`(KEY_HOT_FLAG)
            )
                .from(this)
                .where(STORE_TYPE.eq(storeType).and(STORE_CODE.eq(storeCode)))
                .fetchOne()
        }
    }

    /**
     * 批量获取统计数据oo
     */
    fun batchGetStatisticByStoreCode(
        dslContext: DSLContext,
        storeCodeList: List<String?>,
        storeType: Byte
    ): Result<Record7<Int, Int, BigDecimal, Int, Int, String, Boolean>>? {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            val baseStep = dslContext.select(
                DOWNLOADS,
                COMMITS,
                SCORE_AVERAGE,
                PIPELINE_NUM,
                RECENT_EXECUTE_NUM,
                STORE_CODE.`as`(KEY_STORE_CODE),
                HOT_FLAG.`as`(KEY_HOT_FLAG)
            )
                .from(this)

            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType))
            if (storeCodeList.isNotEmpty()) {
                conditions.add(STORE_CODE.`in`(storeCodeList))
            }
            return baseStep.where(conditions).fetch()
        }
    }

    /**
     * 批量获取统计数据oo
     */
    fun batchGetStatisticByStoreCode(
        dslContext: DSLContext,
        storeType: Byte,
        offset: Int,
        limit: Int
    ): List<String> {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            return dslContext.select(STORE_CODE).from(this)
            .where(STORE_TYPE.eq(storeType))
                .groupBy(STORE_CODE)
                .orderBy(CREATE_TIME.desc())
                .limit(limit).offset(offset)
                .fetchInto(String::class.java)
        }
    }

    fun getStatisticList(
        dslContext: DSLContext,
        storeType: Byte,
        timeDescFlag: Boolean = true,
        page: Int,
        pageSize: Int
    ): Result<Record1<String>>? {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            val baseStep = dslContext.select(STORE_CODE)
                .from(this).where(STORE_TYPE.eq(storeType))
            // 因为数据库表的CREATE_TIME字段类型为datetime（精度不够）,当时间间隔很近排序会出问题，故把时间和ID一起排序
            if (timeDescFlag) {
                baseStep.orderBy(CREATE_TIME.desc(), ID)
            } else {
                baseStep.orderBy(CREATE_TIME.asc(), ID)
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun getCountByType(dslContext: DSLContext, storeType: StoreTypeEnum): Int {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            return dslContext.selectCount().from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()).and(RECENT_EXECUTE_NUM.gt(0)))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun getStorePercentileValue(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        index: Int
    ): Record1<Int>? {
        with(TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL) {
            return dslContext.select(RECENT_EXECUTE_NUM)
                .from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()).and(RECENT_EXECUTE_NUM.gt(0)))
                .orderBy(RECENT_EXECUTE_NUM.asc(), CREATE_TIME, STORE_CODE)
                .limit(index - 1, 1).fetchOne()
        }
    }
}
