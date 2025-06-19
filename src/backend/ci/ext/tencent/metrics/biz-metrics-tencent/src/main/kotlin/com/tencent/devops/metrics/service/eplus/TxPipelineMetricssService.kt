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

package com.tencent.devops.metrics.service.eplus

import com.tencent.devops.common.client.Client
import com.tencent.devops.metrics.dao.PipelineMetricsInfoDao
import com.tencent.devops.metrics.pojo.ProjectPipelineIssueAnalysisInfo
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxPipelineMetricssService@Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineMetricsInfoDao: PipelineMetricsInfoDao,
    private val txPipelineMetricsCronService: TxPipelineMetricsCronService
) {

    @Value("\${eplus.ms.metrics.namespace.highFailureRate30d.card.id}")
    private var highFailureRate30dCardId: Int = 0 // 高失败率30天卡片ID

    @Value("\${eplus.ms.metrics.namespace.consecutiveFailures90d.card.id}")
    private var consecutiveFailures90dCardId: Int = 0 // 连续失败90天卡片ID

    @Value("\${eplus.ms.metrics.namespace.scheduledTriggerNoCodeChange.card.id}")
    private var scheduledTriggerNoCodeChangeCardId: Int = 0 // 定时触发无代码变更卡片ID

    fun getPipelineIssueAnalysis(userId: String, projectId: String): ProjectPipelineIssueAnalysisInfo? {
        val verifyUserProjectPermission = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
            userId = userId,
            projectCode = projectId
        ).data
        if (verifyUserProjectPermission != true) {
            logger.info("user ${userId} does not have the permission to view pipeline management information")
            return null
        }

        val failureRateCount = pipelineMetricsInfoDao.countHighFailureRate30d(dslContext, projectId)
        val consecutiveFailuresCount = pipelineMetricsInfoDao.countConsecutiveFailures90d(dslContext, projectId)
        val scheduledTriggerNoCodeChangeCount
        = pipelineMetricsInfoDao.countScheduledTriggerNoCodeChange(dslContext, projectId)

        val cardId = when {
            failureRateCount > 0 -> highFailureRate30dCardId
            consecutiveFailuresCount > 0 -> consecutiveFailures90dCardId
            scheduledTriggerNoCodeChangeCount > 0 -> scheduledTriggerNoCodeChangeCardId
            else -> return null
        }

        return ProjectPipelineIssueAnalysisInfo(
            projectId = projectId,
            failureRateCount = failureRateCount,
            consecutiveFailuresCount = consecutiveFailuresCount,
            scheduledTriggerNoCodeChangeCount = scheduledTriggerNoCodeChangeCount,
            cardId = cardId
        )
    }

    fun runAllSyncDataTasks() {
        txPipelineMetricsCronService.runAllSyncDataTasks()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineMetricssService::class.java)
    }
}
