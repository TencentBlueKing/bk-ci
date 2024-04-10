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
import com.tencent.devops.model.store.tables.TStoreBaseEnv
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
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
        storeType: StoreTypeEnum,
        status: StoreStatusEnum? = null
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(STORE_CODE.eq(storeCode))
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getLatestComponentByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum
    ): TStoreBaseRecord? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getReleaseComponentsByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        num: Int? = null
    ): Result<TStoreBaseRecord>? {
        return with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .orderBy(CREATE_TIME.desc())
            if (null != num) {
                baseStep.limit(num)
            }
            baseStep.fetch()
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
        storeCode: String? = null,
        status: StoreStatusEnum? = null
    ): Int {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            if (!name.isNullOrBlank()) {
                conditions.add(NAME.eq(name))
            }
            if (!storeCode.isNullOrBlank()) {
                conditions.add(STORE_CODE.eq(storeCode))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun countReleaseStoreByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeTepe: StoreTypeEnum,
        version: String? = null
    ): Int {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeTepe.type.toByte()))
            if (version != null) {
                conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getComponentIds(dslContext: DSLContext, storeCode: String, storeType: StoreTypeEnum): MutableList<String> {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.select(ID)
                .from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchInto(String::class.java)
        }
    }

    fun getMyComponents(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        name: String?,
        page: Int,
        pageSize: Int
    ): Result<out Record>? {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val tStoreBaseEnv = TStoreBaseEnv.T_STORE_BASE_ENV
        val conditions = generateGetMyComponentConditions(
            tStoreBase = tStoreBase,
            userId = userId,
            tStoreMember = tStoreMember,
            storeName = name,
            storeType = storeType
        )
        val baseStep = dslContext.select(
            tStoreBase.ID,
            tStoreBase.STORE_CODE,
            tStoreBase.NAME,
            tStoreBaseEnv.LANGUAGE.`as`(KEY_LANGUAGE),
            tStoreBase.LOGO_URL,
            tStoreBase.VERSION,
            tStoreBase.STATUS,
            tStoreBase.CREATOR,
            tStoreBase.CREATE_TIME,
            tStoreBase.MODIFIER,
            tStoreBase.UPDATE_TIME
        )
            .from(tStoreBase)
            .join(tStoreMember)
            .on(tStoreBase.STORE_CODE.eq(tStoreMember.STORE_CODE))
            .leftJoin(tStoreBaseEnv)
            .on(tStoreBase.ID.eq(tStoreBaseEnv.STORE_ID))
            .where(conditions)
            .groupBy(tStoreBase.ID)
            .orderBy(tStoreBase.UPDATE_TIME.desc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun countMyComponents(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        name: String?
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = generateGetMyComponentConditions(
            tStoreBase = tStoreBase,
            userId = userId,
            tStoreMember = tStoreMember,
            storeName = name,
            storeType = storeType
        )
        return dslContext.selectCount()
            .from(tStoreBase)
            .join(tStoreMember)
            .on(tStoreBase.STORE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .groupBy(tStoreBase.ID)
            .orderBy(tStoreBase.UPDATE_TIME.desc())
            .limit(1)
            .fetchOne(0, Int::class.java) ?: 0
    }

    private fun generateGetMyComponentConditions(
        userId: String,
        tStoreBase: TStoreBase,
        tStoreMember: TStoreMember,
        storeType: StoreTypeEnum,
        storeName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        conditions.add(tStoreMember.USERNAME.eq(userId))
        conditions.add(tStoreMember.STORE_TYPE.eq(storeType.type.toByte()))
        if (null != storeName) {
            conditions.add(tStoreBase.NAME.contains(storeName))
        }
        return conditions
    }

    fun countReleaseComponentByCode(dslContext: DSLContext, storeCode: String, version: String? = null): Int {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STATUS.eq(StoreStatusEnum.RELEASED.name))
            if (version != null) {
                conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, storeCode: String, storeType: StoreTypeEnum): Int {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.selectCount().from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType.type.toByte())))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getComponentsByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TStoreBaseRecord> {
        return with(TStoreBase.T_STORE_BASE) {
            val baseStep = dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .orderBy(CREATE_TIME.desc())
            if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }
}
