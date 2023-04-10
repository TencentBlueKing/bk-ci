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
import com.tencent.devops.model.store.tables.TStoreStatistics
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record4
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Suppress("ALL")
@Repository
class StoreStatisticDao {

    /**
     * 批量获取统计数据oo
     */
    fun batchGetStatisticByStoreCode(
        dslContext: DSLContext,
        storeCodeList: List<String?>,
        storeType: Byte,
        limit: Int? = null,
        offset: Int? = null
    ): Result<Record4<BigDecimal, BigDecimal, BigDecimal, String>> {
        with(TStoreStatistics.T_STORE_STATISTICS) {
            val baseStep = dslContext.select(
                DSL.sum(DOWNLOADS),
                DSL.sum(COMMITS),
                DSL.sum(SCORE),
                STORE_CODE
            ).from(this)

            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType))
            if (storeCodeList.isNotEmpty()) {
                conditions.add(STORE_CODE.`in`(storeCodeList))
            }
            val finalStep = baseStep.where(conditions)
                .groupBy(STORE_CODE)
                .orderBy(CREATE_TIME, ID)
            return if (null != offset && null != limit) {
                finalStep.limit(limit).offset(offset).fetch()
            } else {
                finalStep.fetch()
            }
        }
    }

    /**
     * 更新下载量
     */
    fun updateDownloads(
        dslContext: DSLContext,
        userId: String,
        storeId: String,
        storeCode: String,
        storeType: Byte,
        increment: Int
    ) {
        with(TStoreStatistics.T_STORE_STATISTICS) {
            val record = dslContext.selectFrom(this).where(STORE_ID.eq(storeId)).fetchOne()
            if (null == record) {
                dslContext.insertInto(this).columns(
                    ID,
                    STORE_ID,
                    STORE_CODE,
                    DOWNLOADS,
                    COMMITS,
                    SCORE,
                    STORE_TYPE,
                    CREATOR,
                    MODIFIER
                ).values(
                    UUIDUtil.generate(),
                    storeId,
                    storeCode,
                    increment,
                    0,
                    0,
                    storeType,
                    userId,
                    userId
                ).execute()
            } else {
                dslContext.update(this)
                    .set(DOWNLOADS, DOWNLOADS + increment)
                    .where(STORE_ID.eq(storeId).and(STORE_TYPE.eq(storeType)))
                    .execute()
            }
        }
    }

    /**
     * 更新评论信息
     */
    fun updateCommentInfo(
        dslContext: DSLContext,
        userId: String,
        storeId: String,
        storeCode: String,
        storeType: Byte,
        commentIncrement: Int,
        scoreIncrement: Int
    ) {
        with(TStoreStatistics.T_STORE_STATISTICS) {
            val record = dslContext.selectFrom(this).where(STORE_ID.eq(storeId)).fetchOne()
            if (null == record) {
                dslContext.insertInto(this).columns(
                    ID,
                    STORE_ID,
                    STORE_CODE,
                    DOWNLOADS,
                    COMMITS,
                    SCORE,
                    STORE_TYPE,
                    CREATOR,
                    MODIFIER
                ).values(
                    UUIDUtil.generate(),
                    storeId,
                    storeCode,
                    0,
                    commentIncrement,
                    scoreIncrement,
                    storeType,
                    userId,
                    userId
                ).execute()
            } else {
                dslContext.update(this)
                    .set(COMMITS, COMMITS + commentIncrement)
                    .set(SCORE, SCORE + scoreIncrement)
                    .where(STORE_ID.eq(storeId).and(STORE_TYPE.eq(storeType)))
                    .execute()
            }
        }
    }

    fun deleteStoreStatistic(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreStatistics.T_STORE_STATISTICS) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }
}
