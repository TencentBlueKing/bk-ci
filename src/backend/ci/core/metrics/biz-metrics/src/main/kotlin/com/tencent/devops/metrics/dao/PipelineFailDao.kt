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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.pojo.po.PipelineFailDetailDataPO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineFailQO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineOverviewQO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.model.metrics.tables.TPipelineFailDetailData
import com.tencent.devops.model.metrics.tables.TPipelineFailSummaryData
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PipelineFailDao constructor(private val metricsConfig: MetricsConfig) {

    fun getPipelineIdByTotalExecuteCount(
        dslContext: DSLContext,
        queryPipelineFailQo: QueryPipelineFailQO
    ): List<String> {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val pipelineIds = queryPipelineFailQo.baseQueryReq.pipelineIds
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailQo.projectId,
                baseQueryReq = queryPipelineFailQo.baseQueryReq,
                pipelineIds = pipelineIds
            )
            val step = dslContext.select(PIPELINE_ID).from(this)
            return step.where(conditions)
                .orderBy(ERROR_COUNT.desc())
                .limit(metricsConfig.defaultLimitNum)
                .fetchInto(String::class.java)
        }
    }

    fun queryPipelineFailTrendInfo(
        dslContext: DSLContext,
        queryPipelineFailTrendQo: QueryPipelineOverviewQO,
        errorType: Int
    ): Result<Record2<LocalDateTime, BigDecimal>> {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val pipelineIds = queryPipelineFailTrendQo.baseQueryReq.pipelineIds
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailTrendQo.projectId,
                baseQueryReq = queryPipelineFailTrendQo.baseQueryReq,
                pipelineIds = pipelineIds
            )
            val step = dslContext.select(
                this.STATISTICS_TIME.`as`(BK_STATISTICS_TIME),
                sum<Int>(ERROR_COUNT).`as`(BK_ERROR_COUNT_SUM)
            ).from(this)
            conditions.add(ERROR_TYPE.eq(errorType))
            return step.where(conditions).groupBy(this.STATISTICS_TIME).fetch()
        }
    }

    fun queryPipelineFailTrendInfoCount(
        dslContext: DSLContext,
        queryPipelineFailTrendQo: QueryPipelineOverviewQO,
        errorType: Int
    ): Int {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val pipelineIds = queryPipelineFailTrendQo.baseQueryReq.pipelineIds
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailTrendQo.projectId,
                baseQueryReq = queryPipelineFailTrendQo.baseQueryReq,
                pipelineIds = pipelineIds
            )
            val step = dslContext.selectCount().from(this)
            conditions.add(ERROR_TYPE.eq(errorType))
            return step.where(conditions).fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun queryPipelineFailErrorTypeInfo(
        dslContext: DSLContext,
        queryPipelineFailTrendQo: QueryPipelineOverviewQO
    ): List<Int> {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val pipelineIds = queryPipelineFailTrendQo.baseQueryReq.pipelineIds
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailTrendQo.projectId,
                baseQueryReq = queryPipelineFailTrendQo.baseQueryReq,
                pipelineIds = pipelineIds
            )
            val step = dslContext.select(this.ERROR_TYPE)
                .from(this)
            return step.where(conditions).groupBy(ERROR_TYPE).fetchInto(Int::class.java)
        }
    }

    fun queryPipelineFailSumInfo(
        dslContext: DSLContext,
        queryPipelineFailQo: QueryPipelineFailQO
    ): Result<Record2<Int, BigDecimal>> {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            var pipelineIds = queryPipelineFailQo.baseQueryReq.pipelineIds
            if (!pipelineIds.isNullOrEmpty()) {
                pipelineIds = getPipelineIdByTotalExecuteCount(dslContext, queryPipelineFailQo)
            }
            val conditions = getConditions(
                    dslContext = dslContext,
                    projectId = queryPipelineFailQo.projectId,
                    baseQueryReq = queryPipelineFailQo.baseQueryReq,
                    pipelineIds = pipelineIds
                )
            if (!queryPipelineFailQo.errorTypes.isNullOrEmpty()) {
                conditions.add(ERROR_TYPE.`in`(queryPipelineFailQo.errorTypes))
            }
            val step = dslContext.select(
                ERROR_TYPE.`as`(BK_ERROR_TYPE),
                sum<Int>(ERROR_COUNT).`as`(BK_ERROR_COUNT_SUM)
            ).from(this)
            return step.where(conditions).groupBy(this.ERROR_TYPE).fetch()
        }
    }

    fun queryPipelineFailDetailInfo(
        dslContext: DSLContext,
        queryPipelineFailQo: QueryPipelineFailQO
    ): List<PipelineFailDetailDataPO> {
        with(TPipelineFailDetailData.T_PIPELINE_FAIL_DETAIL_DATA) {
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailQo.projectId,
                baseQuery = queryPipelineFailQo.baseQueryReq
            )
            if (!queryPipelineFailQo.errorTypes.isNullOrEmpty()) {
                conditions.add(ERROR_TYPE.`in`(queryPipelineFailQo.errorTypes))
            }
            val step = dslContext.select(
                PROJECT_ID,
                PIPELINE_ID,
                PIPELINE_NAME,
                CHANNEL_CODE,
                BUILD_ID,
                BUILD_NUM,
                REPO_URL,
                BRANCH,
                START_USER,
                START_TIME,
                END_TIME,
                ERROR_TYPE,
                ERROR_CODE,
                ERROR_MSG,
                STATISTICS_TIME
            ).from(this)
            return step.where(conditions)
                .orderBy(START_TIME.desc(), ID)
                .offset((queryPipelineFailQo.page - 1) * queryPipelineFailQo.pageSize)
                .limit(queryPipelineFailQo.pageSize)
                .fetchInto(PipelineFailDetailDataPO::class.java)
        }
    }

    fun queryPipelineFailDetailCount(
        dslContext: DSLContext,
        queryPipelineFailQo: QueryPipelineFailQO
    ): Long {
        with(TPipelineFailDetailData.T_PIPELINE_FAIL_DETAIL_DATA) {
            val conditions = getConditions(
                dslContext = dslContext,
                projectId = queryPipelineFailQo.projectId,
                baseQuery = queryPipelineFailQo.baseQueryReq
            )
            if (!queryPipelineFailQo.errorTypes.isNullOrEmpty()) {
                conditions.add(ERROR_TYPE.`in`(queryPipelineFailQo.errorTypes))
            }
            val step = dslContext.selectCount().from(this)
            return step.where(conditions)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    private fun TPipelineFailDetailData.getConditions(
        dslContext: DSLContext,
        projectId: String,
        baseQuery: BaseQueryReqVO
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(this.PROJECT_ID.eq(projectId))
        val startDateTime =
            DateTimeUtil.stringToLocalDate(baseQuery.startTime!!)!!.atStartOfDay()
        val endDateTime =
            DateTimeUtil.stringToLocalDate(baseQuery.endTime!!)!!.atStartOfDay()
        if (startDateTime.isEqual(endDateTime)) {
            conditions.add(this.STATISTICS_TIME.eq(startDateTime))
        } else {
            conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
        }
        if (!baseQuery.pipelineIds.isNullOrEmpty()) {
            conditions.add(this.PIPELINE_ID.`in`(baseQuery.pipelineIds))
        }
        if (!baseQuery.pipelineLabelIds.isNullOrEmpty()) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            conditions.add(
                DSL.exists(
                    dslContext.select(tProjectPipelineLabelInfo.PIPELINE_ID).from(tProjectPipelineLabelInfo)
                        .where(tProjectPipelineLabelInfo.PROJECT_ID.eq(this.PROJECT_ID))
                        .and(tProjectPipelineLabelInfo.PIPELINE_ID.eq(this.PIPELINE_ID))
                        .and(
                            tProjectPipelineLabelInfo.LABEL_ID.`in`(
                                baseQuery.pipelineLabelIds
                            )
                        )
                )
            )
        }
        return conditions
    }

    private fun TPipelineFailSummaryData.getConditions(
        dslContext: DSLContext,
        projectId: String,
        baseQueryReq: BaseQueryReqVO,
        pipelineIds: List<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_ID.eq(projectId))

        val startDateTime = DateTimeUtil.stringToLocalDate(baseQueryReq.startTime!!)!!.atStartOfDay()
        val endDateTime = DateTimeUtil.stringToLocalDate(baseQueryReq.endTime!!)!!.atStartOfDay()
        if (startDateTime.isEqual(endDateTime)) {
            conditions.add(this.STATISTICS_TIME.eq(startDateTime))
        } else {
            conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
        }
        if (!pipelineIds.isNullOrEmpty()) {
            conditions.add(this.PIPELINE_ID.`in`(pipelineIds))
        }
        if (!baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            conditions.add(
                DSL.exists(
                    dslContext.select(tProjectPipelineLabelInfo.PIPELINE_ID).from(tProjectPipelineLabelInfo)
                        .where(tProjectPipelineLabelInfo.PROJECT_ID.eq(this.PROJECT_ID))
                        .and(tProjectPipelineLabelInfo.PIPELINE_ID.eq(this.PIPELINE_ID))
                        .and(
                            tProjectPipelineLabelInfo.LABEL_ID.`in`(
                                baseQueryReq.pipelineLabelIds
                            )
                        )
                )
            )
        }
        return conditions
    }
}
