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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.measure.BuildEndPipelineMetricsData
import com.tencent.devops.common.event.pojo.measure.BuildEndTaskMetricsData
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.MetricsDataQueryDao
import com.tencent.devops.metrics.dao.MetricsDataReportDao
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
import com.tencent.devops.metrics.service.MetricsDataClearService
import com.tencent.devops.metrics.service.MetricsDataReportService
import com.tencent.devops.metrics.utils.ErrorCodeInfoCacheUtil
import com.tencent.devops.model.metrics.tables.records.TAtomFailSummaryDataRecord
import com.tencent.devops.model.metrics.tables.records.TAtomOverviewDataRecord
import com.tencent.devops.model.metrics.tables.records.TPipelineFailSummaryDataRecord
import com.tencent.devops.model.metrics.tables.records.TPipelineOverviewDataRecord
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.common.enums.ErrorCodeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.math.BigDecimal
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.exception.TooManyRowsException
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.roundToLong

@Service
@Suppress("ComplexMethod", "NestedBlockDepth", "LongMethod", "LongParameterList")
class MetricsDataReportServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val metricsDataQueryDao: MetricsDataQueryDao,
    private val metricsDataReportDao: MetricsDataReportDao,
    private val metricsDataClearService: MetricsDataClearService,
    private val client: Client,
    private val redisOperation: RedisOperation
) : MetricsDataReportService {

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsDataReportService::class.java)
        private fun metricsDataReportKey(key: String) = "metricsDataReport:$key"
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
        val lock = RedisLock(redisOperation, metricsDataReportKey(pipelineId), 120)
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
                    metricsDataReportDao.batchUpdatePipelineStageOverviewData(
                        dslContext = context,
                        updatePipelineStageOverviewDataPOs = updatePipelineStageOverviewDataPOs
                    )
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
                    saveErrorCodeInfoPOs.forEach { saveErrorCodeInfoPO ->
                        metricsDataReportDao.saveErrorCodeInfo(dslContext, saveErrorCodeInfoPO)
                    }
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
        // 没有报错信息则无需处理
        val taskErrorType = taskMetricsData.errorType ?: return
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        var atomFailSummaryDataRecord: TAtomFailSummaryDataRecord? = null
        try {
            atomFailSummaryDataRecord = metricsDataQueryDao.getAtomFailSummaryData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime,
                errorType = taskErrorType,
                atomCode = taskMetricsData.atomCode
            )
        } catch (ignored: TooManyRowsException) {
            logger.warn("fail to get atomFailSummaryData of $projectId|$pipelineId|$statisticsTime", ignored)
            metricsDataClearService.metricsDataClear(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime,
                buildId = buildEndPipelineMetricsData.buildId
            )
            atomFailSummaryDataRecord = metricsDataQueryDao.getAtomFailSummaryData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime,
                errorType = taskErrorType,
                atomCode = taskMetricsData.atomCode
            )
        }
        // 获取该插件在更新集合中的记录
        var existUpdateAtomFailSummaryDataPO = updateAtomFailSummaryDataPOs.firstOrNull {
            it.atomCode == taskMetricsData.atomCode
        }
        if (existUpdateAtomFailSummaryDataPO == null) {
            // 判断该插件是否存在新增集合中，如果存在只需更新记录即可
            val existSaveAtomFailSummaryDataPO = saveAtomFailSummaryDataPOs.firstOrNull {
                it.atomCode == taskMetricsData.atomCode
            }
            existUpdateAtomFailSummaryDataPO = if (existSaveAtomFailSummaryDataPO != null) {
                UpdateAtomFailSummaryDataPO(
                    id = existSaveAtomFailSummaryDataPO.id,
                    projectId = existSaveAtomFailSummaryDataPO.projectId,
                    atomCode = existSaveAtomFailSummaryDataPO.atomCode,
                    errorCount = existSaveAtomFailSummaryDataPO.errorCount,
                    modifier = startUser,
                    updateTime = currentTime
                )
            } else {
                null
            }
            existUpdateAtomFailSummaryDataPO?.let { updateAtomFailSummaryDataPOs.add(it) }
        }
        if (atomFailSummaryDataRecord != null || existUpdateAtomFailSummaryDataPO != null) {
            // 由于插件的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
            val originErrorCount = existUpdateAtomFailSummaryDataPO?.errorCount
                ?: atomFailSummaryDataRecord?.errorCount ?: 0
            val currentErrorCount = originErrorCount + 1
            if (existUpdateAtomFailSummaryDataPO != null) {
                existUpdateAtomFailSummaryDataPO.errorCount = currentErrorCount
                existUpdateAtomFailSummaryDataPO.modifier = startUser
                existUpdateAtomFailSummaryDataPO.updateTime = currentTime
            } else {
                updateAtomFailSummaryDataPOs.add(
                    UpdateAtomFailSummaryDataPO(
                        id = atomFailSummaryDataRecord!!.id,
                        projectId = projectId,
                        atomCode = taskMetricsData.atomCode,
                        errorCount = currentErrorCount,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            }
        } else {
            saveAtomFailSummaryDataPOs.add(
                SaveAtomFailSummaryDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("ATOM_FAIL_SUMMARY_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    channelCode = buildEndPipelineMetricsData.channelCode,
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
                channelCode = buildEndPipelineMetricsData.channelCode,
                buildId = buildEndPipelineMetricsData.buildId,
                buildNum = buildEndPipelineMetricsData.buildNum,
                atomCode = taskMetricsData.atomCode,
                atomName = taskMetricsData.atomName,
                atomPosition = taskMetricsData.atomPosition,
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
                atomCode = taskMetricsData.atomCode,
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
        val atomCode = taskMetricsData.atomCode
        val atomOverviewDataRecord = atomOverviewDataRecords?.firstOrNull { it.atomCode == atomCode }
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        // 获取该插件在更新集合中的记录
        var existUpdateAtomOverviewDataPO = updateAtomOverviewDataPOs.firstOrNull {
            it.atomCode == atomCode
        }
        if (existUpdateAtomOverviewDataPO == null) {
            // 判断该插件是否存在新增集合中，如果存在只需更新记录即可
            val existSaveAtomOverviewDataPO = saveAtomOverviewDataPOs.firstOrNull {
                it.atomCode == atomCode
            }
            existUpdateAtomOverviewDataPO = if (existSaveAtomOverviewDataPO != null) {
                UpdateAtomOverviewDataPO(
                    id = existSaveAtomOverviewDataPO.id,
                    projectId = projectId,
                    atomCode = atomCode,
                    successRate = existSaveAtomOverviewDataPO.successRate,
                    avgCostTime = existSaveAtomOverviewDataPO.avgCostTime,
                    totalExecuteCount = existSaveAtomOverviewDataPO.totalExecuteCount,
                    successExecuteCount = existSaveAtomOverviewDataPO.successExecuteCount,
                    failExecuteCount = existSaveAtomOverviewDataPO.failExecuteCount,
                    modifier = startUser,
                    updateTime = currentTime
                )
            } else {
                null
            }
            existUpdateAtomOverviewDataPO?.let { updateAtomOverviewDataPOs.add(it) }
        }
        if (atomOverviewDataRecord != null || existUpdateAtomOverviewDataPO != null) {
            // 由于插件的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
            val originAvgCostTime = existUpdateAtomOverviewDataPO?.avgCostTime
                ?: atomOverviewDataRecord?.avgCostTime ?: 0L
            val originTotalExecuteCount = existUpdateAtomOverviewDataPO?.totalExecuteCount
                ?: atomOverviewDataRecord?.totalExecuteCount ?: 0L
            val originSuccessExecuteCount = existUpdateAtomOverviewDataPO?.successExecuteCount
                ?: atomOverviewDataRecord?.successExecuteCount ?: 0L
            val originFailExecuteCount = existUpdateAtomOverviewDataPO?.failExecuteCount
                ?: atomOverviewDataRecord?.failExecuteCount ?: 0L
            val currentTotalExecuteCount = originTotalExecuteCount + 1
            val currentTotalCostTime = originAvgCostTime * originTotalExecuteCount + taskMetricsData.costTime
            val currentAvgCostTime = currentTotalCostTime.toDouble().div(currentTotalExecuteCount).roundToLong()
            val currentSuccessExecuteCount = if (taskSuccessFlag) {
                originSuccessExecuteCount + 1
            } else {
                originSuccessExecuteCount
            }
            val currentFailExecuteCount = if (taskSuccessFlag) {
                originFailExecuteCount
            } else {
                originFailExecuteCount + 1
            }
            val currentSuccessRate = currentSuccessExecuteCount.toBigDecimal()
                .divide(currentTotalExecuteCount.toBigDecimal(), 4, BigDecimal.ROUND_HALF_UP)
            val formatSuccessRate = currentSuccessRate.multiply(100.toBigDecimal())
            // 更新已存在的插件统计记录数据
            if (existUpdateAtomOverviewDataPO != null) {
                existUpdateAtomOverviewDataPO.successRate = formatSuccessRate
                existUpdateAtomOverviewDataPO.avgCostTime = currentAvgCostTime
                existUpdateAtomOverviewDataPO.totalExecuteCount = currentTotalExecuteCount
                existUpdateAtomOverviewDataPO.successExecuteCount = currentSuccessExecuteCount
                existUpdateAtomOverviewDataPO.failExecuteCount = currentFailExecuteCount
                existUpdateAtomOverviewDataPO.modifier = startUser
                existUpdateAtomOverviewDataPO.updateTime = currentTime
            } else {
                updateAtomOverviewDataPOs.add(
                    UpdateAtomOverviewDataPO(
                        id = atomOverviewDataRecord!!.id,
                        projectId = projectId,
                        atomCode = atomCode,
                        successRate = formatSuccessRate,
                        avgCostTime = currentAvgCostTime,
                        totalExecuteCount = currentTotalExecuteCount,
                        successExecuteCount = currentSuccessExecuteCount,
                        failExecuteCount = currentFailExecuteCount,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            }
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
                    channelCode = buildEndPipelineMetricsData.channelCode,
                    atomCode = taskMetricsData.atomCode,
                    atomName = taskMetricsData.atomName,
                    classifyCode = taskMetricsData.classifyCode,
                    classifyName = taskMetricsData.classifyName,
                    successRate = successRate,
                    avgCostTime = taskMetricsData.costTime,
                    totalExecuteCount = 1,
                    successExecuteCount = if (taskSuccessFlag) 1 else 0,
                    failExecuteCount = if (taskSuccessFlag) 0 else 1,
                    statisticsTime = statisticsTime,
                    creator = startUser,
                    modifier = startUser,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            )
        }

        metricsDataReportDao.saveAtomMonitorDailyData(
            dslContext = dslContext,
            saveAtomMonitorDailyPO = SaveAtomMonitorDailyPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("ATOM_MONITOR_DATA_DAILY").data ?: 0,
                atomCode = taskMetricsData.atomCode,
                executeCount = 1,
                errorType = taskMetricsData.errorType ?: -1,
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
        if (taskSuccessFlag) return
        val lock = RedisLock(redisOperation, metricsDataReportKey(atomCode), 40)
        try {
            lock.lock()
            val atomIndexStatisticsDailyRecord = metricsDataQueryDao.getAtomIndexStatisticsDailyData(
                dslContext = dslContext,
                statisticsTime = statisticsTime,
                atomCode = atomCode
            )
            val failComplianceCount =
                if (isComplianceErrorCode(atomCode, "${taskMetricsData.errorCode}")) 1 else 0
            if (atomIndexStatisticsDailyRecord != null) {
                metricsDataReportDao.updateAtomIndexStatisticsDailyData(
                    dslContext = dslContext,
                    updateAtomIndexStatisticsDailyPO = UpdateAtomIndexStatisticsDailyPO(
                        id = atomIndexStatisticsDailyRecord.id,
                        failComplianceCount = atomIndexStatisticsDailyRecord.failComplianceCount + failComplianceCount,
                        failExecuteCount = atomIndexStatisticsDailyRecord.failExecuteCount + 1,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            } else {
                metricsDataReportDao.saveAtomIndexStatisticsDailyData(
                    dslContext = dslContext,
                    saveAtomIndexStatisticsDailyPO = SaveAtomIndexStatisticsDailyPO(
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("T_ATOM_INDEX_STATISTICS_DAILY").data ?: 0,
                        atomCode = taskMetricsData.atomCode,
                        failExecuteCount = 1,
                        failComplianceCount = failComplianceCount,
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
        } finally {
            lock.unlock()
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
        val stageOverviewDataRecord = stageOverviewDataRecords?.firstOrNull()
        // 获取该stage标签在更新集合中的记录
        val existUpdateStageOverviewDataPOs = updatePipelineStageOverviewDataPOs.filter {
            stageTagNames.contains(it.stageTagName)
        }.toMutableList()
        // 获取该stage标签在新增集合中的记录
        val existSaveStageOverviewDataPOs = savePipelineStageOverviewDataPOs.filter {
            stageTagNames.contains(it.stageTagName)
        }
        val existUpdateStageTagNames = existUpdateStageOverviewDataPOs.map { it.stageTagName }
        existSaveStageOverviewDataPOs.filter {
            !existUpdateStageTagNames.contains(it.stageTagName)
        }.forEach { saveStageOverviewDataPO ->
            existUpdateStageOverviewDataPOs.add(
                UpdatePipelineStageOverviewDataPO(
                    id = saveStageOverviewDataPO.id,
                    projectId = saveStageOverviewDataPO.projectId,
                    stageTagName = saveStageOverviewDataPO.stageTagName,
                    avgCostTime = saveStageOverviewDataPO.avgCostTime,
                    executeCount = saveStageOverviewDataPO.executeCount,
                    modifier = startUser,
                    updateTime = currentTime
                )
            )
        }
        val existUpdateStageOverviewDataPO = existUpdateStageOverviewDataPOs.firstOrNull()
        val updateStageTagNames = existUpdateStageOverviewDataPOs.map { it.stageTagName }.toMutableSet()
        if (stageOverviewDataRecord != null || existUpdateStageOverviewDataPO != null) {
            // 由于stage的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
            val originStageAvgCostTime = existUpdateStageOverviewDataPO?.avgCostTime
                ?: stageOverviewDataRecord?.avgCostTime ?: 0L
            val originStageExecuteCount = existUpdateStageOverviewDataPO?.executeCount
                ?: stageOverviewDataRecord?.executeCount ?: 0L
            val currentStageExecuteCount = originStageExecuteCount + 1
            val currentStageTotalCostTime = originStageAvgCostTime * originStageExecuteCount + stageCostTime
            val currentStageAvgCostTime = currentStageTotalCostTime.toDouble().div(currentStageExecuteCount)
                .roundToLong()
            existUpdateStageOverviewDataPOs.forEach { stageOverviewDataPO ->
                stageOverviewDataPO.avgCostTime = currentStageAvgCostTime
                stageOverviewDataPO.executeCount = currentStageExecuteCount
                stageOverviewDataPO.modifier = startUser
                stageOverviewDataPO.updateTime = currentTime
            }
            // 更新db中已存在的stage统计记录数据
            stageOverviewDataRecords?.filter {
                // 剔除掉集合中已存在的更新记录
                !updateStageTagNames.contains(it.stageTagName)
            }?.forEach { tmpStageOverviewDataRecord ->
                updateStageTagNames.add(tmpStageOverviewDataRecord.stageTagName)
                existUpdateStageOverviewDataPOs.add(
                    UpdatePipelineStageOverviewDataPO(
                        id = tmpStageOverviewDataRecord.id,
                        projectId = projectId,
                        stageTagName = tmpStageOverviewDataRecord.stageTagName,
                        avgCostTime = currentStageAvgCostTime,
                        executeCount = currentStageExecuteCount,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            }
        }
        // 排除已存在的stage统计记录
        stageTagNames.removeAll(updateStageTagNames)
        stageTagNames.removeAll(existSaveStageOverviewDataPOs.map { it.stageTagName })
        existUpdateStageOverviewDataPOs.forEach { tmpExistUpdateStageOverviewDataPO ->
            val tmpUpdateStageOverviewDataPO = updatePipelineStageOverviewDataPOs.firstOrNull {
                it.stageTagName == tmpExistUpdateStageOverviewDataPO.stageTagName
            }
            if (tmpUpdateStageOverviewDataPO != null) {
                tmpUpdateStageOverviewDataPO.avgCostTime = tmpExistUpdateStageOverviewDataPO.avgCostTime
                tmpUpdateStageOverviewDataPO.executeCount = tmpExistUpdateStageOverviewDataPO.executeCount
                tmpUpdateStageOverviewDataPO.modifier = tmpExistUpdateStageOverviewDataPO.modifier
                tmpUpdateStageOverviewDataPO.updateTime = tmpExistUpdateStageOverviewDataPO.updateTime
            } else {
                updatePipelineStageOverviewDataPOs.add(tmpExistUpdateStageOverviewDataPO)
            }
        }
        stageTagNames.forEach { stageTagName ->
            savePipelineStageOverviewDataPOs.add(
                SavePipelineStageOverviewDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_STAGE_DETAIL_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    channelCode = buildEndPipelineMetricsData.channelCode,
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
        // 没有报错信息则无需处理
        val errorInfos = buildEndPipelineMetricsData.errorInfos ?: return
        val buildSuccessFlag = buildEndPipelineMetricsData.successFlag // 流水线构建是否成功标识
        if (buildSuccessFlag) {
            return
        }
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val buildId = buildEndPipelineMetricsData.buildId
        val buildNum = buildEndPipelineMetricsData.buildNum // 构建序号
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val errorTypes = mutableSetOf<Int>()
        errorInfos.forEach { errorInfo ->
            val errorType = errorInfo.errorType
            val errorCode = errorInfo.errorCode
            val errorMsg = errorInfo.errorMsg
            errorTypes.add(errorType)
            // 插入流水线失败详情数据
            val savePipelineFailDetailDataPO = SavePipelineFailDetailDataPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_FAIL_DETAIL_DATA").data ?: 0,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                channelCode = buildEndPipelineMetricsData.channelCode,
                buildId = buildId,
                buildNum = buildNum,
                repoUrl = buildEndPipelineMetricsData.repoUrl,
                branch = buildEndPipelineMetricsData.branch,
                startUser = startUser,
                startTime = buildEndPipelineMetricsData.startTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                endTime = buildEndPipelineMetricsData.endTime?.let { DateTimeUtil.stringToLocalDateTime(it) },
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg,
                statisticsTime = statisticsTime,
                creator = startUser,
                modifier = startUser,
                createTime = currentTime,
                updateTime = currentTime
            )
            metricsDataReportDao.savePipelineFailDetailData(dslContext, savePipelineFailDetailDataPO)
            // 添加错误信息
            addErrorCodeInfo(
                saveErrorCodeInfoPOs = saveErrorCodeInfoPOs,
                atomCode = errorInfo.atomCode,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg,
                startUser = startUser,
                currentTime = currentTime
            )
        }
        errorTypes.forEach { errorType ->
            // 插入流水线失败汇总数据
            var pipelineFailSummaryDataRecord: TPipelineFailSummaryDataRecord? = null
            try {
                pipelineFailSummaryDataRecord = metricsDataQueryDao.getPipelineFailSummaryData(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    statisticsTime = statisticsTime,
                    errorType = errorType
                )
            } catch (ignored: TooManyRowsException) {
                logger.warn("fail to get pipelineFailSummaryData of $projectId|$pipelineId|$statisticsTime", ignored)
                metricsDataClearService.metricsDataClear(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    statisticsTime = statisticsTime,
                    buildId = buildEndPipelineMetricsData.buildId
                )
                pipelineFailSummaryDataRecord = metricsDataQueryDao.getPipelineFailSummaryData(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    statisticsTime = statisticsTime,
                    errorType = errorType
                )
            }
            if (pipelineFailSummaryDataRecord == null) {
                val savePipelineFailSummaryDataPO = SavePipelineFailSummaryDataPO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PIPELINE_FAIL_SUMMARY_DATA").data ?: 0,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    channelCode = buildEndPipelineMetricsData.channelCode,
                    errorType = errorType,
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
        }
    }

    private fun pipelineOverviewDataReport(
        dslContext: DSLContext,
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val buildSuccessFlag = buildEndPipelineMetricsData.successFlag // 流水线构建是否成功标识
        val pipelineBuildCostTime = buildEndPipelineMetricsData.costTime // 流水线构建所耗时间
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        var pipelineOverviewDataRecord: TPipelineOverviewDataRecord? = null
        try {
            pipelineOverviewDataRecord = metricsDataQueryDao.getPipelineOverviewData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime
            )
        } catch (ignored: TooManyRowsException) {
            logger.warn("fail to get pipelineOverviewData of $projectId|$pipelineId|$statisticsTime", ignored)
            metricsDataClearService.metricsDataClear(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime,
                buildId = buildEndPipelineMetricsData.buildId
            )
            pipelineOverviewDataRecord = metricsDataQueryDao.getPipelineOverviewData(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime
            )
        }
        if (pipelineOverviewDataRecord == null) {
            // db没有记录则插入记录
            val savePipelineOverviewDataPO = SavePipelineOverviewDataPO(
                id = client.get(ServiceAllocIdResource::class)
                    .generateSegmentId("PIPELINE_OVERVIEW_DATA").data ?: 0,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = buildEndPipelineMetricsData.pipelineName,
                channelCode = buildEndPipelineMetricsData.channelCode,
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
                val currentSuccessCostTime = originSuccessAvgCostTime * originSuccessExecuteCount +
                    pipelineBuildCostTime
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
        atomCode: String,
        errorType: Int,
        errorCode: Int,
        errorMsg: String?,
        startUser: String,
        currentTime: LocalDateTime
    ) {
        // 从本地缓存获取错误码信息
        val cacheKey = "$atomCode:$errorType:$errorCode"
        val errorCodeInfo = ErrorCodeInfoCacheUtil.getIfPresent(cacheKey)
        if (errorCodeInfo == null) {
            // 缓存中不存在则需要入库
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
                    updateTime = currentTime,
                    atomCode = atomCode
                )
            )
            // 将错误码信息放入缓存中
            ErrorCodeInfoCacheUtil.put(cacheKey, true)
        }
    }

    private fun isComplianceErrorCode(atomCode: String, errorCode: String): Boolean {
        if (errorCode.length != 6) return false
        val errorCodePrefix = errorCode.substring(0, 3)
        val errorCodeType: ErrorCodeTypeEnum = when {
            errorCodePrefix.startsWith("8") -> {
                ErrorCodeTypeEnum.ATOM
            }
            errorCodePrefix.startsWith("100") -> {
                ErrorCodeTypeEnum.GENERAL
            }
            errorCodePrefix.toInt() in 101..599 -> {
                ErrorCodeTypeEnum.PLATFORM
            }
            else -> return false
        }
        return client.get(ServiceStoreResource::class).isComplianceErrorCode(
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            errorCode = errorCode.toInt(),
            errorCodeType = errorCodeType
        ).data!!
    }
}
