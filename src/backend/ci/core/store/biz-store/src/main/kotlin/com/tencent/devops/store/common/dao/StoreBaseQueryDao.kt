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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StoreBaseQueryDao {

    fun getMaxVersionComponentByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .orderBy(
                    JooqUtils.subStr(
                        str = VERSION,
                        delim = ".",
                        count = 1
                    ).plus(0).desc(),
                    JooqUtils.subStr(
                        str = JooqUtils.subStr(
                            str = VERSION,
                            delim = ".",
                            count = -2
                        ),
                        delim = ".",
                        count = 1
                    ).plus(0).desc(),
                    JooqUtils.subStr(
                        str = VERSION,
                        delim = ".",
                        count = -1
                    ).plus(0).desc()
                )
                .limit(1)
                .fetchOne()
        }
    }

    fun getNewestComponentByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getLatestAtomByCode(dslContext: DSLContext, storeCode: String): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getComponentById(
        dslContext: DSLContext,
        storeId: String
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(ID.eq(storeId))
                .fetchOne()
        }
    }

    fun getComponent(
        dslContext: DSLContext,
        storeCode: String,
        version: String,
        storeType: StoreTypeEnum
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun countByCondition(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        name: String? = null,
        storeCode: String? = null
    ): Int {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            if (name.isNullOrBlank()) {
                conditions.add(NAME.eq(name))
            }
            if (storeCode.isNullOrBlank()) {
                conditions.add(STORE_CODE.eq(storeCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun updateComponentBaseInfo(
        dslContext: DSLContext,
        userId: String,
        storeIds: List<String>,
        classifyId: String?,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ) {
        with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.update(this)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            classifyId?.let {
                baseStep.set(CLASSIFY_ID, it)
            }
            storeBaseInfoUpdateRequest.summary?.let {
                baseStep.set(SUMMARY, it)
            }
            storeBaseInfoUpdateRequest.description?.let {
                baseStep.set(DESCRIPTION, it)
            }
            storeBaseInfoUpdateRequest.logoUrl?.let {
                baseStep.set(LOGO_URL, it)
            }
            storeBaseInfoUpdateRequest.publisher?.let {
                baseStep.set(PUBLISHER, it)
            }
            if (!storeBaseInfoUpdateRequest.name.isNullOrBlank()) {
                baseStep.set(NAME, storeBaseInfoUpdateRequest.name)
            }
            baseStep.where(ID.`in`(storeIds)).execute()
        }
    }

    fun countReleaseStoreByCode(dslContext: DSLContext, storeCode: String, version: String? = null): Int {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()

        }
    }
}
