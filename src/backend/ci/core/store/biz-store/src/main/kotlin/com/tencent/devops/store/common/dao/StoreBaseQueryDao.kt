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
import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.model.store.tables.TStoreCategoryRel
import com.tencent.devops.model.store.tables.TStoreLabelRel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.ComponentFullQuery
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
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

    fun getLatestComponentByCodes(
        dslContext: DSLContext,
        storeCodes: List<String>,
        storeType: StoreTypeEnum
    ): Result<TStoreBaseRecord>? {
        return with(TStoreBase.T_STORE_BASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.`in`(storeCodes).and(STORE_TYPE.eq(storeType.type.toByte())))
                .and(LATEST_FLAG.eq(true))
                .fetch()
        }
    }

    fun getReleaseComponentsByCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        num: Int? = null
    ): Result<TStoreBaseRecord>? {
        return with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STATUS.eq(StoreStatusEnum.RELEASED.name))
            val baseStep = dslContext.selectFrom(this)
                .where(conditions)
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
        status: StoreStatusEnum? = null,
        version: String? = null
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
            if (version != null) {
                conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
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

    fun countComponents(
        dslContext: DSLContext,
        listComponentsQuery: ComponentFullQuery,
        classifyId: String?,
        categoryIds: List<String>?,
        labelIds: List<String>?
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val baseStep = dslContext.selectCount().from(tStoreBase)
        val conditions = generateListComponentsConditions(
            componentFullQuery = listComponentsQuery,
            classifyId = classifyId,
            categoryIds = categoryIds,
            labelIds = labelIds,
            baseStep = baseStep
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java) ?: 0
    }

    fun listComponents(
        dslContext: DSLContext,
        listComponentsQuery: ComponentFullQuery,
        classifyId: String?,
        categoryIds: List<String>?,
        labelIds: List<String>?
    ): Result<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val sortType = listComponentsQuery.sortType
        val baseStep = dslContext.select(
            tStoreBase.ID,
            tStoreBase.STORE_CODE,
            tStoreBase.NAME,
            tStoreBase.LOGO_URL,
            tStoreBase.VERSION,
            tStoreBase.STATUS,
            tStoreBase.CREATOR,
            tStoreBase.CREATE_TIME,
            tStoreBase.MODIFIER,
            tStoreBase.UPDATE_TIME
        ).from(tStoreBase)
        val conditions = generateListComponentsConditions(
            componentFullQuery = listComponentsQuery,
            classifyId = classifyId,
            categoryIds = categoryIds,
            labelIds = labelIds,
            baseStep = baseStep
        )
        if (null != sortType && sortType != StoreSortTypeEnum.DOWNLOAD_COUNT) {
            baseStep.where(conditions).orderBy(tStoreBase.field(sortType.name))
        } else {
            baseStep.where(conditions)
        }
        return baseStep.limit(
            (listComponentsQuery.page - 1) * listComponentsQuery.pageSize,
            listComponentsQuery.pageSize
        ).fetch()
    }

    private fun generateListComponentsConditions(
        componentFullQuery: ComponentFullQuery,
        classifyId: String?,
        categoryIds: List<String>?,
        labelIds: List<String>?,
        baseStep: SelectJoinStep<out Record>
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
        val storeType = StoreTypeEnum.valueOf(componentFullQuery.storeType)
        val name = componentFullQuery.name
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        componentFullQuery.type?.let {
            conditions.add(tStoreBaseFeature.TYPE.eq(it))
        }
        if (null != name) {
            conditions.add(tStoreBase.NAME.contains(name))
        }
        componentFullQuery.processFlag?.let {
            val processingStatusList = StoreStatusEnum.getProcessingStatusList()
            conditions.add(tStoreBase.STATUS.`in`(processingStatusList))
        }
        classifyId?.let {
            conditions.add(tStoreBase.CLASSIFY_ID.eq(it))
        }
        categoryIds?.let {
            val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
            conditions.add(tStoreCategoryRel.CATEGORY_ID.`in`(it))
            baseStep.leftJoin(tStoreCategoryRel).on(tStoreBase.ID.eq(tStoreCategoryRel.STORE_ID))
        }
        labelIds?.let {
            val tStoreLabelRel = TStoreLabelRel.T_STORE_LABEL_REL
            conditions.add(tStoreLabelRel.LABEL_ID.`in`(it))
            baseStep.leftJoin(tStoreLabelRel).on(tStoreBase.ID.eq(tStoreLabelRel.STORE_ID))
        }
        return conditions
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
        val conditions = generateGetMyComponentConditions(
            userId = userId,
            storeName = name,
            storeType = storeType
        )
        val subquery = dslContext.select(
            tStoreBase.STORE_CODE,
            tStoreBase.STORE_TYPE,
            DSL.max(tStoreBase.CREATE_TIME).`as`("max_create_time")
        )
            .from(tStoreBase)
            .join(tStoreMember)
            .on(tStoreBase.STORE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .groupBy(tStoreBase.STORE_CODE)

        val baseStep = dslContext.select(
            tStoreBase.ID,
            tStoreBase.STORE_CODE,
            tStoreBase.NAME,
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
            .join(subquery)
            .on(tStoreBase.STORE_CODE.eq(subquery.field(tStoreBase.STORE_CODE)))
            .and(tStoreBase.STORE_TYPE.eq(subquery.field(tStoreBase.STORE_TYPE)))
            .and(tStoreBase.CREATE_TIME.eq(subquery.field("max_create_time", LocalDateTime::class.java)))
            .where(conditions)
            .orderBy(tStoreBase.UPDATE_TIME.desc())
        return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
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
            userId = userId,
            storeName = name,
            storeType = storeType
        )
        return dslContext.select(DSL.countDistinct(tStoreBase.STORE_CODE))
            .from(tStoreBase)
            .join(tStoreMember)
            .on(tStoreBase.STORE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java) ?: 0
    }

    private fun generateGetMyComponentConditions(
        userId: String,
        storeType: StoreTypeEnum,
        storeName: String?
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        val statusList = StoreStatusEnum.getAll().toMutableList()
        statusList.removeAll(StoreStatusEnum.getProcessingStatusList())
        statusList.add(StoreStatusEnum.INIT.name)
        conditions.add(tStoreBase.STATUS.`in`(statusList))
        if (null != storeName) {
            conditions.add(tStoreBase.NAME.contains(storeName))
        }
        conditions.add(tStoreMember.STORE_TYPE.eq(storeType.type.toByte()))
        conditions.add(tStoreMember.USERNAME.eq(userId))
        return conditions
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

    fun getStoreBaseInfoByConditions(
        dslContext: DSLContext,
        storeCodeList: List<String>,
        storeType: StoreTypeEnum,
        storeStatusList: List<String>? = null
    ): Result<TStoreBaseRecord> {
        with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.`in`(storeCodeList))
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            if (storeStatusList != null) {
                conditions.add(STATUS.`in`(storeStatusList))
            }
            return dslContext.selectFrom(this)
                .where(conditions).orderBy(CREATE_TIME.desc()).fetch()
        }
    }
}
