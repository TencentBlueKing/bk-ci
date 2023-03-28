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
import com.tencent.devops.model.store.tables.records.TStoreHonorInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreHonorRelRecord
import com.tencent.devops.store.pojo.common.STORE_HONOR_ID
import com.tencent.devops.store.pojo.common.StoreHonorRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record10
import org.jooq.Record5
import org.jooq.Record6
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreHonorDao {

    fun count(
        dslContext: DSLContext,
        keyWords: String?
    ): Long {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            val condition = mutableListOf<Condition>()
            if (!keyWords.isNullOrBlank()) {
                condition.add(
                    (HONOR_TITLE.eq(keyWords))
                        .or(HONOR_NAME.eq(keyWords))
                        .or(tStoreHonorRel.STORE_NAME.eq(keyWords))
                        .or(tStoreHonorRel.STORE_CODE.eq(keyWords))
                )
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

    fun countByhonorTitle(
        dslContext: DSLContext,
        honorTitle: String
    ): Int {
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(HONOR_TITLE.eq(honorTitle))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun list(
        dslContext: DSLContext,
        keyWords: String?,
        page: Int,
        pageSize: Int
    ): Result<Record10<String, String, String, String, String, Byte, String, String, LocalDateTime, LocalDateTime>> {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            val condition = mutableListOf<Condition>()
            if (!keyWords.isNullOrBlank()) {
                condition.add(
                    (HONOR_TITLE.eq(keyWords))
                        .or(HONOR_NAME.eq(keyWords))
                        .or(tStoreHonorRel.STORE_NAME.eq(keyWords))
                        .or(tStoreHonorRel.STORE_CODE.eq(keyWords))
                )
            }
            return dslContext.select(
                tStoreHonorRel.STORE_NAME,
                tStoreHonorRel.STORE_CODE,
                tStoreHonorRel.HONOR_ID,
                HONOR_TITLE,
                HONOR_NAME,
                STORE_TYPE,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            ).from(this)
                .join(tStoreHonorRel)
                .on(ID.eq(tStoreHonorRel.HONOR_ID).and(STORE_TYPE.eq(tStoreHonorRel.STORE_TYPE)))
                .where(condition)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
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

    fun createStoreHonorInfo(dslContext: DSLContext, userId: String, storeHonorInfo: TStoreHonorInfoRecord) {
        dslContext.executeInsert(storeHonorInfo)
    }

    fun getHonorInfosByStoreCodes(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCodes: List<String>
    ): Result<Record6<String, String, String, String, Boolean, LocalDateTime>> {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {

            return dslContext.select(
                ID.`as`(STORE_HONOR_ID),
                tStoreHonorRel.STORE_CODE,
                HONOR_TITLE,
                HONOR_NAME,
                tStoreHonorRel.MOUNT_FLAG,
                tStoreHonorRel.CREATE_TIME
            )
                .from(this)
                .join(tStoreHonorRel)
                .on(ID.eq(tStoreHonorRel.HONOR_ID).and(STORE_TYPE.eq(tStoreHonorRel.STORE_TYPE)))
                .where(STORE_TYPE.eq(storeType.type.toByte()).and(tStoreHonorRel.STORE_CODE.`in`(storeCodes)))
                .fetch()
        }
    }

    fun getHonorByStoreCode(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<Record5<String, String, String, Boolean, LocalDateTime>> {
        val tStoreHonorRel = TStoreHonorRel.T_STORE_HONOR_REL
        with(TStoreHonorInfo.T_STORE_HONOR_INFO) {
            return dslContext.select(
                ID.`as`(STORE_HONOR_ID),
                HONOR_TITLE,
                HONOR_NAME,
                tStoreHonorRel.MOUNT_FLAG,
                tStoreHonorRel.CREATE_TIME
            ).from(this).leftJoin(tStoreHonorRel)
                .on(ID.eq(tStoreHonorRel.HONOR_ID).and(STORE_TYPE.eq(tStoreHonorRel.STORE_TYPE)))
                .where(tStoreHonorRel.STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .fetch()
        }
    }

    fun installStoreHonor(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        honorId: String
    ) {
        with(TStoreHonorRel.T_STORE_HONOR_REL) {
            dslContext.update(this)
                .set(MOUNT_FLAG, false)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(MOUNT_FLAG.eq(true))
                .execute()
            dslContext.update(this)
                .set(MOUNT_FLAG, true)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(HONOR_ID.eq(honorId))
                .execute()
        }
    }
}
