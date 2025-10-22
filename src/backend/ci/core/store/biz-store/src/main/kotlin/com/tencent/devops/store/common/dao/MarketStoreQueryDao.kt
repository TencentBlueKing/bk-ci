/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TStoreBase
import com.tencent.devops.model.store.tables.TStoreBaseFeature
import com.tencent.devops.model.store.tables.TStoreCategoryRel
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.model.store.tables.TStoreLabelRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record2
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
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery
    ): Int {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
        val tStoreDeptRel = TStoreDeptRel.T_STORE_DEPT_REL
        val baseStep =
            dslContext.select(DSL.countDistinct(tStoreBase.STORE_CODE)).from(tStoreBase).leftJoin(tStoreBaseFeature)
                .on(
                    tStoreBase.STORE_CODE.eq(tStoreBaseFeature.STORE_CODE)
                        .and(tStoreBase.STORE_TYPE.eq(tStoreBaseFeature.STORE_TYPE))
                )
        applyConditionFilter(
            dslContext = dslContext,
            baseStep = baseStep,
            tStoreBase = tStoreBase,
            tStoreDeptRel = tStoreDeptRel,
            userDeptList = userDeptList,
            storeInfoQuery = storeInfoQuery
        )
        return baseStep.fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 研发商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery
    ): Result<out Record> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreDeptRel = TStoreDeptRel.T_STORE_DEPT_REL
        val baseStep = createBaseStep(dslContext)
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)

        storeInfoQuery.sortType?.let { sortType ->
            applySorting(
                dslContext = dslContext,
                baseStep = baseStep,
                tStoreBase = tStoreBase,
                storeType = storeType,
                sortType = sortType,
                score = storeInfoQuery.score
            )
        }

        applyConditionFilter(
            dslContext = dslContext,
            baseStep = baseStep,
            tStoreBase = tStoreBase,
            tStoreDeptRel = tStoreDeptRel,
            userDeptList = userDeptList,
            storeInfoQuery = storeInfoQuery
        )
        val offset = (storeInfoQuery.page - 1) * storeInfoQuery.pageSize
        return baseStep.limit(offset, storeInfoQuery.pageSize)
            .skipCheck()
            .fetch()
    }

    private fun applySorting(
        dslContext: DSLContext,
        baseStep: SelectJoinStep<out Record>,
        tStoreBase: TStoreBase,
        storeType: StoreTypeEnum,
        sortType: StoreSortTypeEnum,
        score: Int?
    ) {
        val isDownloadCountSort = sortType == StoreSortTypeEnum.DOWNLOAD_COUNT
        val needStatsJoin = isDownloadCountSort && score == null
        if (needStatsJoin) {
            val statsSubquery = buildStatsSubquery(dslContext, sortType, storeType)
            baseStep.leftJoin(statsSubquery)
                .on(tStoreBase.STORE_CODE.eq(statsSubquery.field(KEY_STORE_CODE, String::class.java)))
        }

        val sortField = if (isDownloadCountSort) {
            DSL.field(sortType.name)
        } else {
            tStoreBase.field(sortType.name) ?: throw IllegalArgumentException("Invalid sort field")
        }
        baseStep.orderBy(sortField.desc())
    }

    private fun buildStatsSubquery(
        dslContext: DSLContext,
        sortType: StoreSortTypeEnum,
        storeType: StoreTypeEnum
    ): SelectConditionStep<Record2<String, Int>> {
        val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
        val statsSubquery = dslContext.select(
            tStoreStatisticsTotal.STORE_CODE,
            tStoreStatisticsTotal.DOWNLOADS.`as`(sortType.name)
        )
            .from(tStoreStatisticsTotal)
            .where(tStoreStatisticsTotal.STORE_TYPE.eq(storeType.type.toByte()))
        return statsSubquery
    }

    private fun applyConditionFilter(
        dslContext: DSLContext,
        baseStep: SelectJoinStep<out Record>,
        tStoreBase: TStoreBase,
        tStoreDeptRel: TStoreDeptRel,
        userDeptList: List<Int>,
        storeInfoQuery: StoreInfoQuery
    ) {
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        val conditions = formatConditions(
            dslContext = dslContext,
            storeType = storeType,
            storeInfoQuery = storeInfoQuery,
            baseStep = baseStep
        )

        val finalConditions = if (storeType == StoreTypeEnum.DEVX && storeInfoQuery.queryTestFlag != true) {
            val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
            val deptCondition = tStoreDeptRel.STORE_CODE.eq(tStoreBase.STORE_CODE)
                .and(tStoreDeptRel.STORE_TYPE.eq(tStoreBase.STORE_TYPE))
                .and(tStoreDeptRel.DEPT_ID.`in`(userDeptList))
            val existsCondition = DSL.exists(
                dslContext.selectOne()
                    .from(tStoreDeptRel)
                    .where(deptCondition)
            )
            val publicFlagCondition = tStoreBaseFeature.PUBLIC_FLAG.eq(true)
            val combinedCondition = DSL.or(publicFlagCondition, existsCondition)

            conditions.plus(combinedCondition)
        } else {
            conditions
        }

        baseStep.where(finalConditions)
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
            tStoreBase.STATUS,
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
            tStoreBase.CREATE_TIME,
            tStoreBase.BUS_NUM
        ).from(tStoreBase)
            .leftJoin(tStoreBaseFeature)
            .on(
                tStoreBase.STORE_CODE.eq(tStoreBaseFeature.STORE_CODE)
                    .and(tStoreBase.STORE_TYPE.eq(tStoreBaseFeature.STORE_TYPE))
            )
    }

    private fun formatConditions(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeInfoQuery: StoreInfoQuery,
        baseStep: SelectJoinStep<out Record>
    ): MutableList<Condition> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val tStoreBaseFeature = TStoreBaseFeature.T_STORE_BASE_FEATURE
        val tStoreLabelRel = TStoreLabelRel.T_STORE_LABEL_REL
        val tStoreCategoryRel = TStoreCategoryRel.T_STORE_CATEGORY_REL
        val tClassify = TClassify.T_CLASSIFY
        val conditions = mutableListOf<Condition>()
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        // 缩减查询范围
        if (storeInfoQuery.getSpecQueryFlag()) {
            conditions.add(
                tStoreBase.ID.`in`(
                    getStoreIdsByCondition(
                        dslContext = dslContext,
                        storeInfoQuery = storeInfoQuery
                    )
                )
            )
        } else {
            conditions.add(tStoreBase.STATUS.eq(StoreStatusEnum.RELEASED.name))
            conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        }
        conditions.add(tStoreBaseFeature.SHOW_FLAG.eq(true))
        storeInfoQuery.recommendFlag?.let {
            conditions.add(tStoreBaseFeature.RECOMMEND_FLAG.eq(it))
        }
        storeInfoQuery.rdType?.let {
            conditions.add(tStoreBaseFeature.RD_TYPE.eq(it.name))
        }
        storeInfoQuery.score?.let {
            val tStoreStatisticsTotal = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL
            baseStep.leftJoin(tStoreStatisticsTotal).on(tStoreBase.STORE_CODE.eq(tStoreStatisticsTotal.STORE_CODE))
            conditions.add(tStoreStatisticsTotal.STORE_TYPE.eq(storeType.type.toByte()))
            conditions.add(
                tStoreStatisticsTotal.SCORE_AVERAGE.ge(it.toBigDecimal())
            )
        }
        val keyword = storeInfoQuery.keyword
        if (!keyword.isNullOrEmpty()) {
            conditions.add(tStoreBase.NAME.contains(keyword).or(tStoreBase.SUMMARY.contains(keyword)))
        }
        storeInfoQuery.classifyId?.let {
            baseStep.leftJoin(tClassify).on(tStoreBase.CLASSIFY_ID.eq(tClassify.ID))
            conditions.add(tStoreBase.CLASSIFY_ID.eq(it))
        }

        storeInfoQuery.labelId?.let {
            baseStep.leftJoin(tStoreLabelRel).on(tStoreBase.ID.eq(tStoreLabelRel.STORE_ID))
            conditions.add(tStoreLabelRel.LABEL_ID.eq(it))
        }

        storeInfoQuery.categoryId?.let {
            baseStep.leftJoin(tStoreCategoryRel).on(tStoreBase.ID.eq(tStoreCategoryRel.STORE_ID))
            conditions.add(tStoreCategoryRel.CATEGORY_ID.eq(it))
        }
        return conditions
    }

    private fun getStoreIdsByCondition(
        dslContext: DSLContext,
        storeInfoQuery: StoreInfoQuery
    ): SelectConditionStep<Record1<String>> {
        val tStoreBase = TStoreBase.T_STORE_BASE
        val selectJoinStep = dslContext.selectDistinct(tStoreBase.ID).from(tStoreBase)
        val subConditions = setStoreVisibleCondition(tStoreBase, storeInfoQuery)
        return selectJoinStep.where(subConditions)
    }

    fun setStoreVisibleCondition(
        tStoreBase: TStoreBase,
        storeInfoQuery: StoreInfoQuery
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        val storeType = StoreTypeEnum.valueOf(storeInfoQuery.storeType)
        conditions.add(tStoreBase.STORE_TYPE.eq(storeType.type.toByte()))
        if (storeInfoQuery.getSpecQueryFlag()) {
            val testStoreQueryCondition = storeInfoQuery.testStoreCodes?.let { testStoreCodes ->
                tStoreBase.STORE_CODE.`in`(testStoreCodes).and(
                    tStoreBase.STATUS.`in`(StoreStatusEnum.getTestStatusList())
                )
            }

            val normalStoreQueryCondition = storeInfoQuery.normalStoreCodes?.let { normalStoreCodes ->
                tStoreBase.STORE_CODE.`in`(normalStoreCodes).and(
                    tStoreBase.STATUS.eq(StoreStatusEnum.RELEASED.name)
                ).and(
                    tStoreBase.LATEST_FLAG.eq(true)
                )
            }

            if (testStoreQueryCondition != null && normalStoreQueryCondition != null) {
                conditions.add(normalStoreQueryCondition.or(testStoreQueryCondition))
            } else if (testStoreQueryCondition != null) {
                conditions.add(testStoreQueryCondition)
            } else if (normalStoreQueryCondition != null) {
                conditions.add(normalStoreQueryCondition)
            }
        } else {
            conditions.add(tStoreBase.STATUS.eq(StoreStatusEnum.RELEASED.name))
            conditions.add(tStoreBase.LATEST_FLAG.eq(true))
        }
        return conditions
    }
}
