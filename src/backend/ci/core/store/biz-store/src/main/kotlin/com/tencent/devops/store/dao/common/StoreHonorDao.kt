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

import com.tencent.devops.model.store.tables.TStoreHonorInfo
import com.tencent.devops.model.store.tables.TStoreHonorRel
import com.tencent.devops.model.store.tables.records.TStoreHonorRelRecord
import com.tencent.devops.store.pojo.common.StoreHonorInfo
import com.tencent.devops.store.pojo.common.StoreHonorRel
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StoreHonorDao {

    fun count(
        dslContext: DSLContext,
        keyWords: String?,
        page: Int,
        pageSize: Int
    ): Long {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(HONOR_TITLE.like("%$it%"))
                condition.add(HONOR_NAME.like("%$it%"))
                condition.add(STORE_TYPE.like("%$it%"))
                condition.add(tStoreHonorRel.STORE_CODE.like("%$it%"))
            }
            return dslContext.selectCount()
                .from(this)
                .join(tStoreHonorRel)
                .on(ID.eq(tStoreHonorRel.HONOR_ID).and(STORE_TYPE.eq(tStoreHonorRel.STORE_TYPE)))
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0
        }
    }

    fun getByIds(
        dslContext: DSLContext,
        honorIds: List<String>
    ): List<String> {
        with(TStoreHonorRel.T_STORE_HONOR_REL) {
            return dslContext.select(HONOR_ID)
                .from(this)
                .where(HONOR_ID.`in`(honorIds))
                .fetchInto(String::class.java)
        }
    }

    fun list(
        dslContext: DSLContext,
        keyWords: String?,
        page: Int,
        pageSize: Int
    ): List<StoreHonorInfo> {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            val condition = mutableListOf<Condition>()
            keyWords?.let {
                condition.add(HONOR_TITLE.like("%$it%"))
                condition.add(HONOR_NAME.like("%$it%"))
                condition.add(STORE_TYPE.like("%$it%"))
                condition.add(tStoreHonorRel.STORE_CODE.like("%$it%"))
            }
            return dslContext.select().from(this)
                .join(tStoreHonorRel)
                .on(ID.eq(tStoreHonorRel.HONOR_ID).and(STORE_TYPE.eq(tStoreHonorRel.STORE_TYPE)))
                .where(condition)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetchInto(StoreHonorInfo::class.java)
        }
    }

    fun batchDeleteStoreHonorRel(dslContext: DSLContext, storeHonorRelList: List<StoreHonorRel>) {
        dslContext.batch(storeHonorRelList.map {
            with(TStoreHonorRel.T_STORE_HONOR_REL) {
                dslContext.deleteFrom(this)
                    .where(STORE_CODE.eq(it.storeCode))
                    .and(STORE_TYPE.eq(it.storeType.type.toByte()))
                    .and(HONOR_ID.eq(it.honorId))
            }
        }).execute()
    }

    fun batchDeleteStoreHonorInfo(dslContext: DSLContext, storeHonorInfoIds: List<String>) {
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(storeHonorInfoIds))
                .execute()
        }
    }

    fun batchCreateStoreHonorRel(
        dslContext: DSLContext,
        tStoreHonorRelRecords: List<TStoreHonorRelRecord>
    ) {
        dslContext.batchInsert(tStoreHonorRelRecords).execute()
    }

    fun addStoreHonorInfo(dslContext: DSLContext, userId: String, storeHonorInfo: StoreHonorInfo) {
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            dslContext.insertInto(
                this,
                ID,
                HONOR_TITLE,
                HONOR_NAME,
                STORE_TYPE,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                storeHonorInfo.id,
                storeHonorInfo.honorTitle,
                storeHonorInfo.honorName,
                storeHonorInfo.storeType.type.toByte(),
                storeHonorInfo.creator,
                storeHonorInfo.modifier,
                storeHonorInfo.createTime,
                storeHonorInfo.updateTime
            ).execute()
        }
    }
}