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
import com.tencent.devops.common.client.Client
import com.tencent.devops.log.dao.PipelineMetricsInfoDao
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import com.tencent.devops.log.pojo.PipelineExpirationInfo
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
     * 查询并处理图卡数据
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
            logger.error("handle consecutive failures90d data fail", e)
            throw e
        }
        logger.info("end handleConsecutiveFailures90d")
    }

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
            logger.error("handle scheduled trigger no code change data fail", e)
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
}
