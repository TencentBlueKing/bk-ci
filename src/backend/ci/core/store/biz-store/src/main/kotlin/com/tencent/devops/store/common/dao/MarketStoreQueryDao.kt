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

import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.model.store.tables.TStoreCategoryRel
import com.tencent.devops.model.store.tables.TStoreLabelRel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class MarketStoreQueryDao {

    /**
     * 研发商店搜索结果，总数
     */
    fun count(
        dslContext: DSLContext,
        storeInfoQuery: StoreInfoQuery
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val baseStep = dslContext.select(DSL.countDistinct(tStoreBase.STORE_CODE)).from(tStoreBase)
        val conditions = formatConditions(
            dslContext = dslContext,
            storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType),
            storeInfoQuery = storeInfoQuery,
            baseStep = baseStep
        )
        return baseStep.where(conditions).fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 研发商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        storeInfoQuery: StoreInfoQuery
    ): Result<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val baseStep = createBaseStep(dslContext)
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        val keyword = storeInfoQuery.keyword
        val sortType = storeInfoQuery.sortType
        val conditions = formatConditions(
            dslContext = dslContext,
            storeType = storeType,
            storeInfoQuery = storeInfoQuery,
            baseStep = baseStep
        )
        if (!keyword.isNullOrEmpty()) {
            conditions.add(
                tStoreBase.NAME.contains(keyword).or(tStoreBase.SUMMARY.contains(keyword))
            )
        }

        val subquery = dslContext.select(
            tStoreBase.STORE_CODE,
            tStoreBase.STORE_TYPE,
            DSL.max(tStoreBase.CREATE_TIME).`as`(KEY_CREATE_TIME)
        ).from(tStoreBase)
            .where(conditions)
            .groupBy(tStoreBase.STORE_CODE)

        baseStep.join(subquery)
            .on(tStoreBase.STORE_CODE.eq(subquery.field(tStoreBase.STORE_CODE)))
            .and(tStoreBase.STORE_TYPE.eq(subquery.field(tStoreBase.STORE_TYPE)))
            .and(tStoreBase.CREATE_TIME.eq(subquery.field(KEY_CREATE_TIME, LocalDateTime::class.java)))

        if (null != sortType) {
            val flag = sortType == StoreSortTypeEnum.DOWNLOAD_COUNT
            if (flag && storeInfoQuery.score == null) {
                val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
                val t =
                    dslContext.select(
                        tStoreStatisticsTotal.STORE_CODE,
                        tStoreStatisticsTotal.DOWNLOADS.`as`(StoreSortTypeEnum.DOWNLOAD_COUNT.name)
                    )
                        .from(tStoreStatisticsTotal).where(tStoreStatisticsTotal.STORE_TYPE.eq(storeType.type.toByte())).asTable("t")
                baseStep.leftJoin(t).on(tStoreBase.STORE_CODE.eq(t.field(KEY_STORE_CODE, String::class.java)))
            }

            val realSortType = if (flag) { DSL.field(sortType.name) } else { tStoreBase.field(sortType.name) }
            baseStep.where(conditions).orderBy(realSortType)
        } else {
            baseStep.where(conditions)
        }

        return baseStep
            .limit(
                (storeInfoQuery.page - 1) * storeInfoQuery.pageSize,
                storeInfoQuery.pageSize
            ).fetch()
    }



    private fun createBaseStep(dslContext: DSLContext): SelectJoinStep<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE

        return dslContext.select(
            tStoreBase.ID,
            tStoreBase.STORE_CODE,
            tStoreBase.STORE_TYPE,
            tStoreBase.NAME,
            tStoreBase.VERSION,
            tStoreBase.DOCS_LINK,
            tStoreBase.DESCRIPTION,
            tStoreBase.SUMMARY,
            tStoreBase.LOGO_URL,
            tStoreBase.PUBLISHER,
            tStoreBase.PUB_TIME,
            tStoreBase.MODIFIER,
            tStoreBase.UPDATE_TIME,
            tStoreBase.CLASSIFY_ID,
            tStoreBaseFeature.RECOMMEND_FLAG,
            tStoreBaseFeature.RD_TYPE,
            tStoreBaseFeature.PUBLIC_FLAG,
            tStoreBase.CREATE_TIME
        ).from(tStoreBase)
            .leftJoin(tStoreBaseFeature)
            .on(tStoreBase.STORE_CODE.eq(tStoreBaseFeature.STORE_CODE)
                .and(tStoreBase.STORE_TYPE.eq(tStoreBaseFeature.STORE_TYPE)))
    }

    private fun formatConditions(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeInfoQuery: StoreInfoQuery,
        baseStep: SelectJoinStep<out Record>
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
        val conditions = setStoreVisibleCondition(tStoreBase, storeType, storeInfoQuery.queryProjectComponentFlag)
        val queryProjectComponentFlag = storeInfoQuery.queryProjectComponentFlag
        val classifyId = storeInfoQuery.classifyId
        val labelId = storeInfoQuery.labelId
        val categoryId = storeInfoQuery.categoryId
        // 缩减查询范围
        if (queryProjectComponentFlag || !classifyId.isNullOrBlank() ||
            !labelId.isNullOrBlank() || !categoryId.isNullOrBlank()) {
            conditions.add(
                tStoreBase.STORE_CODE.`in`(getStoreCodesByCondition(
                    dslContext = dslContext,
                    storeType = storeType.type.toByte(),
                    storeInfoQuery = storeInfoQuery
                ))
            )
        } else {
            if (!storeInfoQuery.storeCodes.isNullOrEmpty()) {
                conditions.add(tStoreBase.STORE_CODE.`in`(storeInfoQuery.storeCodes))
            }
        }
        if (storeInfoQuery.recommendFlag != null || storeInfoQuery.rdType != null) {
            baseStep.leftJoin(tStoreBaseFeature)
                .on(tStoreBase.STORE_CODE.eq(tStoreBaseFeature.STORE_CODE)
                    .and(tStoreBase.STORE_TYPE.eq(tStoreBaseFeature.STORE_TYPE)))
            storeInfoQuery.recommendFlag?.let {
                conditions.add(tStoreBaseFeature.RECOMMEND_FLAG.eq(it))
            }
            storeInfoQuery.rdType?.let {
                conditions.add(tStoreBaseFeature.RD_TYPE.eq(it.name))
            }
        }
        if (storeInfoQuery.score != null) {
            val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            baseStep.leftJoin(tStoreStatisticsTotal).on(tStoreBase.STORE_CODE.eq(tStoreStatisticsTotal.STORE_CODE))
            conditions.add(tStoreStatisticsTotal.STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(
                tStoreStatisticsTotal.SCORE_AVERAGE.ge(BigDecimal.valueOf(storeInfoQuery.score!!.toLong()))
            )
        }
        return conditions
    }

    private fun getStoreCodesByCondition(
        dslContext: DSLContext,
        storeType: Byte,
        storeInfoQuery: StoreInfoQuery
    ): SelectConditionStep<Record1<String>> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val tStoreLabelRel = TStoreLabelRel.T_STORE_LABEL_REL
        val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
        val tClassify = TClassify.T_CLASSIFY
        val selectJoinStep = dslContext.select(tStoreBase.STORE_CODE).from(tStoreBase)
        val conditions = mutableListOf<Condition>().apply {
            add(tStoreBase.STORE_TYPE.eq(storeType))
            if (!storeInfoQuery.storeCodes.isNullOrEmpty()) {
                add(tStoreBase.STORE_CODE.`in`(storeInfoQuery.storeCodes))
            }
            storeInfoQuery.projectCode?.let {
                if (storeInfoQuery.queryProjectComponentFlag) {
                    add(tStoreProjectRel.PROJECT_CODE.eq(it))
                    selectJoinStep.leftJoin(tStoreProjectRel).on(
                        tStoreBase.STORE_CODE.eq(tStoreProjectRel.STORE_CODE)
                            .and(tStoreBase.STORE_TYPE.eq(tStoreProjectRel.STORE_TYPE))
                    )
                }
            }

            storeInfoQuery.classifyId?.let {
                add(tClassify.ID.eq(it))
                selectJoinStep.leftJoin(tClassify).on(tStoreBase.CLASSIFY_ID.eq(tClassify.ID))
            }

            storeInfoQuery.labelId?.let {
                add(tStoreLabelRel.LABEL_ID.eq(it))
                selectJoinStep.leftJoin(tStoreLabelRel).on(tStoreBase.ID.eq(tStoreLabelRel.STORE_ID))
            }

            storeInfoQuery.categoryId?.let {
                add(tStoreCategoryRel.CATEGORY_ID.eq(it))
                selectJoinStep.leftJoin(tStoreCategoryRel).on(tStoreBase.ID.eq(tStoreCategoryRel.STORE_ID))
            }
        }

        return selectJoinStep.where(conditions)
    }

    fun setStoreVisibleCondition(
        tStoreBase: TStoreBase,
        storeType: StoreTypeEnum,
        queryProjectComponentFlag: Boolean
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        if (!queryProjectComponentFlag) {
            conditions.add(tStoreBase.STATUS.eq(StoreStatusEnum.RELEASED.name))
            conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        } else {
            conditions.add(
                tStoreBase.STATUS.`in`(listOf(
                    StoreStatusEnum.RELEASED.name,
                    StoreStatusEnum.TESTING.name,
                    StoreStatusEnum.AUDITING.name
                ))
            )
        }
        return conditions
    }
}
