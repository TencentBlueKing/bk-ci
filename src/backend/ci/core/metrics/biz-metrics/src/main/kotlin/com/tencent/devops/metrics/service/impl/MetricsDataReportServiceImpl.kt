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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.pojo.BuildEndPipelineMetricsData
import com.tencent.devops.common.api.pojo.BuildEndTaskMetricsData
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.MetricsDataQueryDao
import com.tencent.devops.metrics.dao.MetricsDataReportDao
import com.tencent.devops.metrics.pojo.po.SaveAtomFailDetailDataPO
import com.tencent.devops.metrics.pojo.po.SaveAtomFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.SaveAtomOverviewDataPO
import com.tencent.devops.metrics.pojo.po.SaveErrorCodeInfoPO
import com.tencent.devops.metrics.pojo.po.SavePipelineFailDetailDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineOverviewDataPO
import com.tencent.devops.metrics.pojo.po.SavePipelineStageOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineStageOverviewDataPO
import com.tencent.devops.metrics.service.MetricsDataReportService
import com.tencent.devops.model.metrics.tables.records.TAtomOverviewDataRecord
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.math.roundToLong

@Service
class MetricsDataReportServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val metricsDataQueryDao: MetricsDataQueryDao,
    private val metricsDataReportDao: MetricsDataReportDao,
    private val client: Client,
    private val redisOperation: RedisOperation
) : MetricsDataReportService {

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsDataReportService::class.java)
        private fun metricsDataReportKey(pipelineId: String) = "metricsDataReport:$pipelineId"
    }

    override fun metricsDataReport(buildEndPipelineMetricsData: BuildEndPipelineMetricsData): Boolean {
        // 判断流水线是否在db中有记录
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val buildId = buildEndPipelineMetricsData.buildId
        logger.info("[$projectId|$pipelineId|$buildId]|start metricsDataReport")
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val currentTime = LocalDateTime.now()
        val saveErrorCodeInfoPOs = mutableSetOf<SaveErrorCodeInfoPO>()
        val lock = RedisLock(redisOperation, metricsDataReportKey(pipelineId), 20)
        try {
            // 上锁保证数据计算安全
            lock.lock()
            val savePipelineStageOverviewDataPOs = mutableListOf<SavePipelineStageOverviewDataPO>()
            val updatePipelineStageOverviewDataPOs = mutableListOf<UpdatePipelineStageOverviewDataPO>()
            val saveAtomOverviewDataPOs = mutableListOf<SaveAtomOverviewDataPO>()
            val updateAtomOverviewDataPOs = mutableListOf<UpdateAtomOverviewDataPO>()
            val saveAtomFailSummaryDataPOs = mutableListOf<SaveAtomFailSummaryDataPO>()
            val updateAtomFailSummaryDataPOs = mutableListOf<UpdateAtomFailSummaryDataPO>()
            val saveAtomFailDetailDataPOs = mutableListOf<SaveAtomFailDetailDataPO>()
            buildEndPipelineMetricsData.stages.forEach { stage ->
                val stageTagNames = stage.stageTagNames?.toMutableList()
                // 上报stage构建数据
                pipelineStageOverviewDataReport(
                    stageTagNames = stageTagNames,
                    stageCostTime = stage.costTime,
                    buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                    updatePipelineStageOverviewDataPOs = updatePipelineStageOverviewDataPOs,
                    currentTime = currentTime,
                    savePipelineStageOverviewDataPOs = savePipelineStageOverviewDataPOs
                )
                stage.containers.forEach { container ->
                    val atomOverviewDataRecords = metricsDataQueryDao.getAtomOverviewDatas(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        statisticsTime = statisticsTime,
                        atomCodes = container.atomCodes
                    )
                    container.tasks.forEach { task ->
                        atomOverviewDataReport(
                            buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                            taskMetricsData = task,
                            atomOverviewDataRecords = atomOverviewDataRecords,
                            updateAtomOverviewDataPOs = updateAtomOverviewDataPOs,
                            currentTime = currentTime,
                            saveAtomOverviewDataPOs = saveAtomOverviewDataPOs
                        )
                        atomFailSummaryDataReport(
                            buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                            taskMetricsData = task,
                            updateAtomFailSummaryDataPOs = updateAtomFailSummaryDataPOs,
                            currentTime = currentTime,
                            saveAtomFailSummaryDataPOs = saveAtomFailSummaryDataPOs,
                            saveAtomFailDetailDataPOs = saveAtomFailDetailDataPOs,
                            saveErrorCodeInfoPOs = saveErrorCodeInfoPOs
                        )
                    }
                }
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 1、上报流水线构建数据
                pipelineOverviewDataReport(
                    dslContext = context,
                    buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                    currentTime = currentTime
                )
                pipelineFailDataReport(
                    dslContext = context,
                    buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                    currentTime = currentTime,
                    saveErrorCodeInfoPOs = saveErrorCodeInfoPOs
                )
                // 2、上报流水线stage和task构建数据
                if (savePipelineStageOverviewDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchSavePipelineStageOverviewData(context, savePipelineStageOverviewDataPOs)
                }
                if (updatePipelineStageOverviewDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchUpdatePipelineStageOverviewData(context, updatePipelineStageOverviewDataPOs)
                }
                if (saveAtomOverviewDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchSaveAtomOverviewData(context, saveAtomOverviewDataPOs)
                }
                if (updateAtomOverviewDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchUpdateAtomOverviewData(context, updateAtomOverviewDataPOs)
                }
                if (saveAtomFailSummaryDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchSaveAtomFailSummaryData(context, saveAtomFailSummaryDataPOs)
                }
                if (updateAtomFailSummaryDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchUpdateAtomFailSummaryData(context, updateAtomFailSummaryDataPOs)
                }
                if (saveAtomFailDetailDataPOs.isNotEmpty()) {
                    metricsDataReportDao.batchSaveAtomFailDetailData(dslContext, saveAtomFailDetailDataPOs)
                }
                if (saveErrorCodeInfoPOs.isNotEmpty()) {
                    metricsDataReportDao.batchSaveErrorCodeInfo(context, saveErrorCodeInfoPOs)
                }
            }
            logger.info("[$projectId|$pipelineId|$buildId]|end metricsDataReport")
        } finally {
            lock.unlock()
        }
        return true
    }

    private fun atomFailSummaryDataReport(
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        taskMetricsData: BuildEndTaskMetricsData,
        updateAtomFailSummaryDataPOs: MutableList<UpdateAtomFailSummaryDataPO>,
        currentTime: LocalDateTime,
        saveAtomFailSummaryDataPOs: MutableList<SaveAtomFailSummaryDataPO>,
        saveAtomFailDetailDataPOs: MutableList<SaveAtomFailDetailDataPO>,
        saveErrorCodeInfoPOs: MutableSet<SaveErrorCodeInfoPO>
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val taskErrorType = taskMetricsData.errorType ?: return
        val atomFailSummaryDataRecord = metricsDataQueryDao.getAtomFailSummaryData(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            statisticsTime = statisticsTime,
            errorType = taskErrorType,
            atomCode = taskMetricsData.atomCode
        )
        if (atomFailSummaryDataRecord != null) {
            updateAtomFailSummaryDataPOs.add(
                UpdateAtomFailSummaryDataPO(
                    id = atomFailSummaryDataRecord.id,
                    projectId = projectId,
                    errorCount = atomFailSummaryDataRecord.errorCount + 1,
                    modifier = startUser,
                    updateTime = currentTime
                )
            )
        } else {
            saveAtomFailSummaryDataPOs.add(
                SaveAtomFailSummaryDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("ATOM_FAIL_SUMMARY_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    atomCode = taskMetricsData.atomCode,
                    atomName = taskMetricsData.atomName,
                    classifyCode = taskMetricsData.classifyCode,
                    classifyName = taskMetricsData.classifyName,
                    errorType = taskErrorType,
                    errorCount = 1,
                    statisticsTime = statisticsTime,
                    creator = startUser,
                    modifier = startUser,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }
        // 添加插件失败详情
        saveAtomFailDetailDataPOs.add(
            SaveAtomFailDetailDataPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("ATOM_FAIL_DETAIL_DATA").data ?: 0,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                buildId = buildEndPipelineMetricsData.buildId,
                buildNum = buildEndPipelineMetricsData.buildNum,
                atomCode = taskMetricsData.atomCode,
                atomName = taskMetricsData.atomName,
                classifyCode = taskMetricsData.classifyCode,
                classifyName = taskMetricsData.classifyName,
                startUser = startUser,
                startTime = taskMetricsData.startTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                endTime = taskMetricsData.endTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                errorType = taskErrorType,
                errorCode = taskMetricsData.errorCode,
                errorMsg = taskMetricsData.errorMsg,
                statisticsTime = statisticsTime,
                creator = startUser,
                modifier = startUser,
                createTime = currentTime,
                updateTime = currentTime
            )
        )
        // 添加错误信息
        val taskErrorCode = taskMetricsData.errorCode
        if (taskErrorCode != null) {
            addErrorCodeInfo(
                saveErrorCodeInfoPOs = saveErrorCodeInfoPOs,
                errorType = taskErrorType,
                errorCode = taskErrorCode,
                errorMsg = taskMetricsData.errorMsg,
                startUser = startUser,
                currentTime = currentTime
            )
        }
    }

    private fun atomOverviewDataReport(
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        taskMetricsData: BuildEndTaskMetricsData,
        atomOverviewDataRecords: Result<TAtomOverviewDataRecord>?,
        updateAtomOverviewDataPOs: MutableList<UpdateAtomOverviewDataPO>,
        currentTime: LocalDateTime,
        saveAtomOverviewDataPOs: MutableList<SaveAtomOverviewDataPO>
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val taskSuccessFlag = taskMetricsData.successFlag
        val atomOverviewDataRecord = atomOverviewDataRecords?.filter { it.atomCode == taskMetricsData.atomCode }?.get(0)
        if (atomOverviewDataRecord != null) {
            // 更新db中已存在的插件统计记录数据
            val originAvgCostTime = atomOverviewDataRecord.avgCostTime ?: 0L
            val originTotalExecuteCount = atomOverviewDataRecord.totalExecuteCount
            val currentTotalExecuteCount = atomOverviewDataRecord.totalExecuteCount + 1
            val currentTotalCostTime = originAvgCostTime * originTotalExecuteCount + taskMetricsData.costTime
            val currentAvgCostTime = currentTotalCostTime.toDouble().div(currentTotalExecuteCount).roundToLong()
            val currentSuccessExecuteCount = if (taskSuccessFlag) {
                atomOverviewDataRecord.successExecuteCount + 1
            } else {
                atomOverviewDataRecord.successExecuteCount
            }
            val currentFailExecuteCount = if (taskSuccessFlag) {
                atomOverviewDataRecord.failExecuteCount
            } else {
                atomOverviewDataRecord.failExecuteCount + 1
            }
            val currentSuccessRate = currentSuccessExecuteCount.toBigDecimal().div(currentTotalExecuteCount.toBigDecimal())
            updateAtomOverviewDataPOs.add(
                UpdateAtomOverviewDataPO(
                    id = atomOverviewDataRecord.id,
                    projectId = projectId,
                    successRate = String.format("%.2f", currentSuccessRate).toBigDecimal(),
                    avgCostTime = currentAvgCostTime,
                    totalExecuteCount = currentTotalExecuteCount,
                    successExecuteCount = currentSuccessExecuteCount,
                    failExecuteCount = currentFailExecuteCount,
                    modifier = startUser,
                    updateTime = currentTime
                )
            )
        } else {
            val successRate = if (taskSuccessFlag) {
                BigDecimal(100.00)
            } else {
                BigDecimal(0.00)
            }
            saveAtomOverviewDataPOs.add(
                SaveAtomOverviewDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("ATOM_OVERVIEW_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    atomCode = taskMetricsData.atomCode,
                    atomName = taskMetricsData.atomName,
                    classifyCode = taskMetricsData.classifyCode,
                    classifyName = taskMetricsData.classifyName,
                    successRate = successRate,
                    avgCostTime = taskMetricsData.costTime,
                    totalExecuteCount = 1,
                    successExecuteCount = if (taskSuccessFlag) 1 else 0,
                    failExecuteCount = if (taskSuccessFlag) 0 else 1,
                    statisticsTime = DateTimeUtil.stringToLocalDateTime(
                        dateTimeStr = buildEndPipelineMetricsData.statisticsTime,
                        formatStr = YYYY_MM_DD
                    ),
                    creator = startUser,
                    modifier = startUser,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }
    }

    private fun pipelineStageOverviewDataReport(
        stageTagNames: MutableList<String>?,
        stageCostTime: Long,
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        updatePipelineStageOverviewDataPOs: MutableList<UpdatePipelineStageOverviewDataPO>,
        currentTime: LocalDateTime,
        savePipelineStageOverviewDataPOs: MutableList<SavePipelineStageOverviewDataPO>
    ) {
        if (stageTagNames.isNullOrEmpty()) {
            return
        }
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val stageOverviewDataRecords = metricsDataQueryDao.getPipelineStageOverviewDatas(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            statisticsTime = statisticsTime,
            stageTagNames = stageTagNames
        )
        val stageOverviewDataRecord = stageOverviewDataRecords?.get(0)
        if (stageOverviewDataRecord != null) {
            val originStageAvgCostTime = stageOverviewDataRecord.avgCostTime ?: 0L
            val originStageExecuteCount = stageOverviewDataRecord.executeCount
            val currentStageExecuteCount = stageOverviewDataRecord.executeCount + 1
            val currentStageTotalCostTime = originStageAvgCostTime * originStageExecuteCount + stageCostTime
            val currentStageAvgCostTime = currentStageTotalCostTime.toDouble().div(currentStageExecuteCount).roundToLong()
            // 更新db中已存在的stage统计记录数据
            stageOverviewDataRecords.forEach { tmpStageOverviewDataRecord ->
                updatePipelineStageOverviewDataPOs.add(
                    UpdatePipelineStageOverviewDataPO(
                        id = tmpStageOverviewDataRecord.id,
                        projectId = projectId,
                        avgCostTime = currentStageAvgCostTime,
                        executeCount = currentStageExecuteCount,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            }
            val updateStageTagNames = stageOverviewDataRecords.map { it.stageTagName }
            // 排除db中已存在的stage统计记录
            stageTagNames.removeAll(updateStageTagNames)
        }
        stageTagNames.forEach { stageTagName ->
            savePipelineStageOverviewDataPOs.add(
                SavePipelineStageOverviewDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_STAGE_DETAIL_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    stageTagName = stageTagName,
                    avgCostTime = stageCostTime,
                    executeCount = 1,
                    statisticsTime = statisticsTime,
                    creator = startUser,
                    modifier = startUser,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }
    }

    private fun pipelineFailDataReport(
        dslContext: DSLContext,
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime,
        saveErrorCodeInfoPOs: MutableSet<SaveErrorCodeInfoPO>
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val buildId = buildEndPipelineMetricsData.buildId
        val buildNum = buildEndPipelineMetricsData.buildNum // 构建序号
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val buildSuccessFlag = buildEndPipelineMetricsData.successFlag // 流水线构建是否成功标识
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val buildErrorType = buildEndPipelineMetricsData.errorType
        if (!buildSuccessFlag && buildErrorType != null) {
            // 插入流水线失败汇总数据
            val pipelineFailSummaryDataRecord = metricsDataQueryDao.getPipelineFailSummaryData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime,
                errorType = buildErrorType
            )
            if (pipelineFailSummaryDataRecord == null) {
                val savePipelineFailSummaryDataPO = SavePipelineFailSummaryDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_FAIL_SUMMARY_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    errorType = buildErrorType,
                    errorCount = 1,
                    statisticsTime = statisticsTime,
                    creator = startUser,
                    modifier = startUser,
                    createTime = currentTime,
                    updateTime = currentTime
                )
                metricsDataReportDao.savePipelineFailSummaryData(dslContext, savePipelineFailSummaryDataPO)
            } else {
                val updatePipelineFailSummaryDataPO = UpdatePipelineFailSummaryDataPO(
                    id = pipelineFailSummaryDataRecord.id,
                    projectId = projectId,
                    errorCount = pipelineFailSummaryDataRecord.errorCount + 1,
                    modifier = startUser,
                    updateTime = currentTime
                )
                metricsDataReportDao.updatePipelineFailSummaryData(dslContext, updatePipelineFailSummaryDataPO)
            }
            // 插入流水线失败详情数据
            val savePipelineFailDetailDataPO = SavePipelineFailDetailDataPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_FAIL_DETAIL_DATA").data ?: 0,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                buildId = buildId,
                buildNum = buildNum,
                repoUrl = buildEndPipelineMetricsData.repoUrl,
                branch = buildEndPipelineMetricsData.branch,
                startUser = startUser,
                startTime = buildEndPipelineMetricsData.startTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                endTime = buildEndPipelineMetricsData.endTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                errorType = buildEndPipelineMetricsData.errorType,
                errorCode = buildEndPipelineMetricsData.errorCode,
                errorMsg = buildEndPipelineMetricsData.errorMsg,
                statisticsTime = statisticsTime,
                creator = startUser,
                modifier = startUser,
                createTime = currentTime,
                updateTime = currentTime
            )
            metricsDataReportDao.savePipelineFailDetailData(dslContext, savePipelineFailDetailDataPO)
            // 添加错误信息
            val buildErrorCode = buildEndPipelineMetricsData.errorCode
            if (buildErrorCode != null) {
                addErrorCodeInfo(
                    saveErrorCodeInfoPOs = saveErrorCodeInfoPOs,
                    errorType = buildErrorType,
                    errorCode = buildErrorCode,
                    errorMsg = buildEndPipelineMetricsData.errorMsg,
                    startUser = startUser,
                    currentTime = currentTime
                )
            }
        }
    }

    private fun pipelineOverviewDataReport(
        dslContext: DSLContext,
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val buildSuccessFlag = buildEndPipelineMetricsData.successFlag // 流水线构建是否成功标识
        val pipelineBuildCostTime = buildEndPipelineMetricsData.costTime // 流水线构建所耗时间
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val pipelineOverviewDataRecord = metricsDataQueryDao.getPipelineOverviewData(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = buildEndPipelineMetricsData.pipelineId,
            statisticsTime = statisticsTime
        )
        if (pipelineOverviewDataRecord == null) {
            // db没有记录则插入记录
            val savePipelineOverviewDataPO = SavePipelineOverviewDataPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_OVERVIEW_DATA").data ?: 0,
                projectId = projectId,
                pipelineId = buildEndPipelineMetricsData.pipelineId,
                pipelineName = buildEndPipelineMetricsData.pipelineName,
                totalAvgCostTime = pipelineBuildCostTime,
                successAvgCostTime = if (buildSuccessFlag) pipelineBuildCostTime else null,
                failAvgCostTime = if (buildSuccessFlag) null else pipelineBuildCostTime,
                totalExecuteCount = 1,
                successExecuteCount = if (buildSuccessFlag) 1 else 0,
                failExecuteCount = if (buildSuccessFlag) 0 else 1,
                statisticsTime = statisticsTime,
                creator = startUser,
                modifier = startUser,
                createTime = currentTime,
                updateTime = currentTime
            )
            metricsDataReportDao.savePipelineOverviewData(dslContext, savePipelineOverviewDataPO)
        } else {
            // 更新db中原有记录数据
            val originTotalAvgCostTime = pipelineOverviewDataRecord.totalAvgCostTime ?: 0L
            val originSuccessAvgCostTime = pipelineOverviewDataRecord.successAvgCostTime ?: 0L
            val originFailAvgCostTime = pipelineOverviewDataRecord.failAvgCostTime ?: 0L
            val originTotalExecuteCount = pipelineOverviewDataRecord.totalExecuteCount
            val originSuccessExecuteCount = pipelineOverviewDataRecord.successExecuteCount
            val originFailExecuteCount = pipelineOverviewDataRecord.failExecuteCount
            val currentTotalExecuteCount = originTotalExecuteCount + 1
            val currentSuccessExecuteCount = if (buildSuccessFlag) {
                originSuccessExecuteCount + 1
            } else {
                originSuccessExecuteCount
            }
            val currentFailExecuteCount = if (buildSuccessFlag) {
                originFailExecuteCount
            } else {
                originFailExecuteCount + 1
            }
            // 计算平均耗时和执行次数
            val currentTotalCostTime = originTotalAvgCostTime * originTotalExecuteCount + pipelineBuildCostTime
            val currentTotalAvgCostTime = currentTotalCostTime.toDouble().div(currentTotalExecuteCount).roundToLong()
            val currentSuccessAvgCostTime = if (buildSuccessFlag) {
                val currentSuccessCostTime = originSuccessAvgCostTime * originSuccessExecuteCount + pipelineBuildCostTime
                currentSuccessCostTime.toDouble().div(currentSuccessExecuteCount).roundToLong()
            } else {
                originSuccessAvgCostTime
            }
            val currentFailAvgCostTime = if (buildSuccessFlag) {
                originFailAvgCostTime
            } else {
                val currentFailCostTime = originFailAvgCostTime * originFailExecuteCount + pipelineBuildCostTime
                currentFailCostTime.toDouble().div(currentFailExecuteCount).roundToLong()
            }
            val updatePipelineOverviewDataPO = UpdatePipelineOverviewDataPO(
                id = pipelineOverviewDataRecord.id,
                projectId = projectId,
                totalAvgCostTime = currentTotalAvgCostTime,
                successAvgCostTime = currentSuccessAvgCostTime,
                failAvgCostTime = currentFailAvgCostTime,
                totalExecuteCount = currentTotalExecuteCount,
                successExecuteCount = currentSuccessExecuteCount,
                failExecuteCount = currentFailExecuteCount,
                modifier = startUser,
                updateTime = currentTime
            )
            metricsDataReportDao.updatePipelineOverviewData(dslContext, updatePipelineOverviewDataPO)
        }
    }

    private fun addErrorCodeInfo(
        saveErrorCodeInfoPOs: MutableSet<SaveErrorCodeInfoPO>,
        errorType: Int,
        errorCode: Int,
        errorMsg: String?,
        startUser: String,
        currentTime: LocalDateTime
    ) {
        saveErrorCodeInfoPOs.add(
            SaveErrorCodeInfoPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("METRICS_ERROR_CODE_INFO").data ?: 0,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg,
                creator = startUser,
                modifier = startUser,
                createTime = currentTime,
                updateTime = currentTime
            )
        )
    }
}
