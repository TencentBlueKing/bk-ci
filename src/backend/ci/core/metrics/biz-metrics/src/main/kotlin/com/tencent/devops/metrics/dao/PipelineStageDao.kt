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
import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME
import com.tencent.devops.metrics.constant.Constants.BK_PIPELINE_NAME
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.model.metrics.tables.TPipelineStageOverviewData
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import com.tencent.devops.metrics.pojo.qo.QueryPipelineStageTrendInfoQO
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineStageDao constructor(private val metricsConfig: MetricsConfig) {

    fun getStagePipelineIdByProject(
        dslContext: DSLContext,
        projectId: String,
        tag: String,
        startTime: String,
        endTime: String
    ): List<String> {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            val startDateTime =
                DateTimeUtil.stringToLocalDate(startTime)!!.atStartOfDay()
            val endDateTime =
                DateTimeUtil.stringToLocalDate(endTime)!!.atStartOfDay()
            val field = sum<Long>(AVG_COST_TIME).`as`(Constants.BK_TOTAL_EXECUTE_COUNT_SUM)
            return dslContext.select(
                PIPELINE_ID,
                field
            ).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATISTICS_TIME.between(startDateTime, endDateTime))
                .groupBy(PIPELINE_ID)
                .orderBy(field.desc(), PIPELINE_ID)
                .limit(metricsConfig.defaultLimitNum)
                .fetch().map { it.value1() }
        }
    }

    fun getStageTrendPipelineInfo(
        dslContext: DSLContext,
        queryInfo: QueryPipelineStageTrendInfoQO
    ): List<String> {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(queryInfo, tProjectPipelineLabelInfo)
            val step = dslContext.select(PIPELINE_ID).from(this)
            if (!queryInfo.pipelineLabelIds.isNullOrEmpty()) {
                step.join(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions)
                .groupBy(PIPELINE_ID)
                .orderBy(AVG_COST_TIME.desc())
                .limit(metricsConfig.defaultLimitNum)
                .fetch()
                .map {
                    it.value1()
                }
        }
    }

    fun queryPipelineStageTrendInfo(
        dslContext: DSLContext,
        queryInfo: QueryPipelineStageTrendInfoQO
    ): Result<Record3<String, LocalDateTime, Long>> {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            val pipelineInfos = getStageTrendPipelineInfo(dslContext, queryInfo)
            val tProjectPipelineLabelInfo = TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO
            val conditions = getConditions(queryInfo, tProjectPipelineLabelInfo)
            conditions.add(PIPELINE_ID.`in`(pipelineInfos))
            val step = dslContext.select(
                    PIPELINE_NAME.`as`(BK_PIPELINE_NAME),
                    STATISTICS_TIME.`as`(BK_STATISTICS_TIME),
                    AVG_COST_TIME.`as`(BK_AVG_COST_TIME)
                    ).from(this)
            if (!queryInfo.pipelineLabelIds.isNullOrEmpty()) {
                step.join(tProjectPipelineLabelInfo)
                    .on(this.PIPELINE_ID.eq(tProjectPipelineLabelInfo.PIPELINE_ID))
            }
            return step.where(conditions)
                .groupBy(PIPELINE_ID, STATISTICS_TIME)
                .fetch()
        }
    }

    fun getStageTag(dslContext: DSLContext, projectId: String): MutableList<String> {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            return dslContext
                .select(STAGE_TAG_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(STAGE_TAG_NAME)
                .fetch()
                .map { it.value1() }
        }
    }

    private fun TPipelineStageOverviewData.getConditions(
        queryCondition: QueryPipelineStageTrendInfoQO,
        tProjectPipelineLabelInfo: TProjectPipelineLabelInfo
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        val pipelineIds = queryCondition.pipelineIds
        conditions.add(this.PROJECT_ID.eq(queryCondition.projectId))
        if (!pipelineIds.isNullOrEmpty()) {
            conditions.add(this.PIPELINE_ID.`in`(pipelineIds))
        }
        if (!queryCondition.pipelineLabelIds.isNullOrEmpty()) {
            conditions.add(tProjectPipelineLabelInfo.LABEL_ID.`in`(queryCondition.pipelineLabelIds))
        }
        val startDateTime = DateTimeUtil.stringToLocalDate(queryCondition.startTime)!!.atStartOfDay()
        val endDateTime = DateTimeUtil.stringToLocalDate(queryCondition.endTime)!!.atStartOfDay()
        if (startDateTime.isEqual(endDateTime)) {
            conditions.add(this.STATISTICS_TIME.eq(startDateTime))
        } else {
            conditions.add(this.STATISTICS_TIME.between(startDateTime, endDateTime))
        }
        conditions.add(this.STAGE_TAG_NAME.eq(queryCondition.stageTag))
        return conditions
    }
}
