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

import com.tencent.devops.model.metrics.tables.TAtomFailSummaryData
import com.tencent.devops.model.metrics.tables.TAtomIndexStatisticsDaily
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TPipelineFailSummaryData
import com.tencent.devops.model.metrics.tables.TPipelineOverviewData
import com.tencent.devops.model.metrics.tables.TPipelineStageOverviewData
import com.tencent.devops.model.metrics.tables.records.TAtomFailSummaryDataRecord
import com.tencent.devops.model.metrics.tables.records.TAtomIndexStatisticsDailyRecord
import com.tencent.devops.model.metrics.tables.records.TAtomOverviewDataRecord
import com.tencent.devops.model.metrics.tables.records.TPipelineFailSummaryDataRecord
import com.tencent.devops.model.metrics.tables.records.TPipelineOverviewDataRecord
import com.tencent.devops.model.metrics.tables.records.TPipelineStageOverviewDataRecord
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class MetricsDataQueryDao {

    fun getPipelineOverviewData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ): TPipelineOverviewDataRecord? {
        with(TPipelineOverviewData.T_PIPELINE_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    fun getPipelineStageOverviewDatas(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        stageTagNames: List<String>
    ): Result<TPipelineStageOverviewDataRecord>? {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(STAGE_TAG_NAME.`in`(stageTagNames))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getPipelineFailSummaryData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        errorType: Int
    ): TPipelineFailSummaryDataRecord? {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ERROR_TYPE.eq(errorType))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    fun getAtomOverviewDatas(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        atomCodes: List<String>
    ): Result<TAtomOverviewDataRecord>? {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ATOM_CODE.`in`(atomCodes))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getAtomFailSummaryData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        errorType: Int,
        atomCode: String
    ): TAtomFailSummaryDataRecord? {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ERROR_TYPE.eq(errorType))
            conditions.add(ATOM_CODE.eq(atomCode))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
        }
    }

    fun getAtomIndexStatisticsDailyData(
        dslContext: DSLContext,
        statisticsTime: LocalDateTime,
        atomCode: String
    ): TAtomIndexStatisticsDailyRecord? {
        with(TAtomIndexStatisticsDaily.T_ATOM_INDEX_STATISTICS_DAILY) {
            return dslContext.selectFrom(this)
                .where(STATISTICS_TIME.eq(statisticsTime))
                .and(ATOM_CODE.eq(atomCode))
                .fetchOne()
        }
    }
}
