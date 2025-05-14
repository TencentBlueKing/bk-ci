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

package com.tencent.devops.process.service.eplus

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.constants.Constants.BK_TO_HANDLE
import com.tencent.devops.log.dao.PipelineMetricsInfoDao
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.pojo.eplus.PipelineExpirationInfo
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

@Service
class TxPipelineMetricsCronService @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val pipelineMetricsInfoDao: PipelineMetricsInfoDao
) {

    @Value("\${eplus.queryCard.token}")
    private lateinit var token: String

    @Value("\${eplus.ms.process.invalidBuildPipeline.namespace.card.id}")
    private var cardId: Int = 0

    @Value("\${eplus.ms.process.invalidBuildPipeline.namespace.id}")
    private var namespaceId: Int = 0

    @Value("\${eplus.card.query.url}")
    private val cardQueryUrl: String = ""

    @Value("\${eplus.ms.process.pipelineGeneral.namespace.id}")
    private var pipelineGeneralNamespaceId: Int = 0

    @Value("\${eplus.ms.process.highFailureRate30d.namespace.card.id}")
    private var highFailureRate30dCardId: Int = 0

    @Value("\${eplus.ms.process.consecutiveFailures90d.namespace.card.id}")
    private var consecutiveFailures90dCardId: Int = 0

    @Value("\${eplus.ms.process.scheduledTriggerNoCodeChange.namespace.card.id}")
    private var scheduledTriggerNoCodeChangeCardId: Int = 0

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineMetricsCronService::class.java)
    }

    /**
     * 查询并处理卡片数据
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
        var hasMore = true

        // 分页查询已失效流水线数据
        while (hasMore) {
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

            if (rows.isEmpty()) {
                hasMore = false
            } else {
                rows.forEach { row ->
                    tPipelineMetricsInfoRecords.add(
                        TEplusPipelineMetricsDataDailyRecord().apply {
                            assignData(row)
                            this.statisticsTime = LocalDate.now().atStartOfDay()
                        }
                    )
                }
                val total = (result["total"] as? Number)?.toInt() ?: 0
                if (pageNum * pageSize >= total) {
                    hasMore = false
                }
                pageNum++
            }
        }
        metricsData(tPipelineMetricsInfoRecords)
    }

    @Scheduled(cron = "0 0 1 * * ?")
    fun processInvalidPipelineData() {
        logger.info("start processInvalidPipelineData")

        try {
            queryAndProcessCardData(
                cardId = cardId,
                namespaceId = namespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.url = row["n3"] as String
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveInvalidPipelineData(dslContext, records)
                }
            )
        } catch (e: Exception) {
            logger.warn("handle process invalid pipeline data fail", e)
            throw e
        }
        logger.info("end processInvalidPipelineData")
    }

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
            logger.warn("handle  pipeline high failure rate30d data fail", e)
            throw e
        }
        logger.info("end handleHighFailureRate30d")
    }

    @Scheduled(cron = "0 0 1 * * ?")
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
            logger.error("handle consecutive failures90d data fail", e)
            throw e
        }
        logger.info("end handleConsecutiveFailures90d")
    }

    @Scheduled(cron = "0 0 1 * * ?")
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
            logger.error("handle scheduled trigger no code change data fail", e)
            throw e
        }
        logger.info("end handleScheduledTriggerNoCodeChange")
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
     * 查询无效流水线图卡数据
     * @param token 认证token
     * @param dateFrom 开始日期(yyyyMMdd格式)
     * @param dateTo 结束日期(yyyyMMdd格式)
     * @param cardId 图卡ID
     * @param namespaceId 命名空间ID
     * @param queryMode 查询模式
     * @param pageNum 页码
     * @param pageSize 每页大小
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
}
