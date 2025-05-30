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

import com.tencent.devops.model.metrics.tables.TEplusPipelineMetricsDataDaily
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import java.time.LocalDate
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PipelineMetricsInfoDao {

    fun getPipelineIssueAnalysis(
        dslContext: DSLContext,
        projectId: String
    ): Result<TEplusPipelineMetricsDataDailyRecord> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectFrom(this)
                .where(STATISTICS_TIME.eq(LocalDate.now().atStartOfDay()))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun batchSaveHighFailureRate30dData(dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>) {
        val steps = records.map {
            with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
                dslContext.insertInto(this)
                    .set(it)
                    .onDuplicateKeyUpdate()
                    .set(FAILURE_RATE_30D, it.failureRate_30d)
            }
        }
        dslContext.batch(steps).execute()
    }

    fun batchSaveConsecutiveFailures90dData(
        dslContext: DSLContext,
        records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        val steps = records.map {
            with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
                dslContext.insertInto(this)
                    .set(it)
                    .onDuplicateKeyUpdate()
                    .set(CONSECUTIVE_FAILURES_90D, it.consecutiveFailures_90d)
            }
        }
        dslContext.batch(steps).execute()
    }

    fun batchSaveScheduledTriggerNoCodeChangeData(
        dslContext: DSLContext,
        records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        val steps = records.map {
            with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
                dslContext.insertInto(this)
                    .set(it)
                    .onDuplicateKeyUpdate()
                    .set(SCHEDULED_TRIGGER_NO_CODE_CHANGE, it.scheduledTriggerNoCodeChange)
            }
        }
        dslContext.batch(steps).execute()
    }

    fun countHighFailureRate30d(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectCount()
                .from(this)
                .where(STATISTICS_TIME.eq(LocalDate.now().atStartOfDay()))
                .and(PROJECT_ID.eq(projectId))
                .and(FAILURE_RATE_30D.eq(true))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun countConsecutiveFailures90d(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectCount()
                .from(this)
                .where(STATISTICS_TIME.eq(LocalDate.now().atStartOfDay()))
                .and(PROJECT_ID.eq(projectId))
                .and(CONSECUTIVE_FAILURES_90D.eq(true))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun countScheduledTriggerNoCodeChange(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectCount()
                .from(this)
                .where(STATISTICS_TIME.eq(LocalDate.now().atStartOfDay()))
                .and(PROJECT_ID.eq(projectId))
                .and(SCHEDULED_TRIGGER_NO_CODE_CHANGE.eq(true))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun listInvalidPipelineProjectIds(dslContext: DSLContext, limit: Int, offset: Int): List<String> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.select(PROJECT_ID).from(this)
                .where(IS_INVALID_PIPELINE.eq(true))
                .groupBy(PROJECT_ID)
                .orderBy(PROJECT_ID.desc())
                .limit(limit).offset(offset)
                .fetchInto(String::class.java)
        }
    }

    fun listProjectInvalidPipelineInfo(
        dslContext: DSLContext,
        projectId: String,
        statisticsTime: LocalDateTime
    ): Result<Record2<String, String>> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.select(PIPELINE_ID, URL)
                .from(this)
                .where(STATISTICS_TIME.eq(LocalDate.now().atStartOfDay()))
                .and(PROJECT_ID.eq(projectId))
                .and(IS_INVALID_PIPELINE.eq(true))
                .fetch()
        }
    }
}
