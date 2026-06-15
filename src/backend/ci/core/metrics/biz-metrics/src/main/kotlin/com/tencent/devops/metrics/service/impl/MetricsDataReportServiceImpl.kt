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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.DateTimeUtil.YYYY_MM_DD
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.measure.BuildEndMetricsBroadCastEvent
import com.tencent.devops.common.event.pojo.measure.BuildEndPipelineMetricsData
import com.tencent.devops.common.event.pojo.measure.BuildEndTaskMetricsData
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.event.pojo.measure.DispatchJobMetricsData
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.metrics.api.ServiceMetricsDataReportResource
import com.tencent.devops.metrics.dao.DispatchJobMetricsDao
import com.tencent.devops.metrics.dao.MetricsDataQueryDao
import com.tencent.devops.metrics.dao.MetricsDataReportDao
import com.tencent.devops.metrics.dao.ProjectInfoDao
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
import com.tencent.devops.metrics.pojo.po.SaveProjectAtomRelationDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomIndexStatisticsDailyPO
import com.tencent.devops.metrics.pojo.po.UpdateAtomOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineFailSummaryDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineOverviewDataPO
import com.tencent.devops.metrics.pojo.po.UpdatePipelineStageOverviewDataPO
import com.tencent.devops.metrics.service.MetricsDataClearService
import com.tencent.devops.metrics.service.MetricsDataReportService
import com.tencent.devops.metrics.service.ProjectBuildSummaryService
import com.tencent.devops.metrics.utils.ErrorCodeInfoCacheUtil
import com.tencent.devops.metrics.utils.ProjectAtomRelationCacheUtil
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
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToLong
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.exception.TooManyRowsException
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ComplexMethod", "NestedBlockDepth", "LongMethod", "LongParameterList")
class MetricsDataReportServiceImpl @Autowired constructor(
    private val bkTag: BkTag,
    private val dslContext: DSLContext,
    private val metricsDataQueryDao: MetricsDataQueryDao,
    private val metricsDataReportDao: MetricsDataReportDao,
    private val projectInfoDao: ProjectInfoDao,
    private val dispatchJobMetricsDao: DispatchJobMetricsDao,
    private val metricsDataClearService: MetricsDataClearService,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val projectBuildSummaryService: ProjectBuildSummaryService,
    private val measureEventDispatcher: SampleEventDispatcher
) : MetricsDataReportService {

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsDataReportService::class.java)
        private fun metricsDataReportKey(key: String) = "metricsDataReport:$key"

        // 锁获取失败时的最大重试次数，防止某些项目竞争过大导致MQ消息无限重发堆积
        private const val MAX_LOCK_RETRY = 5
        // 锁获取失败后下次重投的基础延迟(毫秒)
        private const val LOCK_RETRY_BASE_DELAY_MILLS = 10_000
        // 重投延迟的随机抖动上限(毫秒)；用 base+random(0, jitter) 打散并发失败的重试时刻，
        // 避免「同一时刻一批 build 失败 → 整齐延迟 10 秒 → 再次同时冲击锁」的重试风暴
        private const val LOCK_RETRY_JITTER_MILLS = 5_000
    }

    /**
     * 处理流水线构建结束的指标数据上报
     *
     * @param event 构建结束后流水线指标数据消息
     * @return Boolean 始终返回true，表示处理成功
     */
    override fun metricsDataReport(event: BuildEndMetricsBroadCastEvent): Boolean {
        // 解构获取关键ID信息
        val buildEndPipelineMetricsData = event.buildEndPipelineMetricsData
        val (projectId, pipelineId, buildId) = buildEndPipelineMetricsData.run {
            Triple(projectId, pipelineId, buildId)
        }
        logger.info("[$projectId|$pipelineId|$buildId]|start metricsDataReport")
        val watcher = Watcher(id = "metricsDataReport|$projectId|$pipelineId|$buildId")
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val currentTime = LocalDateTime.now() // 统一使用当前时间保证时间一致性
        RedisLock(
            redisOperation = redisOperation,
            lockKey = metricsDataReportKey("pipeline:$pipelineId:model:data"),
            expiredTimeInSeconds = 100
        ).use { lock ->
            if (!lock.tryLock()) {
                // 锁获取失败：受控重试，避免无限重发导致MQ堆积
                retryOrDiscardOnLockFailed(
                    event = event,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId
                )
                return false
            }
            // 流水线model数据解析上报
            watcher.start("doPipelineModelDataReportBus")
            doPipelineModelDataReportBus(
                buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                currentTime = currentTime,
                projectId = projectId,
                pipelineId = pipelineId,
                statisticsTime = statisticsTime
            )
            // 流水线级别概览数据上报
            watcher.start("pipelineOverviewDataReport")
            pipelineOverviewDataReport(
                buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                currentTime = currentTime
            )
            // 流水线失败数据上报
            watcher.start("pipelineFailDataReport")
            pipelineFailDataReport(
                buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                currentTime = currentTime
            )
        }

        // 特殊渠道（BS/CREATIVE_STREAM）的项目构建数统计
        if (buildEndPipelineMetricsData.channelCode == ChannelCode.BS.name ||
            buildEndPipelineMetricsData.channelCode == ChannelCode.CREATIVE_STREAM.name
        ) {
            watcher.start("saveProjectBuildCount")
            projectBuildSummaryService.saveProjectBuildCount(
                projectId = projectId,
                trigger = buildEndPipelineMetricsData.trigger
            )
        }

        logger.info("[$projectId|$pipelineId|$buildId]|end metricsDataReport")
        return true
    }

    /**
     * 锁获取失败时的受控重试：
     * - 未达到最大重试次数：递增 retryTime 并延迟重投，错峰缓解锁竞争
     * - 已达到最大重试次数：放弃本次上报，仅打 warn 日志，避免消息在MQ无限堆积（统计上报不属于核心业务链路，丢失单次构建的统计影响可控）
     */
    private fun retryOrDiscardOnLockFailed(
        event: BuildEndMetricsBroadCastEvent,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        if (event.retryTime >= MAX_LOCK_RETRY) {
            logger.warn(
                "[$projectId|$pipelineId|$buildId]|try lock failed, " +
                    "discard metrics report after ${event.retryTime} retries"
            )
            return
        }
        event.retryTime += 1
        // 基础延迟 + 随机抖动，避免一批同时失败的消息又同时到达造成锁竞争惊群
        event.delayMills = LOCK_RETRY_BASE_DELAY_MILLS +
            ThreadLocalRandom.current().nextInt(LOCK_RETRY_JITTER_MILLS)
        logger.warn(
            "[$projectId|$pipelineId|$buildId]|try lock failed, resend message, " +
                "retryTime=${event.retryTime}, delayMills=${event.delayMills}"
        )
        measureEventDispatcher.dispatch(event)
    }

    private fun doPipelineModelDataReportBus(
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime,
        projectId: String,
        pipelineId: String,
        statisticsTime: LocalDateTime
    ) {
        val savePipelineStageOverviewDataPOs = mutableListOf<SavePipelineStageOverviewDataPO>()
        val updatePipelineStageOverviewDataPOs = mutableListOf<UpdatePipelineStageOverviewDataPO>()
        val saveAtomOverviewDataPOs = mutableListOf<SaveAtomOverviewDataPO>()
        val saveProjectAtomRelationPOs = mutableListOf<SaveProjectAtomRelationDataPO>()
        // 收集"DB 中尚不存在、本次构建待写入"的项目-插件关系缓存 key，
        // 在 batchSaveProjectAtomInfo 真正成功之后再统一 put 缓存，避免 DAO 失败时缓存与 DB 不一致。
        val pendingProjectAtomCacheKeys = mutableSetOf<String>()
        val updateAtomOverviewDataPOs = mutableListOf<UpdateAtomOverviewDataPO>()
        val saveErrorCodeInfoPOs = mutableSetOf<SaveErrorCodeInfoPO>()
        val saveAtomFailSummaryDataPOs = mutableListOf<SaveAtomFailSummaryDataPO>()
        val updateAtomFailSummaryDataPOs = mutableListOf<UpdateAtomFailSummaryDataPO>()
        val saveAtomFailDetailDataPOs = mutableListOf<SaveAtomFailDetailDataPO>()
        // 遍历流水线所有阶段进行指标上报
        buildEndPipelineMetricsData.stages.forEach { stage ->
            val stageTagNames = stage.stageTagNames?.toMutableList() ?: mutableListOf()
            // 阶段概览数据上报
            pipelineStageOverviewDataReport(
                stageTagNames = stageTagNames,
                stageCostTime = stage.costTime,
                buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                currentTime = currentTime,
                savePipelineStageOverviewDataPOs = savePipelineStageOverviewDataPOs,
                updatePipelineStageOverviewDataPOs = updatePipelineStageOverviewDataPOs
            )
            // 遍历阶段内所有任务进行插件数据上报
            stage.containers.forEach { container ->
                val atomOverviewDataRecords = metricsDataQueryDao.getAtomOverviewDatas(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    statisticsTime = statisticsTime,
                    atomCodes = container.atomCodes
                )
                container.tasks.forEach { task ->
                    // 插件概览数据上报
                    atomOverviewDataReport(
                        buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                        taskMetricsData = task,
                        currentTime = currentTime,
                        atomOverviewDataRecords = atomOverviewDataRecords,
                        updateAtomOverviewDataPOs = updateAtomOverviewDataPOs,
                        saveAtomOverviewDataPOs = saveAtomOverviewDataPOs,
                        saveProjectAtomRelationPOs = saveProjectAtomRelationPOs,
                        pendingProjectAtomCacheKeys = pendingProjectAtomCacheKeys
                    )
                    // 插件失败汇总数据上报
                    atomFailSummaryDataReport(
                        buildEndPipelineMetricsData = buildEndPipelineMetricsData,
                        taskMetricsData = task,
                        currentTime = currentTime,
                        updateAtomFailSummaryDataPOs = updateAtomFailSummaryDataPOs,
                        saveAtomFailSummaryDataPOs = saveAtomFailSummaryDataPOs,
                        saveAtomFailDetailDataPOs = saveAtomFailDetailDataPOs,
                        saveErrorCodeInfoPOs = saveErrorCodeInfoPOs
                    )
                    // 当构建失败时进行日常统计指标上报
                    if (!buildEndPipelineMetricsData.successFlag) {
                        RedisLock(redisOperation, metricsDataReportKey(task.atomCode), 40).use { lock ->
                            lock.lock()
                            atomIndexStatisticsDailyDataReport(
                                statisticsTime = buildEndPipelineMetricsData.statisticsTime.let {
                                    DateTimeUtil.stringToLocalDateTime(it, YYYY_MM_DD)
                                },
                                taskMetricsData = task,
                                currentTime = currentTime,
                                startUser = buildEndPipelineMetricsData.startUser
                            )
                        }
                    }
                }
            }
        }

        dslContext.transaction { t ->
            val context = DSL.using(t)
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
                metricsDataReportDao.batchSaveAtomFailDetailData(context, saveAtomFailDetailDataPOs)
            }
            if (saveErrorCodeInfoPOs.isNotEmpty()) {
                metricsDataReportDao.batchSaveErrorCodeInfo(context, saveErrorCodeInfoPOs)
            }
        }
        // 项目-插件关系是幂等写入(onDuplicateKeyUpdate)，与上面的聚合统计数据无强一致性要求，
        // 单独在大事务外提交，缩短主事务的持锁时间，进一步减轻并发场景下的间隙锁等待
        if (saveProjectAtomRelationPOs.isNotEmpty()) {
            try {
                projectInfoDao.batchSaveProjectAtomInfo(dslContext, saveProjectAtomRelationPOs)
                // DAO 真正成功之后再把 key 写入缓存
                if (pendingProjectAtomCacheKeys.isNotEmpty()) {
                    pendingProjectAtomCacheKeys.forEach { ProjectAtomRelationCacheUtil.put(it) }
                }
            } catch (ignored: Exception) {
                // metrics 上报路径上，项目-插件关系写入失败不影响主流程，仅记录日志
                logger.warn(
                    "batchSaveProjectAtomInfo failed, size=${saveProjectAtomRelationPOs.size}, " +
                        "projectId=$projectId",
                    ignored
                )
            }
        }
    }

    override fun saveDispatchJobMetrics(dispatchJobMetricsDataList: List<DispatchJobMetricsData>): Boolean {
        // 批量插入时获取批量的自增ID
        val idList = client.get(ServiceAllocIdResource::class)
            .batchGenerateSegmentId("T_DISPATCH_JOB_DAILY_METRICS", (dispatchJobMetricsDataList.size))
            .data?.toMutableList()
        if (idList == null || idList.size != dispatchJobMetricsDataList.size) {
            logger.error("SaveDispatchJobMetrics fail to get idList. " +
                    "${idList?.size ?: 0}|${dispatchJobMetricsDataList.size}")
            return false
        }

        dispatchJobMetricsDataList.forEach {
            fixBkTagMetricsJobDispatchDataReport(it.copy(id = idList.removeAt(0) ?: 0))
        }
        return true
    }

    override fun metricsJobDispatchDataReport(dispatchJobMetricsData: DispatchJobMetricsData): Boolean {
        dispatchJobMetricsDao.saveDispatchJobMetrics(dslContext, dispatchJobMetricsData)
        return true
    }

    private fun fixBkTagMetricsJobDispatchDataReport(dispatchJobMetricsData: DispatchJobMetricsData) {
        try {
            val projectId = dispatchJobMetricsData.projectId
            val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)

            return bkTag.invokeByTag(projectConsulTag) {
                val bkFinalTag = bkTag.getFinalTag()
                logger.info("MetricsJobDispatchDataReport $projectId|$projectConsulTag|$bkFinalTag")
                client.getGateway(ServiceMetricsDataReportResource::class)
                    .metricsJobDispatchDataReport(dispatchJobMetricsData).data
            }
        } catch (e: Exception) {
            logger.error("Save dispatchJobMetrics error.", e)
        }
    }

    private fun atomFailSummaryDataReport(
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        taskMetricsData: BuildEndTaskMetricsData,
        currentTime: LocalDateTime,
        updateAtomFailSummaryDataPOs: MutableList<UpdateAtomFailSummaryDataPO>,
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
        var atomFailSummaryDataRecord: TAtomFailSummaryDataRecord?
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
        when {
            existUpdateAtomFailSummaryDataPO != null -> {
                // 由于插件的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
                existUpdateAtomFailSummaryDataPO.errorCount += 1
                existUpdateAtomFailSummaryDataPO.modifier = startUser
                existUpdateAtomFailSummaryDataPO.updateTime = currentTime
            }
            atomFailSummaryDataRecord != null -> {
                updateAtomFailSummaryDataPOs.add(
                    UpdateAtomFailSummaryDataPO(
                        id = atomFailSummaryDataRecord.id,
                        projectId = projectId,
                        atomCode = taskMetricsData.atomCode,
                        errorCount = atomFailSummaryDataRecord.errorCount + 1,
                        modifier = startUser,
                        updateTime = currentTime
                    )
                )
            }
            else -> {
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
        currentTime: LocalDateTime,
        atomOverviewDataRecords: Result<TAtomOverviewDataRecord>?,
        updateAtomOverviewDataPOs: MutableList<UpdateAtomOverviewDataPO>,
        saveAtomOverviewDataPOs: MutableList<SaveAtomOverviewDataPO>,
        saveProjectAtomRelationPOs: MutableList<SaveProjectAtomRelationDataPO>,
        // 收集"DB 中尚不存在、本次构建待写入"的项目-插件关系缓存 key；
        // 由调用方在 batchSaveProjectAtomInfo 成功之后统一 put 缓存，保证缓存与 DB 一致。
        pendingProjectAtomCacheKeys: MutableSet<String>
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val pipelineName = buildEndPipelineMetricsData.pipelineName
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        val taskSuccessFlag = taskMetricsData.successFlag
        val atomCode = taskMetricsData.atomCode
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val atomOverviewDataRecord: TAtomOverviewDataRecord? =
            atomOverviewDataRecords?.firstOrNull { it.atomCode == atomCode }
        val existUpdateAtomOverviewDataPO: UpdateAtomOverviewDataPO? =
            updateAtomOverviewDataPOs.firstOrNull { it.atomCode == atomCode }
                ?: saveAtomOverviewDataPOs.firstOrNull { it.atomCode == atomCode }
                    ?.let { existSavePO ->
                        UpdateAtomOverviewDataPO(
                            id = existSavePO.id,
                            projectId = projectId,
                            atomCode = atomCode,
                            successRate = existSavePO.successRate,
                            avgCostTime = existSavePO.avgCostTime,
                            totalExecuteCount = existSavePO.totalExecuteCount,
                            successExecuteCount = existSavePO.successExecuteCount,
                            failExecuteCount = existSavePO.failExecuteCount,
                            modifier = startUser,
                            updateTime = currentTime
                        ).also { updateAtomOverviewDataPOs.add(it) }
                    }
        fun updateAtomOverviewData(
            originAvgCostTime: Long,
            originTotalExecuteCount: Long,
            originSuccessExecuteCount: Long,
            originFailExecuteCount: Long,
            updateAction: (BigDecimal, Long, Long, Long, Long) -> Unit
        ) {
            // 由于插件的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
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
            updateAction(
                formatSuccessRate,
                currentAvgCostTime,
                currentTotalExecuteCount,
                currentSuccessExecuteCount,
                currentFailExecuteCount
            )
        }

        when {
            existUpdateAtomOverviewDataPO != null -> updateAtomOverviewData(
                originAvgCostTime = existUpdateAtomOverviewDataPO.avgCostTime,
                originTotalExecuteCount = existUpdateAtomOverviewDataPO.totalExecuteCount,
                originSuccessExecuteCount = existUpdateAtomOverviewDataPO.successExecuteCount,
                originFailExecuteCount = existUpdateAtomOverviewDataPO.failExecuteCount
            ) { formatSuccessRate, currentAvgCostTime, currentTotalExecuteCount,
                currentSuccessExecuteCount, currentFailExecuteCount ->
                existUpdateAtomOverviewDataPO.successRate = formatSuccessRate
                existUpdateAtomOverviewDataPO.avgCostTime = currentAvgCostTime
                existUpdateAtomOverviewDataPO.totalExecuteCount = currentTotalExecuteCount
                existUpdateAtomOverviewDataPO.successExecuteCount = currentSuccessExecuteCount
                existUpdateAtomOverviewDataPO.failExecuteCount = currentFailExecuteCount
                existUpdateAtomOverviewDataPO.modifier = startUser
                existUpdateAtomOverviewDataPO.updateTime = currentTime
            }
            atomOverviewDataRecord != null -> updateAtomOverviewData(
                originAvgCostTime = atomOverviewDataRecord.avgCostTime,
                originTotalExecuteCount = atomOverviewDataRecord.totalExecuteCount,
                originSuccessExecuteCount = atomOverviewDataRecord.successExecuteCount,
                originFailExecuteCount = atomOverviewDataRecord.failExecuteCount
            ) { formatSuccessRate, currentAvgCostTime, currentTotalExecuteCount,
                currentSuccessExecuteCount, currentFailExecuteCount ->
                updateAtomOverviewDataPOs.add(
                    UpdateAtomOverviewDataPO(
                        id = atomOverviewDataRecord.id,
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
            else -> {
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
        }

        // 项目-插件关系：先走本地缓存判断是否已写入过，避免每次构建都对 T_PROJECT_ATOM 进行
        // "先查再写"，降低唯一索引 UNI_TPA_PROJECT_CODE 上的 next-key / gap lock 等待
        val projectAtomCacheKey = ProjectAtomRelationCacheUtil.buildKey(
            projectId = projectId,
            atomCode = atomCode,
            atomName = taskMetricsData.atomName
        )
        if (ProjectAtomRelationCacheUtil.getIfPresent(projectAtomCacheKey) == null) {
            val projectAtomRelationCount = projectInfoDao.projectAtomRelationCountByAtomCode(
                dslContext = dslContext,
                projectId = projectId,
                atomCode = atomCode,
                atomName = taskMetricsData.atomName
            )
            if (projectAtomRelationCount <= 0) {
                saveProjectAtomRelationPOs.add(
                    SaveProjectAtomRelationDataPO(
                        id = client.get(ServiceAllocIdResource::class)
                            .generateSegmentId("METRICS_PROJECT_ATOM_RELEVANCY_INFO").data ?: 0,
                        projectId = projectId,
                        atomCode = atomCode,
                        atomName = taskMetricsData.atomName,
                        creator = SYSTEM,
                        modifier = SYSTEM
                    )
                )
                // 延迟 put：等 doPipelineModelDataReportBus 末尾 batchSaveProjectAtomInfo 成功后再写缓存，
                // 避免 DAO 失败被 catch 吞掉时，缓存却"已写入"导致 24 小时内本机不再尝试。
                pendingProjectAtomCacheKeys.add(projectAtomCacheKey)
            } else {
                // DB 已存在该 (projectId, atomCode, atomName)，立即写缓存让同 build 内后续 task 命中。
                ProjectAtomRelationCacheUtil.put(projectAtomCacheKey)
            }
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
    }

    private fun atomIndexStatisticsDailyDataReport(
        statisticsTime: LocalDateTime,
        currentTime: LocalDateTime,
        startUser: String,
        taskMetricsData: BuildEndTaskMetricsData
    ) {
        val atomCode = taskMetricsData.atomCode
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
                    statisticsTime = statisticsTime,
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
        currentTime: LocalDateTime,
        updatePipelineStageOverviewDataPOs: MutableList<UpdatePipelineStageOverviewDataPO>,
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
        fun updateStageOverviewData(originStageAvgCostTime: Long, originStageExecuteCount: Long) {
            // 由于stage的构建数据是遍历model完成才进行db操作处理，故集合中的数据代表是最新的统计数据
            val currentStageExecuteCount = originStageExecuteCount + 1
            val currentStageAvgCostTime =
                (originStageAvgCostTime * originStageExecuteCount + stageCostTime)
                    .toDouble().div(currentStageExecuteCount).roundToLong()
            // 批量更新已存在的stage统计数据
            existUpdateStageOverviewDataPOs.forEach { stageOverviewDataPO ->
                stageOverviewDataPO.avgCostTime = currentStageAvgCostTime
                stageOverviewDataPO.executeCount = currentStageExecuteCount
                stageOverviewDataPO.modifier = startUser
                stageOverviewDataPO.updateTime = currentTime
            }
            // 将db中已存在但尚未在更新集合中的stage记录也加入更新列表
            if (stageOverviewDataRecords != null) {
                for (index in 0 until stageOverviewDataRecords.size) {
                    val tmpStageOverviewDataRecord = stageOverviewDataRecords[index]
                    if (!updateStageTagNames.contains(tmpStageOverviewDataRecord.stageTagName)) {
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
            }
        }

        when {
            existUpdateStageOverviewDataPO != null -> updateStageOverviewData(
                originStageAvgCostTime = existUpdateStageOverviewDataPO.avgCostTime,
                originStageExecuteCount = existUpdateStageOverviewDataPO.executeCount
            )
            stageOverviewDataRecord != null -> updateStageOverviewData(
                originStageAvgCostTime = stageOverviewDataRecord.avgCostTime,
                originStageExecuteCount = stageOverviewDataRecord.executeCount
            )
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
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime
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
        val saveErrorCodeInfoPOs = mutableSetOf<SaveErrorCodeInfoPO>()
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
            var pipelineFailSummaryDataRecord: TPipelineFailSummaryDataRecord?
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
            if (saveErrorCodeInfoPOs.isNotEmpty()) {
                metricsDataReportDao.batchSaveErrorCodeInfo(dslContext, saveErrorCodeInfoPOs)
            }
        }
    }

    private fun pipelineOverviewDataReport(
        buildEndPipelineMetricsData: BuildEndPipelineMetricsData,
        currentTime: LocalDateTime
    ) {
        val projectId = buildEndPipelineMetricsData.projectId
        val pipelineId = buildEndPipelineMetricsData.pipelineId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(buildEndPipelineMetricsData.statisticsTime, YYYY_MM_DD)
        val buildSuccessFlag = buildEndPipelineMetricsData.successFlag // 流水线构建是否成功标识
        val pipelineBuildCostTime = buildEndPipelineMetricsData.costTime // 流水线构建所耗时间
        val startUser = buildEndPipelineMetricsData.startUser // 启动用户
        var pipelineOverviewDataRecord: TPipelineOverviewDataRecord?
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
