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

import com.tencent.devops.metrics.pojo.po.SaveAtomFailDetailDataPO
import com.tencent.devops.metrics.pojo.po.SaveAtomFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.SaveAtomIndexStatisticsDailyPO
import com.tencent.devops.metrics.pojo.po.SaveAtomMonitorDailyPO
import com.tencent.devops.metrics.pojo.po.SaveAtomOverviewDataPO
import com.tencent.devops.metrics.pojo.po.SaveErrorCodeInfoPO
import com.tencent.devops.metrics.pojo.po.SavePipelineFailDetailDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineOverviewDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineStageOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomIndexStatisticsDailyPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineStageOverviewDataPO
import com.tencent.devops.model.metrics.tables.TAtomFailDetailData
import com.tencent.devops.model.metrics.tables.TAtomFailSummaryData
import com.tencent.devops.model.metrics.tables.TAtomIndexStatisticsDaily
import com.tencent.devops.model.metrics.tables.TAtomMonitorDataDaily
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TErrorCodeInfo
import com.tencent.devops.model.metrics.tables.TPipelineFailDetailData
import com.tencent.devops.model.metrics.tables.TPipelineFailSummaryData
import com.tencent.devops.model.metrics.tables.TPipelineOverviewData
import com.tencent.devops.model.metrics.tables.TPipelineStageOverviewData
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MetricsDataReportDao {

    fun savePipelineOverviewData(
        dslContext: DSLContext,
        savePipelineOverviewDataPO: SavePipelineOverviewDataPO
    ) {
        with(TPipelineOverviewData.T_PIPELINE_OVERVIEW_DATA) {
            dslContext.insertInto(this)
                .set(ID, savePipelineOverviewDataPO.id)
                .set(PROJECT_ID, savePipelineOverviewDataPO.projectId)
                .set(PIPELINE_ID, savePipelineOverviewDataPO.pipelineId)
                .set(PIPELINE_NAME, savePipelineOverviewDataPO.pipelineName)
                .set(CHANNEL_CODE, savePipelineOverviewDataPO.channelCode)
                .set(TOTAL_AVG_COST_TIME, savePipelineOverviewDataPO.totalAvgCostTime)
                .set(SUCCESS_AVG_COST_TIME, savePipelineOverviewDataPO.successAvgCostTime)
                .set(FAIL_AVG_COST_TIME, savePipelineOverviewDataPO.failAvgCostTime)
                .set(TOTAL_EXECUTE_COUNT, savePipelineOverviewDataPO.totalExecuteCount)
                .set(SUCCESS_EXECUTE_COUNT, savePipelineOverviewDataPO.successExecuteCount)
                .set(FAIL_EXECUTE_COUNT, savePipelineOverviewDataPO.failExecuteCount)
                .set(STATISTICS_TIME, savePipelineOverviewDataPO.statisticsTime)
                .set(CREATOR, savePipelineOverviewDataPO.creator)
                .set(MODIFIER, savePipelineOverviewDataPO.modifier)
                .set(UPDATE_TIME, savePipelineOverviewDataPO.updateTime)
                .set(CREATE_TIME, savePipelineOverviewDataPO.createTime)
                .execute()
        }
    }

    fun updatePipelineOverviewData(
        dslContext: DSLContext,
        updatePipelineOverviewDataPO: UpdatePipelineOverviewDataPO
    ) {
        with(TPipelineOverviewData.T_PIPELINE_OVERVIEW_DATA) {
            dslContext.update(this)
                .set(TOTAL_AVG_COST_TIME, updatePipelineOverviewDataPO.totalAvgCostTime)
                .set(SUCCESS_AVG_COST_TIME, updatePipelineOverviewDataPO.successAvgCostTime)
                .set(FAIL_AVG_COST_TIME, updatePipelineOverviewDataPO.failAvgCostTime)
                .set(TOTAL_EXECUTE_COUNT, updatePipelineOverviewDataPO.totalExecuteCount)
                .set(SUCCESS_EXECUTE_COUNT, updatePipelineOverviewDataPO.successExecuteCount)
                .set(FAIL_EXECUTE_COUNT, updatePipelineOverviewDataPO.failExecuteCount)
                .set(MODIFIER, updatePipelineOverviewDataPO.modifier)
                .set(UPDATE_TIME, updatePipelineOverviewDataPO.updateTime)
                .where(
                    PROJECT_ID.eq(updatePipelineOverviewDataPO.projectId)
                        .and(ID.eq(updatePipelineOverviewDataPO.id))
                )
                .execute()
        }
    }

    fun batchSavePipelineStageOverviewData(
        dslContext: DSLContext,
        savePipelineStageOverviewDataPOs: List<SavePipelineStageOverviewDataPO>
    ) {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            savePipelineStageOverviewDataPOs.forEach { savePipelineStageOverviewDataPO ->
                dslContext.insertInto(this)
                    .set(ID, savePipelineStageOverviewDataPO.id)
                    .set(PROJECT_ID, savePipelineStageOverviewDataPO.projectId)
                    .set(PIPELINE_ID, savePipelineStageOverviewDataPO.pipelineId)
                    .set(PIPELINE_NAME, savePipelineStageOverviewDataPO.pipelineName)
                    .set(CHANNEL_CODE, savePipelineStageOverviewDataPO.channelCode)
                    .set(STAGE_TAG_NAME, savePipelineStageOverviewDataPO.stageTagName)
                    .set(AVG_COST_TIME, savePipelineStageOverviewDataPO.avgCostTime)
                    .set(EXECUTE_COUNT, savePipelineStageOverviewDataPO.executeCount)
                    .set(STATISTICS_TIME, savePipelineStageOverviewDataPO.statisticsTime)
                    .set(CREATOR, savePipelineStageOverviewDataPO.creator)
                    .set(MODIFIER, savePipelineStageOverviewDataPO.modifier)
                    .set(UPDATE_TIME, savePipelineStageOverviewDataPO.updateTime)
                    .set(CREATE_TIME, savePipelineStageOverviewDataPO.createTime)
                    .execute()
            }
        }
    }

    fun batchUpdatePipelineStageOverviewData(
        dslContext: DSLContext,
        updatePipelineStageOverviewDataPOs: List<UpdatePipelineStageOverviewDataPO>
    ) {
        with(TPipelineStageOverviewData.T_PIPELINE_STAGE_OVERVIEW_DATA) {
            updatePipelineStageOverviewDataPOs.forEach { updatePipelineStageOverviewDataPO ->
                dslContext.update(this)
                    .set(AVG_COST_TIME, updatePipelineStageOverviewDataPO.avgCostTime)
                    .set(EXECUTE_COUNT, updatePipelineStageOverviewDataPO.executeCount)
                    .set(MODIFIER, updatePipelineStageOverviewDataPO.modifier)
                    .set(UPDATE_TIME, updatePipelineStageOverviewDataPO.updateTime)
                    .where(
                        PROJECT_ID.eq(updatePipelineStageOverviewDataPO.projectId)
                            .and(ID.eq(updatePipelineStageOverviewDataPO.id))
                    )
                    .execute()
            }
        }
    }

    fun savePipelineFailSummaryData(
        dslContext: DSLContext,
        savePipelineFailSummaryDataPO: SavePipelineFailSummaryDataPO
    ) {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            dslContext.insertInto(this)
                .set(ID, savePipelineFailSummaryDataPO.id)
                .set(PROJECT_ID, savePipelineFailSummaryDataPO.projectId)
                .set(PIPELINE_ID, savePipelineFailSummaryDataPO.pipelineId)
                .set(PIPELINE_NAME, savePipelineFailSummaryDataPO.pipelineName)
                .set(CHANNEL_CODE, savePipelineFailSummaryDataPO.channelCode)
                .set(ERROR_TYPE, savePipelineFailSummaryDataPO.errorType)
                .set(ERROR_COUNT, savePipelineFailSummaryDataPO.errorCount)
                .set(STATISTICS_TIME, savePipelineFailSummaryDataPO.statisticsTime)
                .set(CREATOR, savePipelineFailSummaryDataPO.creator)
                .set(MODIFIER, savePipelineFailSummaryDataPO.modifier)
                .set(UPDATE_TIME, savePipelineFailSummaryDataPO.updateTime)
                .set(CREATE_TIME, savePipelineFailSummaryDataPO.createTime)
                .execute()
        }
    }

    fun updatePipelineFailSummaryData(
        dslContext: DSLContext,
        updatePipelineFailSummaryDataPO: UpdatePipelineFailSummaryDataPO
    ) {
        with(TPipelineFailSummaryData.T_PIPELINE_FAIL_SUMMARY_DATA) {
            dslContext.update(this)
                .set(ERROR_COUNT, updatePipelineFailSummaryDataPO.errorCount)
                .set(MODIFIER, updatePipelineFailSummaryDataPO.modifier)
                .set(UPDATE_TIME, updatePipelineFailSummaryDataPO.updateTime)
                .where(
                    PROJECT_ID.eq(updatePipelineFailSummaryDataPO.projectId)
                        .and(ID.eq(updatePipelineFailSummaryDataPO.id))
                )
                .execute()
        }
    }

    fun savePipelineFailDetailData(
        dslContext: DSLContext,
        savePipelineFailDetailDataPO: SavePipelineFailDetailDataPO
    ) {
        with(TPipelineFailDetailData.T_PIPELINE_FAIL_DETAIL_DATA) {
            dslContext.insertInto(this)
                .set(ID, savePipelineFailDetailDataPO.id)
                .set(PROJECT_ID, savePipelineFailDetailDataPO.projectId)
                .set(PIPELINE_ID, savePipelineFailDetailDataPO.pipelineId)
                .set(PIPELINE_NAME, savePipelineFailDetailDataPO.pipelineName)
                .set(CHANNEL_CODE, savePipelineFailDetailDataPO.channelCode)
                .set(BUILD_ID, savePipelineFailDetailDataPO.buildId)
                .set(BUILD_NUM, savePipelineFailDetailDataPO.buildNum)
                .set(REPO_URL, savePipelineFailDetailDataPO.repoUrl)
                .set(BRANCH, savePipelineFailDetailDataPO.branch)
                .set(START_USER, savePipelineFailDetailDataPO.startUser)
                .set(START_TIME, savePipelineFailDetailDataPO.startTime)
                .set(END_TIME, savePipelineFailDetailDataPO.endTime)
                .set(ERROR_TYPE, savePipelineFailDetailDataPO.errorType)
                .set(ERROR_CODE, savePipelineFailDetailDataPO.errorCode)
                .set(ERROR_MSG, savePipelineFailDetailDataPO.errorMsg)
                .set(STATISTICS_TIME, savePipelineFailDetailDataPO.statisticsTime)
                .set(CREATOR, savePipelineFailDetailDataPO.creator)
                .set(MODIFIER, savePipelineFailDetailDataPO.modifier)
                .set(UPDATE_TIME, savePipelineFailDetailDataPO.updateTime)
                .set(CREATE_TIME, savePipelineFailDetailDataPO.createTime)
                .execute()
        }
    }

    fun batchSaveAtomOverviewData(
        dslContext: DSLContext,
        saveAtomOverviewDataPOs: List<SaveAtomOverviewDataPO>
    ) {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            saveAtomOverviewDataPOs.forEach { saveAtomOverviewDataPO ->
                dslContext.insertInto(this)
                    .set(ID, saveAtomOverviewDataPO.id)
                    .set(PROJECT_ID, saveAtomOverviewDataPO.projectId)
                    .set(PIPELINE_ID, saveAtomOverviewDataPO.pipelineId)
                    .set(PIPELINE_NAME, saveAtomOverviewDataPO.pipelineName)
                    .set(CHANNEL_CODE, saveAtomOverviewDataPO.channelCode)
                    .set(ATOM_CODE, saveAtomOverviewDataPO.atomCode)
                    .set(ATOM_NAME, saveAtomOverviewDataPO.atomName)
                    .set(CLASSIFY_CODE, saveAtomOverviewDataPO.classifyCode)
                    .set(CLASSIFY_NAME, saveAtomOverviewDataPO.classifyName)
                    .set(SUCCESS_RATE, saveAtomOverviewDataPO.successRate)
                    .set(AVG_COST_TIME, saveAtomOverviewDataPO.avgCostTime)
                    .set(TOTAL_EXECUTE_COUNT, saveAtomOverviewDataPO.totalExecuteCount)
                    .set(SUCCESS_EXECUTE_COUNT, saveAtomOverviewDataPO.successExecuteCount)
                    .set(FAIL_EXECUTE_COUNT, saveAtomOverviewDataPO.failExecuteCount)
                    .set(STATISTICS_TIME, saveAtomOverviewDataPO.statisticsTime)
                    .set(CREATOR, saveAtomOverviewDataPO.creator)
                    .set(MODIFIER, saveAtomOverviewDataPO.modifier)
                    .set(UPDATE_TIME, saveAtomOverviewDataPO.updateTime)
                    .set(CREATE_TIME, saveAtomOverviewDataPO.createTime)
                    .execute()
            }
        }
    }

    fun saveAtomIndexStatisticsDailyData(
        dslContext: DSLContext,
        saveAtomIndexStatisticsDailyPO: SaveAtomIndexStatisticsDailyPO
    ) {
        with(TAtomIndexStatisticsDaily.T_ATOM_INDEX_STATISTICS_DAILY) {
            dslContext.insertInto(this)
                .set(ID, saveAtomIndexStatisticsDailyPO.id)
                .set(ATOM_CODE, saveAtomIndexStatisticsDailyPO.atomCode)
                .set(FAIL_EXECUTE_COUNT, saveAtomIndexStatisticsDailyPO.failExecuteCount)
                .set(FAIL_COMPLIANCE_COUNT, saveAtomIndexStatisticsDailyPO.failComplianceCount)
                .set(STATISTICS_TIME, saveAtomIndexStatisticsDailyPO.statisticsTime)
                .set(CREATOR, saveAtomIndexStatisticsDailyPO.creator)
                .set(MODIFIER, saveAtomIndexStatisticsDailyPO.modifier)
                .set(UPDATE_TIME, saveAtomIndexStatisticsDailyPO.updateTime)
                .set(CREATE_TIME, saveAtomIndexStatisticsDailyPO.createTime)
                .execute()
        }
    }

    fun getAtomIndexStatisticsDailyData(
        dslContext: DSLContext,
        atomCode: String,
        statisticsTime: LocalDateTime
    ): Long? {
        with(TAtomIndexStatisticsDaily.T_ATOM_INDEX_STATISTICS_DAILY) {
            return dslContext.select(ID).from(this)
                .where(ATOM_CODE.eq(atomCode))
                .and(STATISTICS_TIME.eq(statisticsTime))
                .fetchOne(0, Long::class.java)
        }
    }

    fun updateAtomIndexStatisticsDailyData(
        dslContext: DSLContext,
        updateAtomIndexStatisticsDailyPO: UpdateAtomIndexStatisticsDailyPO
    ) {
        with(TAtomIndexStatisticsDaily.T_ATOM_INDEX_STATISTICS_DAILY) {
            dslContext.update(this)
                .set(FAIL_EXECUTE_COUNT, updateAtomIndexStatisticsDailyPO.failExecuteCount)
                .set(FAIL_COMPLIANCE_COUNT, updateAtomIndexStatisticsDailyPO.failComplianceCount)
                .set(MODIFIER, updateAtomIndexStatisticsDailyPO.modifier)
                .set(UPDATE_TIME, updateAtomIndexStatisticsDailyPO.updateTime)
                .where(ID.eq(updateAtomIndexStatisticsDailyPO.id))
                .execute()
        }
    }

    fun batchUpdateAtomOverviewData(
        dslContext: DSLContext,
        updateAtomOverviewDataPOs: List<UpdateAtomOverviewDataPO>
    ) {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            updateAtomOverviewDataPOs.forEach { updateAtomOverviewDataPO ->
                dslContext.update(this)
                    .set(SUCCESS_RATE, updateAtomOverviewDataPO.successRate)
                    .set(AVG_COST_TIME, updateAtomOverviewDataPO.avgCostTime)
                    .set(TOTAL_EXECUTE_COUNT, updateAtomOverviewDataPO.totalExecuteCount)
                    .set(SUCCESS_EXECUTE_COUNT, updateAtomOverviewDataPO.successExecuteCount)
                    .set(FAIL_EXECUTE_COUNT, updateAtomOverviewDataPO.failExecuteCount)
                    .set(MODIFIER, updateAtomOverviewDataPO.modifier)
                    .set(UPDATE_TIME, updateAtomOverviewDataPO.updateTime)
                    .where(
                        PROJECT_ID.eq(updateAtomOverviewDataPO.projectId)
                            .and(ID.eq(updateAtomOverviewDataPO.id))
                    )
                    .execute()
            }
        }
    }

    fun batchSaveAtomFailSummaryData(
        dslContext: DSLContext,
        saveAtomFailSummaryDataPOs: List<SaveAtomFailSummaryDataPO>
    ) {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            saveAtomFailSummaryDataPOs.forEach { saveAtomFailSummaryDataPO ->
                dslContext.insertInto(this)
                    .set(ID, saveAtomFailSummaryDataPO.id)
                    .set(PROJECT_ID, saveAtomFailSummaryDataPO.projectId)
                    .set(PIPELINE_ID, saveAtomFailSummaryDataPO.pipelineId)
                    .set(PIPELINE_NAME, saveAtomFailSummaryDataPO.pipelineName)
                    .set(CHANNEL_CODE, saveAtomFailSummaryDataPO.channelCode)
                    .set(ATOM_CODE, saveAtomFailSummaryDataPO.atomCode)
                    .set(ATOM_NAME, saveAtomFailSummaryDataPO.atomName)
                    .set(CLASSIFY_CODE, saveAtomFailSummaryDataPO.classifyCode)
                    .set(CLASSIFY_NAME, saveAtomFailSummaryDataPO.classifyName)
                    .set(ERROR_TYPE, saveAtomFailSummaryDataPO.errorType)
                    .set(ERROR_COUNT, saveAtomFailSummaryDataPO.errorCount)
                    .set(STATISTICS_TIME, saveAtomFailSummaryDataPO.statisticsTime)
                    .set(CREATOR, saveAtomFailSummaryDataPO.creator)
                    .set(MODIFIER, saveAtomFailSummaryDataPO.modifier)
                    .set(UPDATE_TIME, saveAtomFailSummaryDataPO.updateTime)
                    .set(CREATE_TIME, saveAtomFailSummaryDataPO.createTime)
                    .execute()
            }
        }
    }

    fun batchUpdateAtomFailSummaryData(
        dslContext: DSLContext,
        updateAtomFailSummaryDataPOs: List<UpdateAtomFailSummaryDataPO>
    ) {
        with(TAtomFailSummaryData.T_ATOM_FAIL_SUMMARY_DATA) {
            updateAtomFailSummaryDataPOs.forEach { updateAtomFailSummaryDataPO ->
                dslContext.update(this)
                    .set(ERROR_COUNT, updateAtomFailSummaryDataPO.errorCount)
                    .set(MODIFIER, updateAtomFailSummaryDataPO.modifier)
                    .set(UPDATE_TIME, updateAtomFailSummaryDataPO.updateTime)
                    .where(
                        PROJECT_ID.eq(updateAtomFailSummaryDataPO.projectId)
                            .and(ID.eq(updateAtomFailSummaryDataPO.id))
                    )
                    .execute()
            }
        }
    }

    fun batchSaveAtomFailDetailData(
        dslContext: DSLContext,
        saveAtomFailDetailDataPOs: List<SaveAtomFailDetailDataPO>
    ) {
        with(TAtomFailDetailData.T_ATOM_FAIL_DETAIL_DATA) {
            saveAtomFailDetailDataPOs.forEach { saveAtomFailDetailDataPO ->
                dslContext.insertInto(this)
                    .set(ID, saveAtomFailDetailDataPO.id)
                    .set(PROJECT_ID, saveAtomFailDetailDataPO.projectId)
                    .set(PIPELINE_ID, saveAtomFailDetailDataPO.pipelineId)
                    .set(PIPELINE_NAME, saveAtomFailDetailDataPO.pipelineName)
                    .set(CHANNEL_CODE, saveAtomFailDetailDataPO.channelCode)
                    .set(BUILD_ID, saveAtomFailDetailDataPO.buildId)
                    .set(BUILD_NUM, saveAtomFailDetailDataPO.buildNum)
                    .set(ATOM_CODE, saveAtomFailDetailDataPO.atomCode)
                    .set(ATOM_NAME, saveAtomFailDetailDataPO.atomName)
                    .set(ATOM_POSITION, saveAtomFailDetailDataPO.atomPosition)
                    .set(CLASSIFY_CODE, saveAtomFailDetailDataPO.classifyCode)
                    .set(CLASSIFY_NAME, saveAtomFailDetailDataPO.classifyName)
                    .set(START_USER, saveAtomFailDetailDataPO.startUser)
                    .set(START_TIME, saveAtomFailDetailDataPO.startTime)
                    .set(END_TIME, saveAtomFailDetailDataPO.endTime)
                    .set(ERROR_TYPE, saveAtomFailDetailDataPO.errorType)
                    .set(ERROR_CODE, saveAtomFailDetailDataPO.errorCode)
                    .set(ERROR_MSG, saveAtomFailDetailDataPO.errorMsg)
                    .set(STATISTICS_TIME, saveAtomFailDetailDataPO.statisticsTime)
                    .set(CREATOR, saveAtomFailDetailDataPO.creator)
                    .set(MODIFIER, saveAtomFailDetailDataPO.modifier)
                    .set(UPDATE_TIME, saveAtomFailDetailDataPO.updateTime)
                    .set(CREATE_TIME, saveAtomFailDetailDataPO.createTime)
                    .execute()
            }
        }
    }

    fun batchSaveErrorCodeInfo(
        dslContext: DSLContext,
        saveErrorCodeInfoPOs: Set<SaveErrorCodeInfoPO>
    ) {
        saveErrorCodeInfoPOs.forEach { saveErrorCodeInfoPO ->
            saveErrorCodeInfo(dslContext, saveErrorCodeInfoPO)
        }
    }

    fun saveErrorCodeInfo(
        dslContext: DSLContext,
        saveErrorCodeInfoPO: SaveErrorCodeInfoPO
    ) {
        with(TErrorCodeInfo.T_ERROR_CODE_INFO) {
            dslContext.insertInto(this)
                .set(ID, saveErrorCodeInfoPO.id)
                .set(ERROR_TYPE, saveErrorCodeInfoPO.errorType)
                .set(ERROR_CODE, saveErrorCodeInfoPO.errorCode)
                .set(ERROR_MSG, saveErrorCodeInfoPO.errorMsg)
                .set(CREATOR, saveErrorCodeInfoPO.creator)
                .set(MODIFIER, saveErrorCodeInfoPO.modifier)
                .set(UPDATE_TIME, saveErrorCodeInfoPO.updateTime)
                .set(CREATE_TIME, saveErrorCodeInfoPO.createTime)
                .set(ATOM_CODE, saveErrorCodeInfoPO.atomCode)
                .onDuplicateKeyUpdate()
                .set(ERROR_MSG, saveErrorCodeInfoPO.errorMsg)
                .set(MODIFIER, saveErrorCodeInfoPO.modifier)
                .set(UPDATE_TIME, saveErrorCodeInfoPO.updateTime)
                .execute()
        }
    }

    fun saveAtomMonitorDailyData(
        dslContext: DSLContext,
        saveAtomMonitorDailyPO: SaveAtomMonitorDailyPO
    ) {
        with(TAtomMonitorDataDaily.T_ATOM_MONITOR_DATA_DAILY) {
            dslContext.insertInto(this)
                .set(ID, saveAtomMonitorDailyPO.id)
                .set(ERROR_TYPE, saveAtomMonitorDailyPO.errorType)
                .set(EXECUTE_COUNT, saveAtomMonitorDailyPO.executeCount)
                .set(STATISTICS_TIME, saveAtomMonitorDailyPO.statisticsTime)
                .set(CREATOR, saveAtomMonitorDailyPO.creator)
                .set(MODIFIER, saveAtomMonitorDailyPO.modifier)
                .set(UPDATE_TIME, saveAtomMonitorDailyPO.updateTime)
                .set(CREATE_TIME, saveAtomMonitorDailyPO.createTime)
                .set(ATOM_CODE, saveAtomMonitorDailyPO.atomCode)
                .onDuplicateKeyUpdate()
                .set(EXECUTE_COUNT, EXECUTE_COUNT + 1)
                .set(MODIFIER, saveAtomMonitorDailyPO.modifier)
                .set(UPDATE_TIME, saveAtomMonitorDailyPO.updateTime)
                .execute()
        }
    }
}
