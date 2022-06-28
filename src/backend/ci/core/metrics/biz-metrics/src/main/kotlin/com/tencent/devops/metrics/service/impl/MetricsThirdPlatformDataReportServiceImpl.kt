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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.MetricsThirdPlatformInfoDao
import com.tencent.devops.metrics.pojo.message.CodeCheckReportEvent
import com.tencent.devops.metrics.pojo.message.QualityReportMessage
import com.tencent.devops.metrics.pojo.message.TurboReportEvent
import com.tencent.devops.metrics.pojo.po.ThirdPlatformDatePO
import com.tencent.devops.metrics.service.MetricsThirdPlatformDataReportService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class MetricsThirdPlatformDataReportServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val metricsThirdPlatformInfoDao: MetricsThirdPlatformInfoDao
) : MetricsThirdPlatformDataReportService {

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsThirdPlatformDataReportServiceImpl::class.java)
        private fun metricsThirdPlatformDataReportKey(projectId: String, statisticsTime: String): String {
            return "metricsThirdPlatformDataReport:$projectId:$statisticsTime"
        }
    }
    override fun metricsCodeCheckDataReport(codeCheckReportEvent: CodeCheckReportEvent): Boolean {
        val projectId = codeCheckReportEvent.projectId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(codeCheckReportEvent.statisticsTime, YYYY_MM_DD)
        val lock = RedisLock(
            redisOperation,
            metricsThirdPlatformDataReportKey(projectId, statisticsTime.toString()),
            10
        )
        val currentTime = LocalDateTime.now()
        try {
            lock.lock()
            val metricsThirdPlatformRecord = metricsThirdPlatformInfoDao.getMetricsThirdPlatformInfo(
                dslContext,
                projectId,
                statisticsTime
            )
            val thirdPlatformDatePO = if (metricsThirdPlatformRecord == null) {
                ThirdPlatformDatePO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("METRICS_PROJECT_THIRD_PLATFORM_DATA").data ?: 0,
                    projectId = projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = codeCheckReportEvent.resolvedDefectNum,
                    repoCodeccAvgScore = BigDecimal(codeCheckReportEvent.repoCodeccAvgScore),
                    qualityPipelineExecuteNum = null,
                    qualityPipelineInterceptionNum = null,
                    turboSaveTime = null,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            } else {
                ThirdPlatformDatePO(
                    id = metricsThirdPlatformRecord.id,
                    projectId = metricsThirdPlatformRecord.projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = codeCheckReportEvent.resolvedDefectNum,
                    repoCodeccAvgScore = BigDecimal(codeCheckReportEvent.repoCodeccAvgScore),
                    qualityPipelineExecuteNum = metricsThirdPlatformRecord.qualityPipelineExecuteNum,
                    qualityPipelineInterceptionNum = metricsThirdPlatformRecord.qualityPipelineInterceptionNum,
                    turboSaveTime = metricsThirdPlatformRecord.turboSaveTime,
                    createTime = metricsThirdPlatformRecord.createTime,
                    updateTime = currentTime
                )
            }
            metricsThirdPlatformInfoDao.saveMetricsThirdPlatformData(
                dslContext,
                thirdPlatformDatePO
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun metricsTurboDataReport(turboReportMessage: TurboReportEvent): Boolean {
        val projectId = turboReportMessage.projectId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(turboReportMessage.statisticsTime, YYYY_MM_DD)
        val lock = RedisLock(
            redisOperation,
            metricsThirdPlatformDataReportKey(projectId, statisticsTime.toString()),
            10
        )
        val currentTime = LocalDateTime.now()
        try {
            lock.lock()
            val metricsThirdPlatformRecord = metricsThirdPlatformInfoDao.getMetricsThirdPlatformInfo(
                dslContext,
                projectId,
                statisticsTime
            )
            val thirdPlatformDatePO = if (metricsThirdPlatformRecord == null) {
                ThirdPlatformDatePO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("METRICS_PROJECT_THIRD_PLATFORM_DATA").data ?: 0,
                    projectId = projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = null,
                    repoCodeccAvgScore = null,
                    qualityPipelineExecuteNum = null,
                    qualityPipelineInterceptionNum = null,
                    turboSaveTime = BigDecimal(turboReportMessage.turboSaveTime),
                    createTime = currentTime,
                    updateTime = currentTime
                )
            } else {
                ThirdPlatformDatePO(
                    id = metricsThirdPlatformRecord.id,
                    projectId = metricsThirdPlatformRecord.projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = metricsThirdPlatformRecord.resolvedDefectNum,
                    repoCodeccAvgScore = metricsThirdPlatformRecord.repoCodeccAvgScore,
                    qualityPipelineExecuteNum = metricsThirdPlatformRecord.qualityPipelineExecuteNum,
                    qualityPipelineInterceptionNum = metricsThirdPlatformRecord.qualityPipelineInterceptionNum,
                    turboSaveTime = BigDecimal(turboReportMessage.turboSaveTime),
                    createTime = metricsThirdPlatformRecord.createTime,
                    updateTime = currentTime
                )
            }
            metricsThirdPlatformInfoDao.saveMetricsThirdPlatformData(
                dslContext,
                thirdPlatformDatePO
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun metricsQualityDataReport(qualityReportMessage: QualityReportMessage): Boolean {
        val projectId = qualityReportMessage.projectId
        val statisticsTime = DateTimeUtil.stringToLocalDateTime(qualityReportMessage.statisticsTime, YYYY_MM_DD)
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = metricsThirdPlatformDataReportKey(projectId, statisticsTime.toString()),
            expiredTimeInSeconds = 10
        )
        val currentTime = LocalDateTime.now()
        try {
            lock.lock()
            val metricsThirdPlatformRecord = metricsThirdPlatformInfoDao.getMetricsThirdPlatformInfo(
                dslContext,
                projectId,
                statisticsTime
            )
            val thirdPlatformDatePO = if (metricsThirdPlatformRecord == null) {
                ThirdPlatformDatePO(
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("METRICS_PROJECT_THIRD_PLATFORM_DATA").data ?: 0,
                    projectId = projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = null,
                    repoCodeccAvgScore = null,
                    qualityPipelineExecuteNum = qualityReportMessage.qualityPipelineExecuteNum,
                    qualityPipelineInterceptionNum = qualityReportMessage.qualityPipelineInterceptionNum,
                    turboSaveTime = null,
                    createTime = currentTime,
                    updateTime = currentTime
                )
            } else {
                ThirdPlatformDatePO(
                    id = metricsThirdPlatformRecord.id,
                    projectId = metricsThirdPlatformRecord.projectId,
                    statisticsTime = statisticsTime,
                    resolvedDefectNum = metricsThirdPlatformRecord.resolvedDefectNum,
                    repoCodeccAvgScore = metricsThirdPlatformRecord.repoCodeccAvgScore,
                    qualityPipelineExecuteNum = qualityReportMessage.qualityPipelineExecuteNum,
                    qualityPipelineInterceptionNum = qualityReportMessage.qualityPipelineInterceptionNum,
                    turboSaveTime = metricsThirdPlatformRecord.turboSaveTime,
                    createTime = metricsThirdPlatformRecord.createTime,
                    updateTime = currentTime
                )
            }
            metricsThirdPlatformInfoDao.saveMetricsThirdPlatformData(
                dslContext,
                thirdPlatformDatePO
            )
            logger.info("metricsQualityDataReport thirdPlatformDatePO: $thirdPlatformDatePO")
        } catch (e: Exception) {
            logger.error("metricsQualityDataReport Exception e:$e   qualityReportMessage: $qualityReportMessage")
        } finally {
            lock.unlock()
        }
        return true
    }
}
