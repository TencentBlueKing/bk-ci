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

import com.tencent.devops.model.metrics.tables.TAtomFailDetailData
import com.tencent.devops.model.metrics.tables.TAtomFailSummaryData
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TPipelineFailDetailData
import com.tencent.devops.model.metrics.tables.TPipelineFailSummaryData
import com.tencent.devops.model.metrics.tables.TPipelineOverviewData
import com.tencent.devops.model.metrics.tables.TPipelineStageOverviewData
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MetricsDataClearDao {

    fun clearRedundantPipelineOverviewData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        with(TPipelineOverviewData.T_PIPELINE_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ID.notIn(
                dslContext.select(DSL.min(ID)).from(this).where(conditions)
            ))
            dslContext.deleteFrom(this).where(conditions).execute()
        }
    }

    fun clearRedundantPipelineStageOverviewData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ID.notIn(
                dslContext.select(DSL.min(ID)).from(this).where(conditions)
            ))
            dslContext.deleteFrom(this).where(conditions).execute()
        }
    }

    fun clearRedundantPipelineFailSummaryData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ID.notIn(
                dslContext.select(DSL.min(ID)).from(this).where(conditions).groupBy(ERROR_TYPE)
            ))
            dslContext.deleteFrom(this).where(conditions).execute()
        }
    }

    fun clearRedundantPipelineFailDetailData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        buildId: String
    ) {
        with(TPipelineFailDetailData.T_PIPELINE_FAIL_DETAIL_DATA) {
            dslContext.deleteFrom(this).where(
                PROJECT_ID.eq(projectId)
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(STATISTICS_TIME.eq(statisticsTime))
                    .and(BUILD_ID.eq(buildId))
            ).execute()
        }
    }

    fun clearRedundantAtomOverviewData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ID.notIn(
                dslContext.select(DSL.min(ID)).from(this).where(conditions).groupBy(ATOM_CODE)
            ))
            dslContext.deleteFrom(this).where(conditions).execute()
        }
    }

    fun clearRedundantAtomFailDetailData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            conditions.add(STATISTICS_TIME.eq(statisticsTime))
            conditions.add(ID.notIn(
                dslContext.select(DSL.min(ID)).from(this).where(conditions).groupBy(ERROR_TYPE, ATOM_CODE)
            ))
            dslContext.deleteFrom(this).where(conditions).execute()
        }
    }

    fun clearRedundantAtomFailDetailData(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime,
        buildId: String
    ) {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            dslContext.deleteFrom(this).where(
                PROJECT_ID.eq(projectId)
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(STATISTICS_TIME.eq(statisticsTime))
                    .and(BUILD_ID.eq(buildId))
            ).execute()
        }
    }
}
