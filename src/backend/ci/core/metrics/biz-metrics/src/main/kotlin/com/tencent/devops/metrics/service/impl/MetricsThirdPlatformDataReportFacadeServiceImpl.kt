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

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.event.pojo.measure.QualityReportEvent
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.metrics.api.ServiceMetricsDataReportResource
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.pojo.dto.CodeccDataReportDTO
import com.tencent.devops.metrics.pojo.dto.QualityDataReportDTO
import com.tencent.devops.metrics.pojo.dto.TurboDataReportDTO
import com.tencent.devops.metrics.pojo.message.CodeCheckReportEvent
import com.tencent.devops.metrics.pojo.message.TurboReportEvent
import com.tencent.devops.metrics.service.MetricsThirdPlatformDataReportFacadeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MetricsThirdPlatformDataReportFacadeServiceImpl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val bkTag: BkTag,
    private val metricsConfig: MetricsConfig
) : MetricsThirdPlatformDataReportFacadeService {

    private val logger = LoggerFactory.getLogger(MetricsThirdPlatformDataReportFacadeService::class.java)

    override fun metricsCodeCheckDataReport(codeCheckReportEvent: CodeCheckReportEvent): Boolean {
        val projectId = codeCheckReportEvent.projectId
        val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)
        // 判断该项目是否需要上报度量数据
        val unReportClusterTags = metricsConfig.unReportClusterTags.split(",")
        if (unReportClusterTags.contains(projectConsulTag)) {
            return true
        }
        return bkTag.invokeByTag(projectConsulTag) {
            val bkFinalTag = bkTag.getFinalTag()
            logger.info("start call ServiceMetricsDataReportResource api $projectId|$projectConsulTag|$bkFinalTag")
            val codeccDataReportDTO = CodeccDataReportDTO(
                statisticsTime = codeCheckReportEvent.statisticsTime,
                projectId = projectId,
                repoCodeccAvgScore = codeCheckReportEvent.repoCodeccAvgScore,
                resolvedDefectNum = codeCheckReportEvent.resolvedDefectNum
            )
            client.getGateway(ServiceMetricsDataReportResource::class)
                .metricsCodeccDataReport(codeccDataReportDTO).data ?: false
        }
    }

    override fun metricsTurboDataReport(turboReportEvent: TurboReportEvent): Boolean {
        val projectId = turboReportEvent.projectId
        val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)
        // 判断该项目是否需要上报度量数据
        val unReportClusterTags = metricsConfig.unReportClusterTags.split(",")
        if (unReportClusterTags.contains(projectConsulTag)) {
            return true
        }
        return bkTag.invokeByTag(projectConsulTag) {
            val bkFinalTag = bkTag.getFinalTag()
            logger.info("start call ServiceMetricsDataReportResource api $projectId|$projectConsulTag|$bkFinalTag")
            val turboDataReportDTO = TurboDataReportDTO(
                statisticsTime = turboReportEvent.statisticsTime,
                projectId = projectId,
                turboSaveTime = turboReportEvent.turboSaveTime
            )
            client.getGateway(ServiceMetricsDataReportResource::class)
                .metricsTurboDataReport(turboDataReportDTO).data ?: false
        }
    }

    override fun metricsQualityDataReport(qualityReportEvent: QualityReportEvent): Boolean {
        val projectId = qualityReportEvent.projectId
        val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, projectId)
        // 判断该项目是否需要上报度量数据
        val unReportClusterTags = metricsConfig.unReportClusterTags.split(",")
        if (unReportClusterTags.contains(projectConsulTag)) {
            return true
        }
        return bkTag.invokeByTag(projectConsulTag) {
            val bkFinalTag = bkTag.getFinalTag()
            logger.info("start call ServiceMetricsDataReportResource api $projectId|$projectConsulTag|$bkFinalTag")
            val qualityDataReportDTO = QualityDataReportDTO(
                statisticsTime = qualityReportEvent.statisticsTime,
                projectId = projectId,
                qualityPipelineInterceptionNum = qualityReportEvent.interceptedCount,
                qualityPipelineExecuteNum = qualityReportEvent.totalCount
            )
            client.getGateway(ServiceMetricsDataReportResource::class)
                .metricsQualityDataReport(qualityDataReportDTO).data ?: false
        }
    }
}
