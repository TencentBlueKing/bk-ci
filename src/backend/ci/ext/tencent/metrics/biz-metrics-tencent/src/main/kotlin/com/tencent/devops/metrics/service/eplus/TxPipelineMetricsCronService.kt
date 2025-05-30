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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.metrics.constants.Constants.BK_TO_HANDLE
import com.tencent.devops.metrics.dao.PipelineMetricsInfoDao
import com.tencent.devops.metrics.pojo.PipelineExpirationInfo
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceUserResource
import java.time.LocalDate
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 腾讯蓝鲸持续集成平台 - 流水线指标定时任务服务
 * 负责定时从Eplus平台获取流水线指标数据并存储到数据库
 */
@Service
class TxPipelineMetricsCronService @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val pipelineMetricsInfoDao: PipelineMetricsInfoDao
) {

    @Value("\${eplus.token}")
    private lateinit var token: String

    @Value("\${eplus.ms.metrics.queryUrl}")
    private val cardQueryUrl: String = ""

    @Value("\${eplus.ms.metrics.namespace.bkciNamespaceId}")
    private var pipelineGeneralNamespaceId: Int = 0

    @Value("\${eplus.ms.metrics.namespace.highFailureRate30d.card.id}")
    private var highFailureRate30dCardId: Int = 0 // 高失败率30天卡片ID

    @Value("\${eplus.ms.metrics.namespace.consecutiveFailures90d.card.id}")
    private var consecutiveFailures90dCardId: Int = 0 // 连续失败90天卡片ID

    @Value("\${eplus.ms.metrics.namespace.scheduledTriggerNoCodeChange.card.id}")
    private var scheduledTriggerNoCodeChangeCardId: Int = 0 // 定时触发无代码变更卡片ID

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineMetricsCronService::class.java)
    }

    /**
     * 查询并处理卡片数据
     * @param cardId 卡片ID
     * @param namespaceId 命名空间ID
     * @param assignData 数据赋值逻辑
     * @param metricsData 数据处理逻辑
     */
    private fun queryAndProcessCardData(
        cardId: Int,
        namespaceId: Int,
        assignData: TEplusPipelineMetricsDataDailyRecord.(Map<String, Any>) -> Unit,
        metricsData: (List<TEplusPipelineMetricsDataDailyRecord>) -> Unit
    ) {
        val tPipelineMetricsInfoRecords = mutableListOf<TEplusPipelineMetricsDataDailyRecord>()
        var pageNum = 1
        val pageSize = 1000
        var failedBatches = 0

        while (true) {
            try {
                val response = queryInvalidPipelineMonitorCardData(
                    token = token,
                    cardId = cardId,
                    namespaceId = namespaceId,
                    pageNum = pageNum,
                    pageSize = pageSize
                )
                val data = response["data"] as Map<String, Any>
                val result = data["result"] as Map<String, Any>
                val rows = result["rows"] as List<Map<String, Any>>

                if (rows.isEmpty()) break

                rows.forEach { row ->
                    tPipelineMetricsInfoRecords.add(
                        TEplusPipelineMetricsDataDailyRecord().apply {
                            assignData(row)
                            this.statisticsTime = LocalDate.now().atStartOfDay()
                        }
                    )
                }

                if (rows.size < pageSize) break
                pageNum++
            } catch (e: Exception) {
                failedBatches++
                logger.warn("Process batch failed (pageNum: $pageNum), will retry next page", e)
                pageNum++ // 递增页码，跳过当前失败页
                continue
            }
        }

        if (failedBatches > 0) {
            logger.warn("Process completed with $failedBatches failed batches")
        }

        metricsData(tPipelineMetricsInfoRecords)
    }

    /**
     * 处理高失败率30天数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun handleHighFailureRate30d() {
        logger.info("start handleHighFailureRate30d")
        try {
            queryAndProcessCardData(
                cardId = highFailureRate30dCardId,
                namespaceId = pipelineGeneralNamespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.failureRate_30d = true
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveHighFailureRate30dData(dslContext, records)
                }
            )
        } catch (e: Exception) {
            logger.warn("handle pipeline high failure rate30d data fail", e)
            throw e
        }
        logger.info("end handleHighFailureRate30d")
    }

    /**
     * 处理连续失败90天数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun handleConsecutiveFailures90d() {
        logger.info("start handleConsecutiveFailures90d")
        try {
            queryAndProcessCardData(
                cardId = consecutiveFailures90dCardId,
                namespaceId = pipelineGeneralNamespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.consecutiveFailures_90d = true
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveConsecutiveFailures90dData(dslContext, records)
                }
            )
        } catch (e: Exception) {
            logger.warn("handle consecutive failures90d data fail", e)
            throw e
        }
        logger.info("end handleConsecutiveFailures90d")
    }

    /**
     * 处理定时触发无代码变更数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    fun handleScheduledTriggerNoCodeChange() {
        logger.info("start handleScheduledTriggerNoCodeChange")
        try {
            queryAndProcessCardData(
                cardId = scheduledTriggerNoCodeChangeCardId,
                namespaceId = pipelineGeneralNamespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.scheduledTriggerNoCodeChange = true
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveScheduledTriggerNoCodeChangeData(dslContext, records)
                }
            )
        } catch (e: Exception) {
            logger.warn("handle scheduled trigger no code change data fail", e)
            throw e
        }
        logger.info("end handleScheduledTriggerNoCodeChange")
    }

    private fun postWithToken(
        url: String,
        token: String,
        requestBody: Map<String, Any>
    ): Map<String, Any> {
        val jsonBody = objectMapper.writeValueAsString(requestBody)
        val request = Request.Builder()
            .url(url)
            .addHeader("token", token)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                throw RemoteServiceException("request failed, status code: ${response.code}")
            }
            val responseStr = response.body!!.string()
            return objectMapper.readValue(responseStr)
        }
    }

    /**
     * 查询无效流水线监控卡片数据
     * @param token 认证Token
     * @param cardId 卡片ID
     * @param namespaceId 命名空间ID
     * @param queryMode 查询模式
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 响应数据
     */
    fun queryInvalidPipelineMonitorCardData(
        token: String,
        cardId: Int,
        namespaceId: Int,
        queryMode: Int = 2,
        pageNum: Int = 1,
        pageSize: Int = 1000
    ): Map<String, Any> {
        val requestBody = mapOf(
            "card_id" to cardId,
            "namespace_id" to namespaceId,
            "query_mode" to queryMode,
            "page" to mapOf(
                "num" to pageNum,
                "size" to pageSize
            )
        )

        return postWithToken(
            url = cardQueryUrl,
            token = token,
            requestBody = requestBody
        )
    }

    private fun fetchInvalidPipelineProjectIds(limit: Int, offset: Int): List<String> {
        return pipelineMetricsInfoDao.listInvalidPipelineProjectIds(dslContext, limit, offset)
    }

    /**
     * 每隔两周周一10点发送项目无效流水线监控报告
     */
    @Scheduled(cron = "0 0 10 ? * MON/2")
    fun sendInvalidPipelineMonitorReport() {
        logger.info("starts the task of sending a report email")
        var offset = 0
        val limit = 100
        do {
            val projectIds = fetchInvalidPipelineProjectIds(limit, offset)
            projectIds.forEach { projectId ->
                sendReportForProject(projectId)
            }
            offset += limit
        } while (projectIds.size == limit)
    }

    private fun sendReportForProject(projectId: String) {
        val invalidPipelineMap = pipelineMetricsInfoDao.listProjectInvalidPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            statisticsTime = LocalDate.now().atStartOfDay()
        ).map { it.value1() to it.value2() }.toMap()
        val projectPipelineInfo = convertPipelineExpirationInfo(projectId, invalidPipelineMap)

        try {
            if (projectPipelineInfo != null) {
                sendProjectReport(projectId, projectPipelineInfo)
            }
            logger.info("report email for the project [$projectId] was successfully sent")
        } catch (e: Exception) {
            logger.error("Failed to send project [$projectId] report email", e)
        }
    }

    /**
     * 获取所有需要发送报告的项目流水线信息
     */
    private fun convertPipelineExpirationInfo(
        projectId: String,
        invalidPipelineMap: Map<String, String>
    ): PipelineExpirationInfo? {

        val projectManagers =
            client.get(ServiceUserResource::class).getProjectUserRoles(projectId, BkAuthGroup.MANAGER).data
        if (projectManagers.isNullOrEmpty()) {
            return null
        }
        return PipelineExpirationInfo(
            receivers = projectManagers,
            projectId = projectId,
            pipelineIds = invalidPipelineMap.keys.toList(),
            linksMap = invalidPipelineMap
        )
    }

    /**
     * 发送单个项目的报告邮件
     */
    private fun sendProjectReport(projectId: String, info: PipelineExpirationInfo) {
        try {
            // 准备邮件模板请求
            val request = SendNotifyMessageTemplateRequest(
                templateCode = "PIPELINE_EXPIRATION_NOTIFICATION",
                receivers = info.receivers.toMutableSet(),
                notifyType = mutableSetOf(NotifyType.EMAIL.name),
                bodyParams = mapOf(
                    "projectName" to info.projectId,
                    "pipelineCount" to info.pipelineIds.size.toString(),
                    "pipelineList" to info.pipelineIds.joinToString("\n") {
                        "${it}:${info.linksMap[it]}"
                    },
                    "table" to buildPipelineTableHtml(info),
                    "eplusUrl" to "${HomeHostUtil.innerServerHost()}/console/metrics/${projectId}"
                ),
                markdownContent = false
            )

            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request).let { result ->
                if (result.isNotOk() || result.data != true) {
                    throw RemoteServiceException("send email fail: ${result.message}")
                }
            }
        } catch (e: Exception) {
            logger.warn("send project[${info.projectId}] invalid pipeline email fail", e)
            throw e
        }
    }

    private fun buildPipelineTableHtml(info: PipelineExpirationInfo): String {
        val rows = info.pipelineIds.joinToString("") { pipelineId ->
            """
            <tr>
                <td style="border-bottom: 1px solid #ddd; padding: 10px;">$pipelineId</td>
                <td style="border-bottom: 1px solid #ddd; padding: 10px;">
                    <a href="${info.linksMap[pipelineId]}" style="color: #3A84FF;">${
                I18nUtil.getCodeLanMessage(messageCode = BK_TO_HANDLE)
            }</a>
                </td>
            </tr>
            """
        }
        return rows
    }
}
