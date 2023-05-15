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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.JooqUtils.productSum
import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.metrics.constant.Constants
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ATOM_NAME
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME
import com.tencent.devops.metrics.constant.Constants.BK_CLASSIFY_CODE
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_RATE
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_COST_TIME_SUM
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.pojo.qo.QueryAtomStatisticsQO
import com.tencent.devops.model.metrics.tables.TAtomFailSummaryData
import com.tencent.devops.model.metrics.tables.TAtomIndexStatisticsDaily
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.Record5
import org.jooq.Record6
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class AtomStatisticsDao {

    fun queryAtomTrendInfo(
        dslContext: DSLContext,
        queryCondition: QueryAtomStatisticsQO
    ): Result<Record5<String, String, BigDecimal, Long, LocalDateTime>>? {
        val atomCodes = if (!queryCondition.errorTypes.isNullOrEmpty()) {
            getAtomCodesByErrorType(dslContext, queryCondition)
        } else queryCondition.atomCodes
        if (atomCodes.isNullOrEmpty()) return null
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(queryCondition, tProjectPipelineLabelInfo, atomCodes)
            val step = dslContext.select(
                this.ATOM_CODE.`as`(BK_ATOM_CODE),
                this.ATOM_NAME.`as`(BK_ATOM_NAME),
                this.SUCCESS_RATE.`as`(BK_SUCCESS_RATE),
                this.AVG_COST_TIME.`as`(BK_AVG_COST_TIME),
                this.STATISTICS_TIME.`as`(BK_STATISTICS_TIME)
            ).from(this)
            if (!queryCondition.baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
                step.leftJoin(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions).groupBy(ATOM_CODE, STATISTICS_TIME).fetch()
        }
    }

    private fun TAtomOverviewData.getConditions(
        queryCondition: QueryAtomStatisticsQO,
        pipelineLabelInfo: TProjectPipelineLabelInfo,
        atomCodes: List<String>
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(this.PROJECT_ID.eq(queryCondition.projectId))
        val pipelineIds = queryCondition.baseQueryReq.pipelineIds
        if (!pipelineIds.isNullOrEmpty()) {
            conditions.add(this.PIPELINE_ID.`in`(pipelineIds))
        }
        val startDateTime = DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.startTime)?.atStartOfDay()
        val endDateTime = DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.endTime)?.atStartOfDay()
        if (!queryCondition.baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
            conditions.add(pipelineLabelInfo.LABEL_ID.`in`(queryCondition.baseQueryReq.pipelineLabelIds))
        }
        if (!atomCodes.isNullOrEmpty()) {
            conditions.add(this.ATOM_CODE.`in`(atomCodes))
        }
        if (startDateTime!!.isEqual(endDateTime)) {
            conditions.add(this.STATISTICS_TIME.eq(startDateTime))
        } else {
            conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
        }
        return conditions
    }

    fun getAtomCodesByErrorType(dslContext: DSLContext, queryCondition: QueryAtomStatisticsQO): List<String>? {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            val conditions = mutableListOf<Condition>()
            val pipelineIds = queryCondition.baseQueryReq.pipelineIds
            conditions.add(this.PROJECT_ID.eq(queryCondition.projectId))
            if (!pipelineIds.isNullOrEmpty()) {
                conditions.add(this.PIPELINE_ID.`in`(pipelineIds))
            }
            if (!queryCondition.atomCodes.isNullOrEmpty()) {
                conditions.add(this.ATOM_CODE.`in`(queryCondition.atomCodes))
            }
            val startDateTime = DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.startTime)?.atStartOfDay()
            val endDateTime = DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.endTime)?.atStartOfDay()
            if (!queryCondition.errorTypes.isNullOrEmpty()) {
                conditions.add(this.ERROR_TYPE.`in`(queryCondition.errorTypes))
            }
            if (startDateTime!!.isEqual(endDateTime)) {
                conditions.add(this.STATISTICS_TIME.eq(startDateTime))
            } else {
                conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
            }
            val fetch = dslContext.select(ATOM_CODE).from(this).where(conditions).groupBy(ATOM_CODE).fetch()
            if (fetch.isNotEmpty) {
                return fetch.map { it.value1() }
            }
            return null
        }
    }

    fun queryAtomExecuteStatisticsInfo(
        dslContext: DSLContext,
        queryCondition: QueryAtomStatisticsQO
    ): Result<Record6<String, String, String, BigDecimal, BigDecimal, BigDecimal>>? {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val atomCodes = if (!queryCondition.errorTypes.isNullOrEmpty()) {
                getAtomCodesByErrorType(dslContext, queryCondition)
            } else queryCondition.atomCodes
            if (atomCodes.isNullOrEmpty()) return null
            val conditions = getConditions(queryCondition, tProjectPipelineLabelInfo, atomCodes)
            val totalCostTimeSum = productSum(AVG_COST_TIME, TOTAL_EXECUTE_COUNT).`as`(BK_TOTAL_COST_TIME_SUM)
            val step = dslContext.select(
                ATOM_CODE.`as`(BK_ATOM_CODE),
                ATOM_NAME.`as`(BK_ATOM_NAME),
                CLASSIFY_CODE.`as`(BK_CLASSIFY_CODE),
                sum<Long>(TOTAL_EXECUTE_COUNT).`as`(BK_TOTAL_EXECUTE_COUNT_SUM),
                sum<Long>(SUCCESS_EXECUTE_COUNT).`as`(BK_SUCCESS_EXECUTE_COUNT_SUM),
                totalCostTimeSum
            ).from(this)
            if (!queryCondition.baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
                step.leftJoin(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions)
                .groupBy(ATOM_CODE)
                .orderBy(SUCCESS_RATE, ATOM_CODE)
                .offset((queryCondition.page - 1) * queryCondition.pageSize)
                .limit(queryCondition.pageSize)
                .fetch()
        }
    }

    fun queryAtomFailStatisticsInfo(
        dslContext: DSLContext,
        queryCondition: QueryAtomStatisticsQO
    ): Result<Record3<String, Int, BigDecimal>> {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            val startTimeDateTime =
                DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.startTime!!)!!.atStartOfDay()
            val endTimeDateTime = DateTimeUtil.stringToLocalDate(queryCondition.baseQueryReq.endTime!!)!!.atStartOfDay()
            return dslContext.select(
                ATOM_CODE.`as`(BK_ATOM_CODE),
                ERROR_TYPE.`as`(BK_ERROR_TYPE),
                sum<Int>(ERROR_COUNT).`as`(BK_ERROR_COUNT_SUM)
            ).from(this)
                .where(PROJECT_ID.eq(queryCondition.projectId))
                .and(STATISTICS_TIME.between(startTimeDateTime, endTimeDateTime))
                .groupBy(ATOM_CODE, ERROR_TYPE)
                .fetch()
        }
    }

    fun queryAtomExecuteStatisticsInfoCount(
        dslContext: DSLContext,
        queryCondition: QueryAtomStatisticsQO
    ): Long {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val atomCodes = if (!queryCondition.errorTypes.isNullOrEmpty()) {
                getAtomCodesByErrorType(dslContext, queryCondition)
            } else queryCondition.atomCodes
            if (atomCodes.isNullOrEmpty()) return 0L
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(queryCondition, tProjectPipelineLabelInfo, atomCodes)
            val step = dslContext.selectCount().from(this)
            if (!queryCondition.baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
                step.leftJoin(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions).groupBy(ATOM_CODE).execute().toLong()
        }
    }

    fun queryAtomComplianceInfo(
        dslContext: DSLContext,
        atomCode: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Record2<BigDecimal, BigDecimal>? {
        with(TAtomIndexStatisticsDaily.T_ATOM_INDEX_STATISTICS_DAILY) {
            return dslContext.select(
                sum(FAIL_EXECUTE_COUNT).`as`(Constants.BK_FAIL_EXECUTE_COUNT),
                sum(FAIL_COMPLIANCE_COUNT).`as`(Constants.BK_FAIL_COMPLIANCE_COUNT)
            ).from(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(STATISTICS_TIME.between(startDateTime, endDateTime))
                .fetchOne()
        }
    }
}
