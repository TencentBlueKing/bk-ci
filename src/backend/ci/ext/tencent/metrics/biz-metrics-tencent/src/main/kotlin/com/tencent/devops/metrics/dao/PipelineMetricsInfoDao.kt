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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.metrics.tables.TEplusPipelineMetricsDataDaily
import com.tencent.devops.model.metrics.tables.TEplusPipelineMetricsWhiteList
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import java.time.LocalDate
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PipelineMetricsInfoDao {

    // 获取当前统计时间（凌晨0点）
    private val currentStatisticsTime: LocalDateTime
        get() = LocalDate.now().atStartOfDay()

    fun getPipelineIssueAnalysis(
        dslContext: DSLContext,
        projectId: String
    ): Result<TEplusPipelineMetricsDataDailyRecord> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectFrom(this)
                .where(STATISTICS_TIME.eq(currentStatisticsTime))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }

    }

    // 统一的批量保存方法
    private fun batchSaveData(
        dslContext: DSLContext,
        records: List<TEplusPipelineMetricsDataDailyRecord>,
        updateField: Field<Boolean>
    ) {
        if (records.isEmpty()) return
        val tEplusPipelineMetricsDataDaily = TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY
        // 分批次处理，每批1000条
        records.chunked(1000).forEach { batch ->
            // 为每个批次创建单独的插入操作
            val queries = batch.map { record ->
                dslContext.insertInto(tEplusPipelineMetricsDataDaily)
                    .set(record)
                    .onDuplicateKeyUpdate()
                    .set(updateField, record.get(updateField))
            }

            // 批量执行
            dslContext.batch(queries).execute()
        }
    }

    fun batchSaveHighFailureRate30dData(
        dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            batchSaveData(dslContext, records, FAILURE_RATE_30D)
        }
    }

    fun batchSaveConsecutiveFailures6mData(
        dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            batchSaveData(dslContext, records, CONSECUTIVE_FAILURES_6M)
        }
    }

    fun batchSaveConsecutiveFailures90dData(
        dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            batchSaveData(dslContext, records, CONSECUTIVE_FAILURES_90D)
        }
    }

    fun batchSaveScheduledTriggerNoCodeChangeData(
        dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>
    ) {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            batchSaveData(dslContext, records, SCHEDULED_TRIGGER_NO_CODE_CHANGE)
        }
    }

    // 统一的计数方法（排除白名单流水线）
    private fun countByField(
        dslContext: DSLContext,
        projectId: String,
        field: Field<Boolean>
    ): Int {
        val tEplusPipelineMetricsDataDaily = TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY
        with(TEplusPipelineMetricsWhiteList.T_EPLUS_PIPELINE_METRICS_WHITE_LIST) {
            return dslContext.selectCount()
                .from(tEplusPipelineMetricsDataDaily)
                .leftJoin(this)
                .on(
                    tEplusPipelineMetricsDataDaily.PIPELINE_ID.eq(PIPELINE_ID)
                        .and(tEplusPipelineMetricsDataDaily.PROJECT_ID.eq(PROJECT_ID))
                )
                .where(tEplusPipelineMetricsDataDaily.STATISTICS_TIME.eq(currentStatisticsTime))
                .and(tEplusPipelineMetricsDataDaily.PROJECT_ID.eq(projectId))
                .and(field.eq(true))
                .and(PIPELINE_ID.isNull()) // 排除白名单中的流水线
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun countHighFailureRate30d(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return countByField(dslContext, projectId, FAILURE_RATE_30D)
        }
    }

    fun countConsecutiveFailures90d(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return countByField(dslContext, projectId, CONSECUTIVE_FAILURES_90D)
        }
    }

    fun countScheduledTriggerNoCodeChange(dslContext: DSLContext, projectId: String): Int {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return countByField(dslContext, projectId, SCHEDULED_TRIGGER_NO_CODE_CHANGE)
        }
    }

    fun listInvalidPipelineProjectIds(dslContext: DSLContext, limit: Int, offset: Int): List<String> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.select(PROJECT_ID).from(this)
                .where(INVALID_PIPELINE_FLAG.eq(true))
                .groupBy(PROJECT_ID)
                .orderBy(PROJECT_ID.desc())
                .limit(limit).offset(offset)
                .fetchInto(String::class.java)
        }
    }

    /**
     * 查询连续失败6个月的流水线记录
     */
    fun listConsecutiveFailures6mPipelines(
        dslContext: DSLContext,
        statisticsTime: LocalDateTime,
        projectId: String
    ): Result<TEplusPipelineMetricsDataDailyRecord> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATISTICS_TIME.eq(statisticsTime))
                .and(CONSECUTIVE_FAILURES_6M.eq(true))
            return query.fetch()
        }
    }

    /**
     * 分页查询有连续失败6个月流水线的项目ID
     */
    fun listConsecutiveFailures6mProjectIds(dslContext: DSLContext, limit: Int, offset: Int): List<String> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.select(PROJECT_ID).from(this)
                .where(CONSECUTIVE_FAILURES_6M.eq(true))
                .and(STATISTICS_TIME.eq(currentStatisticsTime))
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
    ): Result<TEplusPipelineMetricsDataDailyRecord> {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            return dslContext.selectFrom(this)
                .where(STATISTICS_TIME.eq(statisticsTime))
                .and(PROJECT_ID.eq(projectId))
                .and(INVALID_PIPELINE_FLAG.eq(true))
                .fetch()
        }
    }

    fun batchSaveInvalidPipelineData(dslContext: DSLContext, records: List<TEplusPipelineMetricsDataDailyRecord>){
        val steps = records.map {
            with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
                dslContext.insertInto(this)
                    .set(it)
                    .onDuplicateKeyUpdate()
                    .set(INVALID_PIPELINE_FLAG, it.invalidPipelineFlag)
                    .set(URL, it.url)
                    .set(PIPELINE_NAME, it.pipelineName)
            }
        }
        dslContext.batch(steps).execute()
    }

    /**
     * 清理当天统计数据
     * @param dslContext 数据库上下文
     * @return 删除的记录数
     */
    fun cleanTodayData(dslContext: DSLContext) {
        with(TEplusPipelineMetricsDataDaily.T_EPLUS_PIPELINE_METRICS_DATA_DAILY) {
            dslContext.deleteFrom(this)
                .where(STATISTICS_TIME.eq(currentStatisticsTime))
                .execute()
        }
    }

    fun addAutoDisableWhitelist(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: List<String>
    ) {
        if (pipelineIds.isEmpty()) return
        val queries = pipelineIds.map { pipelineId ->
            with(TEplusPipelineMetricsWhiteList.T_EPLUS_PIPELINE_METRICS_WHITE_LIST) {
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    CREATE_TIME
                ).values(
                    UUIDUtil.generate(),
                    projectId,
                    pipelineId,
                    LocalDateTime.now()
                )
                    .onDuplicateKeyIgnore()
            }
        }
        dslContext.batch(queries).execute()
    }

    /**
     * 更新流水线自动禁用白名单设置
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param deleteFlag 是否删除
     * @return 更新记录数
     */
    fun removeAutoDisableWhitelist(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: List<String>
    ) {
        with(TEplusPipelineMetricsWhiteList.T_EPLUS_PIPELINE_METRICS_WHITE_LIST) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }

    /**
     * 根据项目ID获取自动禁用白名单流水线列表
     * @param projectId 项目ID
     * @return 流水线ID列表
     */
    fun listAutoDisableWhitelist(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        with(TEplusPipelineMetricsWhiteList.T_EPLUS_PIPELINE_METRICS_WHITE_LIST) {
            return dslContext.select(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchInto(String::class.java)
        }
    }
}
