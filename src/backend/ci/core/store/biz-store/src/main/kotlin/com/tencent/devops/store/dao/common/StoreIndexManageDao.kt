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

import com.tencent.devops.model.store.tables.TStoreIndexBaseInfo
import com.tencent.devops.model.store.tables.TStoreIndexLevelInfo
import com.tencent.devops.model.store.tables.TStoreIndexResult
import com.tencent.devops.model.store.tables.records.TStoreIndexBaseInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexLevelInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexResultRecord
import com.tencent.devops.store.pojo.common.StoreIndexBaseInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreIndexManageDao {

    fun createStoreIndexBaseInfo(dslContext: DSLContext, tStoreIndexBaseInfoRecord: TStoreIndexBaseInfoRecord): Int {
        return dslContext.executeInsert(tStoreIndexBaseInfoRecord)
    }

    fun batchCreateStoreIndexLevelInfo(
        dslContext: DSLContext,
        tStoreIndexLevelInfoRecord: List<TStoreIndexLevelInfoRecord>
    ) {
        dslContext.batchInsert(tStoreIndexLevelInfoRecord).execute()
    }

    fun getStoreIndexBaseInfoById(dslContext: DSLContext, indexId: String): TStoreIndexBaseInfoRecord? {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(indexId))
                .fetchOne()
        }
    }

    fun count(dslContext: DSLContext, keyWords: String?): Long {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(INDEX_NAME.like("%$it%"))
            }
            return dslContext.selectCount()
                .from(this)
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun list(dslContext: DSLContext, keyWords: String?, page: Int, pageSize: Int): List<StoreIndexBaseInfo> {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(INDEX_NAME.like("%$it%"))
            }
            return dslContext.select()
                .from(this)
                .where(condition)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetchInto(StoreIndexBaseInfo::class.java)
        }
    }

    fun listByIds(dslContext: DSLContext, indexIds: List<String>): List<StoreIndexBaseInfo> {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            return dslContext.select()
                .from(this)
                .where(ID.`in`(indexIds))
                .fetchInto(StoreIndexBaseInfo::class.java)
        }
    }

    fun deleteTStoreIndexLevelInfo(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexLevelInfo.T_STORE_INDEX_LEVEL_INFO) {
            dslContext.deleteFrom(this)
                .where(INDEX_ID.eq(indexId))
                .execute()
        }
    }

    fun deleteTStoreIndexBaseInfo(dslContext: DSLContext, indexId: String) {
        with(TStoreIndexBaseInfo.T_STORE_INDEX_BASE_INFO) {
            dslContext.deleteFrom(this)
                .where(ID.eq(indexId))
                .execute()
        }
    }

}