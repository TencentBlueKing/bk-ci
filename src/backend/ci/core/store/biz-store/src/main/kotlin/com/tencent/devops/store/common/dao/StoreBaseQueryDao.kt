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

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.KEY_PIPELINE_ID
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.model.store.tables.TStoreCategoryRel
import com.tencent.devops.model.store.tables.TStoreLabelRel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStorePipelineRel
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.QueryComponentsParam
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
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

    fun getValidComponentsByCodes(
        dslContext: DSLContext,
        storeCodes: Collection<String>,
        storeType: StoreTypeEnum,
        testComponentFlag: Boolean
    ): Result<out Record> {
        return with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf(STORE_CODE.`in`(storeCodes))
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            if (testComponentFlag) {
                val statusEnumList = listOf(
                    StoreStatusEnum.TESTING,
                    StoreStatusEnum.AUDITING
                )
                conditions.add(STATUS.`in`(statusEnumList))
                val subQuery = dslContext.select(
                    STORE_CODE,
                    STORE_TYPE,
                    DSL.max(CREATE_TIME).`as`(KEY_CREATE_TIME)
                ).from(this)
                    .where(conditions)
                    .groupBy(STORE_CODE)
                dslContext.select().from(this)
                    .join(subQuery)
                    .on(
                        STORE_CODE.eq(subQuery.field(STORE_CODE))
                            .and(STORE_TYPE.eq(subQuery.field(STORE_TYPE)))
                            .and(CREATE_TIME.eq(subQuery.field(KEY_CREATE_TIME, LocalDateTime::class.java)))
                    )
                    .where(conditions)
                    .fetch()
            } else {
                conditions.add(LATEST_FLAG.eq(true))
                dslContext.selectFrom(this)
                    .where(conditions)
                    .fetch()
            }
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

    fun getComponentId(
        dslContext: DSLContext,
        storeCode: String,
        version: String,
        storeType: StoreTypeEnum
    ): String? {
        return with(TStoreBase.T_STORE_BASE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(VERSION.like(VersionUtils.generateQueryVersion(version)))
            dslContext.select(ID).from(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
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

    fun getComponentIds(dslContext: DSLContext, storeCode: String, storeType: Byte): MutableList<String> {
        with(TStoreBase.T_STORE_BASE) {
            return dslContext.select(ID)
                .from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .fetchInto(String::class.java)
        }
    }

    fun countComponents(
        dslContext: DSLContext,
        queryComponentsParam: QueryComponentsParam,
        classifyId: String?,
        categoryIds: List<String>?,
        labelIds: List<String>?
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val baseStep = dslContext.selectCount().from(tStoreBase)
        val conditions = generateListComponentsConditions(
            dslContext = dslContext,
            queryComponentsParam = queryComponentsParam,
            categoryIds = categoryIds,
            labelIds = labelIds,
            baseStep = baseStep
        )
        classifyId?.let {
            conditions.add(tStoreBase.CLASSIFY_ID.eq(it))
        }
        return baseStep.where(conditions).fetchOne(0, Int::class.java) ?: 0
    }

    fun listComponents(
        dslContext: DSLContext,
        queryComponentsParam: QueryComponentsParam,
        classifyId: String?,
        categoryIds: List<String>?,
        labelIds: List<String>?
    ): Result<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val sortType = queryComponentsParam.sortType
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
            dslContext = dslContext,
            queryComponentsParam = queryComponentsParam,
            categoryIds = categoryIds,
            labelIds = labelIds,
            baseStep = baseStep
        )
        classifyId?.let {
            conditions.add(tStoreBase.CLASSIFY_ID.eq(it))
        }
        if (null != sortType && sortType != StoreSortTypeEnum.DOWNLOAD_COUNT) {
            baseStep.where(conditions).orderBy(tStoreBase.field(sortType.name)!!.desc())
        } else {
            baseStep.where(conditions)
        }
        return baseStep
            .limit(
                (queryComponentsParam.page - 1) * queryComponentsParam.pageSize,
                queryComponentsParam.pageSize
            ).fetch()
    }

    fun getProcessingComponents(
        dslContext: DSLContext,
        conditions: List<Condition>,
        tStoreBase: TStoreBase
    ): SelectConditionStep<Record1<String>> {
        val subQueryCondition = mutableListOf<Condition>()
        val processingStatusList = StoreStatusEnum.getProcessingStatusList()
        subQueryCondition.addAll(conditions)
        subQueryCondition.add(tStoreBase.STATUS.`in`(processingStatusList))
        return dslContext.selectDistinct(tStoreBase.STORE_CODE).from(tStoreBase).where(subQueryCondition)
    }

    private fun generateListComponentsConditions(
        dslContext: DSLContext,
        queryComponentsParam: QueryComponentsParam,
        categoryIds: List<String>?,
        labelIds: List<String>?,
        baseStep: SelectJoinStep<out Record>
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
        val storeType = StoreTypeEnum.valueOf(queryComponentsParam.storeType)
        val name = queryComponentsParam.name
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        queryComponentsParam.type?.let {
            conditions.add(tStoreBaseFeature.TYPE.eq(it))
        }
        if (null != name) {
            conditions.add(tStoreBase.NAME.contains(name))
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
        val processingComponents = getProcessingComponents(
            dslContext = dslContext,
            conditions = conditions,
            tStoreBase = tStoreBase
        )
        if (queryComponentsParam.processFlag == true) {
            conditions.add(
                tStoreBase.STORE_CODE.`in`(processingComponents)
            )
        }
        if (queryComponentsParam.processFlag == false) {
            conditions.add(
                tStoreBase.STORE_CODE.notIn(processingComponents)
            )
        }
        conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        return conditions
    }

    fun getMyComponents(
        dslContext: DSLContext,
        conditions: List<Condition>,
        page: Int,
        pageSize: Int
    ): Result<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val subQuery = dslContext.select(
            tStoreBase.STORE_CODE,
            tStoreBase.STORE_TYPE,
            DSL.max(tStoreBase.CREATE_TIME).`as`(KEY_CREATE_TIME)
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
            .join(subQuery)
            .on(tStoreBase.STORE_CODE.eq(subQuery.field(tStoreBase.STORE_CODE)))
            .and(tStoreBase.STORE_TYPE.eq(subQuery.field(tStoreBase.STORE_TYPE)))
            .and(tStoreBase.CREATE_TIME.eq(subQuery.field(KEY_CREATE_TIME, LocalDateTime::class.java)))
            .where(conditions)
            .orderBy(tStoreBase.UPDATE_TIME.desc())
        return baseStep.limit((page - 1) * pageSize, pageSize).skipCheck().fetch()
    }

    fun countMyComponents(
        dslContext: DSLContext,
        conditions: List<Condition>
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        return dslContext.select(DSL.countDistinct(tStoreBase.STORE_CODE))
            .from(tStoreBase)
            .join(tStoreMember)
            .on(tStoreBase.STORE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun generateGetMyComponentConditions(
        userId: String,
        storeType: StoreTypeEnum,
        storeName: String?
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        val statusList = StoreStatusEnum.values().map { it.name }.toMutableList()
        statusList.removeAll(StoreStatusEnum.getProcessingStatusList())
        statusList.add(StoreStatusEnum.INIT.name)
        conditions.add(
            tStoreBase.STATUS.`in`(statusList)
                .or(tStoreBase.LATEST_FLAG.eq(true).and(tStoreBase.VERSION.eq(INIT_VERSION)))
        )
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

    fun getLatestStoreInfoListByCodes(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCodeList: List<String>
    ): Result<out Record>? {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStorePipelineRel = TStorePipelineRel.T_STORE_PIPELINE_REL
        return dslContext.select(
            tStoreBase.STORE_CODE.`as`(KEY_STORE_CODE),
            tStoreBase.VERSION.`as`(KEY_VERSION),
            tStoreBase.CREATOR.`as`(KEY_CREATOR),
            tStorePipelineRel.PIPELINE_ID.`as`(KEY_PIPELINE_ID)
        ).from(tStoreBase)
            .join(tStorePipelineRel)
            .on(
                tStoreBase.STORE_CODE.eq(tStorePipelineRel.STORE_CODE)
                    .and(tStoreBase.STORE_TYPE.eq(tStorePipelineRel.STORE_TYPE))
            )
            .where(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
            .and(tStoreBase.LATEST_FLAG.eq(true))
            .and(tStoreBase.STORE_CODE.`in`(storeCodeList))
            .fetch()
    }
}
