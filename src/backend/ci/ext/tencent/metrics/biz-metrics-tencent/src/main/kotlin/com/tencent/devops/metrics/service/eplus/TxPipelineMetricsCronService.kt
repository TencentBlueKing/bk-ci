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
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.metrics.constants.Constants.BK_TO_HANDLE
import com.tencent.devops.metrics.dao.PipelineMetricsInfoDao
import com.tencent.devops.metrics.pojo.PipelineExpirationInfo
import com.tencent.devops.model.metrics.tables.records.TEplusPipelineMetricsDataDailyRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServiceTXPipelineResource
import com.tencent.devops.project.api.service.ServiceUserResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.ceil
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
    private val pipelineMetricsInfoDao: PipelineMetricsInfoDao,
    private val redisOperation: RedisOperation,
    private val tokenService: ClientTokenService
) {

    @Value("\${eplus.token}")
    private lateinit var token: String

    @Value("\${eplus.ms.metrics.queryUrl}")
    private val cardQueryUrl: String = ""

    @Value("\${eplus.ms.metrics.filterId}")
    private val filterId: String = ""

    @Value("\${eplus.ms.metrics.namespace.bkciNamespaceId}")
    private var pipelineGeneralNamespaceId: Int = 0

    @Value("\${eplus.ms.metrics.namespace.highFailureRate30d.card.id}")
    private var highFailureRate30dCardId: Int = 0 // 高失败率30天卡片ID

    @Value("\${eplus.ms.metrics.namespace.consecutiveFailures90d.card.id}")
    private var consecutiveFailures90dCardId: Int = 0 // 连续失败90天卡片ID

    @Value("\${eplus.ms.metrics.namespace.scheduledTriggerNoCodeChange.card.id}")
    private var scheduledTriggerNoCodeChangeCardId: Int = 0 // 定时触发无代码变更卡片ID

    @Value("\${eplus.ms.metrics.queryCardsPageSize:10000}")
    private val queryCardsPageSize: Int = 10000

    @Value("\${eplus.ms.metrics.sleepDurationMs:60000}")
    private val sleepDurationMs: Long = 60000

    @Value("\${eplus.ms.metrics.enableFlag:false}")
    private val enableFlag: Boolean = false

    @Value("\${eplus.ms.metrics.readTimeoutSeconds:300}")
    private val readTimeoutSeconds: Long = 300

    @Value("\${eplus.ms.metrics.namespace.invalidBuildPipeline.card.id}")
    private var invalidBuildPipelineCardId: Int = 0 // 无效流水线卡片ID

    @Value("\${eplus.ms.metrics.namespace.consecutiveFailures6m.card.id}")
    private var consecutiveFailures6mCardId: Int = 0 // 近6个月内持续失败流水线卡片ID

    @Value("\${eplus.ms.metrics.panelId}")
    private val panelId: Int = 0

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineMetricsCronService::class.java)
        // HTTP请求超时时间（秒）
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val WRITE_TIMEOUT_SECONDS = 15L
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
        metricsData: (List<TEplusPipelineMetricsDataDailyRecord>) -> Unit,
        input: Map<String, Any>? = null
    ) {
        val lockKey = "CARD_DATA_PROCESS:${cardId}:${namespaceId}"
        val redisLock = RedisLock(redisOperation, lockKey, 600)
        if (!redisLock.tryLock()) return

        var pageNum = 1
        var failedPageAttempts = 0
        var totalPages: Int? = null

        while (totalPages == null || pageNum <= totalPages) {
            Thread.sleep(sleepDurationMs)
            try {
                val response = queryInvalidPipelineMonitorCardData(
                    token = token,
                    cardId = cardId,
                    namespaceId = namespaceId,
                    pageNum = pageNum,
                    pageSize = queryCardsPageSize,
                    input = input
                )
                if (response.isEmpty()) return
                val (currentTotalPages, records) = processPageData(response, assignData, totalPages)
                totalPages = currentTotalPages

                metricsData(records)
                pageNum++
                failedPageAttempts = 0
            } catch (ignored: Throwable) {
                logger.warn("Process page $pageNum failed: ${ignored.message}")
                when {
                    ignored is RemoteServiceException -> throw ignored
                    ++failedPageAttempts >= 3 -> {
                        logger.warn("Skipping page $pageNum after 3 attempts")
                        break
                    }
                }
            } finally {
                redisLock.unlock()
            }
        }
    }

    private fun processPageData(
        response: Map<String, Any>,
        assignData: TEplusPipelineMetricsDataDailyRecord.(Map<String, Any>) -> Unit,
        currentTotalPages: Int?
    ): Pair<Int?, List<TEplusPipelineMetricsDataDailyRecord>> {
        val data = response["data"] as? Map<String, Any>
            ?: throw RemoteServiceException("Invalid response data: ${response["message"]}")

        val result = data["result"] as? Map<String, Any>
            ?: throw RemoteServiceException("Missing result field")

        val rows = result["rows"] as? List<Map<String, Any>>
            ?: throw RemoteServiceException("Invalid rows format")

        var totalPages = currentTotalPages
        if (totalPages == null) {
            val totalItems = result["total"] as? Int ?: 0
            totalPages = if (totalItems > 0) ceil(totalItems.toDouble() / queryCardsPageSize).toInt() else 0
        }

        val records = rows.map { row ->
            TEplusPipelineMetricsDataDailyRecord().apply {
                assignData(row)
                statisticsTime = LocalDate.now().atStartOfDay()
            }
        }

        return Pair(totalPages, records)
    }

    /**
     * 处理高失败率30天数据
     */
    @Scheduled(cron = "0 0 8 * * ?")
    fun handleHighFailureRate30d() {
        if (!enableFlag) return
        logger.info("start handleHighFailureRate30d")
        try {
            val dateTimeFrom = LocalDate.now()
                .minusDays(30)
                .atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val dateTimeTo = LocalDate.now()
                .minusDays(1)
                .atTime(23, 59, 59)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

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
                },
                input = mapOf(
                    "input" to listOf(
                        mapOf(
                            "type" to 2,
                            "value" to mapOf(
                                "op" to "day_between",
                                "value" to "$dateTimeFrom,$dateTimeTo"
                            ),
                            "name" to filterId
                        )
                    )
                )
            )
        } catch (ignored: Throwable) {
            logger.warn("handle pipeline high failure rate30d data fail: ${ignored.message}")
        }
        logger.info("end handleHighFailureRate30d")
    }

    /**
     * 处理连续失败90天数据
     */
    @Scheduled(cron = "0 0 8 * * ?")
    fun handleConsecutiveFailures90d() {
        if (!enableFlag) return
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
        } catch (ignored: Throwable) {
            logger.warn("handle consecutive failures90d data fail: ${ignored.message}")
        }
        logger.info("end handleConsecutiveFailures90d")
    }

    /**
     * 处理定时触发无代码变更数据
     */
    @Scheduled(cron = "0 0 8 * * ?")
    fun handleScheduledTriggerNoCodeChange() {
        if (!enableFlag) return
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
        } catch (ignored: Throwable) {
            logger.warn("handle scheduled trigger no code change data fail: ${ignored.message}")
        }
        logger.info("end handleScheduledTriggerNoCodeChange")
    }

    /**
     * 调用所有同步数据方法
     */
    fun runAllSyncDataTasks() {
        val syncExecutorService = Executors.newFixedThreadPool(5)
        try {
            logger.info("start runAllSyncDataTasks")
            pipelineMetricsInfoDao.cleanTodayData(dslContext)
            syncExecutorService.submit { handleHighFailureRate30d() }
            syncExecutorService.submit { handleConsecutiveFailures90d() }
            syncExecutorService.submit { handleScheduledTriggerNoCodeChange() }
            syncExecutorService.submit { processInvalidPipelineData() }
            syncExecutorService.submit { handleConsecutiveFailures6m() }
            logger.info("end runAllSyncDataTasks")
        } finally {
            syncExecutorService.shutdown()
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    fun processInvalidPipelineData() {
        if (!enableFlag) return
        logger.info("start processInvalidPipelineData")
        try {
            queryAndProcessCardData(
                cardId = invalidBuildPipelineCardId,
                namespaceId = pipelineGeneralNamespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.invalidPipelineFlag = true
                    this.url = row["n2"] as String
                    this.pipelineName = row["n1"] as String
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveInvalidPipelineData(dslContext, records)
                }
            )
        } catch (ignored: Throwable) {
            logger.warn("handle process invalid pipeline data fail: ${ignored.message}")
            throw ignored
        }
        logger.info("end processInvalidPipelineData")
    }

    /**
     * 处理近6个月内持续失败数据
     */
    @Scheduled(cron = "0 0 8 * * ?")
    fun handleConsecutiveFailures6m() {
        if (!enableFlag) return
        logger.info("start handleConsecutiveFailures6m")
        try {
            val dateTimeFrom = LocalDate.now()
                .minusDays(30)
                .atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val dateTimeTo = LocalDate.now()
                .minusDays(1)
                .atTime(23, 59, 59)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            queryAndProcessCardData(
                cardId = consecutiveFailures6mCardId,
                namespaceId = pipelineGeneralNamespaceId,
                assignData = { row ->
                    this.projectId = row["project_id"] as String
                    this.pipelineId = row["pipeline_id"] as String
                    this.consecutiveFailures_6m = true
                },
                metricsData = { records ->
                    pipelineMetricsInfoDao.batchSaveConsecutiveFailures6mData(dslContext, records)
                },
                input = mapOf(
                    "input" to listOf(
                        mapOf(
                            "type" to 2,
                            "value" to mapOf(
                                "op" to "day_between",
                                "value" to "$dateTimeFrom,$dateTimeTo"
                            ),
                            "name" to filterId
                        )
                    )
                )
            )
        } catch (ignored: Throwable) {
            logger.warn("handle pipeline consecutive failures 6m data fail: ${ignored.message}")
        }
        logger.info("end handleConsecutiveFailures6m")
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
        pageSize: Int = queryCardsPageSize,
        input: Map<String, Any>? = null
    ): Map<String, Any> {
        val requestBody = mutableMapOf(
            "bindings" to {},
            "card_id" to cardId,
            "namespace_id" to namespaceId,
            "query_mode" to queryMode,
            "page" to mapOf(
                "num" to pageNum,
                "size" to pageSize
            ),
            "panel_id" to panelId
        )
        if (input != null) {
            requestBody.putAll(input)
        }

        val response = OkhttpUtils.doCustomTimeoutPost(
            connectTimeout = CONNECT_TIMEOUT_SECONDS,
            readTimeout = readTimeoutSeconds,
            writeTimeout = WRITE_TIMEOUT_SECONDS,
            url = cardQueryUrl,
            jsonParam = JsonUtil.toJson(requestBody),
            headers = mapOf(
                "token" to token,
                "Content-Type" to "application/json"
            )
        )
        if (!response.isSuccessful) {
            throw RemoteServiceException("queryInvalidPipelineMonitorCardData request failed: ${response.message}")
        }
        val responseStr = response.body?.string()
        return responseStr?.let { JsonUtil.toMap(it) } ?: emptyMap()
    }

    private fun fetchInvalidPipelineProjectIds(limit: Int, offset: Int): List<String> {
        return pipelineMetricsInfoDao.listInvalidPipelineProjectIds(dslContext, limit, offset)
    }

    /**
     * 通用分页处理方法
     * @param fetchProjectIds 获取项目ID列表的函数
     * @param processProject 处理单个项目的函数
     * @param limit 每页大小
     */
    private fun processByProjectIdsInPages(
        fetchProjectIds: (Int, Int) -> List<String>,
        processProject: (String) -> Unit,
        limit: Int = 100
    ) {
        var offset = 0
        var projectIds: List<String>

        do {
            projectIds = fetchProjectIds(limit, offset)
            projectIds.forEach { projectId ->
                try {
                    processProject(projectId)
                } catch (ignored: Throwable) {
                    logger.warn("Error processing project $projectId message: ${ignored.message}")
                }
            }
            offset += limit
        } while (projectIds.size == limit)
    }

    /**
     * 每隔两周周一10点发送项目无效流水线监控报告
     */
    @Scheduled(cron = "0 0 10 ? * MON")
    fun sendInvalidPipelineMonitorReport() {
        if (!enableFlag) return

        val lockKey = "SEND_INVALID_PIPELINE_MONITOR_REPORT"
        val redisLock = RedisLock(redisOperation, lockKey, 600)

        val today = LocalDate.now()
        val weekNumber = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())

        if (!redisLock.tryLock()) {
            return
        }

        try {
            if (weekNumber % 2 != 0) {
                logger.info("Not an even week, skip sending report")
                return
            }

            logger.info("Starting the task of sending report emails")

            processByProjectIdsInPages(
                fetchProjectIds = { limit, offset ->
                    fetchInvalidPipelineProjectIds(limit, offset)
                },
                processProject = { projectId ->
                    sendReportForProject(projectId)
                }
            )
        } catch (e: Throwable) {
            logger.warn("Error in sendInvalidPipelineMonitorReport", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun sendReportForProject(projectId: String) {
        // 获取所有无效流水线
        val allInvalidPipelines = pipelineMetricsInfoDao.listProjectInvalidPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            statisticsTime = LocalDate.now().atStartOfDay()
        ).map { it.pipelineId to Pair(it.pipelineName, it.url) }.toMap()

        if (allInvalidPipelines.isEmpty()) return

        // 获取该项目的已禁用流水线ID列表
        val disabledPipelines = client.get(ServiceTXPipelineResource::class).listDisabledPipelines(projectId).data
            ?: emptyList()

        // 获取该项目的白名单流水线
        val whitelistPipelines = pipelineMetricsInfoDao.listAutoDisableWhitelist(
            dslContext = dslContext,
            projectId = projectId
        ).toSet()

        // 过滤掉白名单和已禁用的流水线
        val filteredInvalidPipelines = allInvalidPipelines.filterKeys {
            !whitelistPipelines.contains(it) && !disabledPipelines.contains(it)
        }

        if (filteredInvalidPipelines.isEmpty()) return

        val projectPipelineInfo = convertPipelineExpirationInfo(projectId, filteredInvalidPipelines)

        try {
            if (projectPipelineInfo != null) {
                sendProjectReport(projectId, projectPipelineInfo)
            }
            logger.info("report email for the project [$projectId] was successfully sent")
        } catch (ignored: Throwable) {
            logger.warn("Failed to send project [$projectId] report email message: ${ignored.message}")
        }
    }

    /**
     * 获取所有需要发送报告的项目流水线信息
     */
    private fun convertPipelineExpirationInfo(
        projectId: String,
        invalidPipelineMap: Map<String, Pair<String, String>>
    ): PipelineExpirationInfo? {

        val projectManagers =
            client.get(ServiceUserResource::class).getProjectUserRoles(projectId, BkAuthGroup.MANAGER).data
        if (projectManagers.isNullOrEmpty()) {
            return null
        }
        return PipelineExpirationInfo(
            receivers = projectManagers,
            projectId = projectId,
            pipelineInfoMap = invalidPipelineMap
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
                    "pipelineCount" to info.pipelineInfoMap.size.toString(),
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
        } catch (ignored: Throwable) {
            logger.warn("send project[${info.projectId}] invalid pipeline email fail", ignored.message)
            throw ignored
        }
    }

    private fun buildPipelineTableHtml(info: PipelineExpirationInfo): String {
        val rows = info.pipelineInfoMap.values.joinToString("") { pipelineInfo ->
            """
            <tr>
                <td style="border-bottom: 1px solid #ddd; padding: 10px;">${pipelineInfo.first}</td>
                <td style="border-bottom: 1px solid #ddd; padding: 10px;">
                    <a href="${pipelineInfo.second}" style="color: #3A84FF;">${
                I18nUtil.getCodeLanMessage(messageCode = BK_TO_HANDLE)
            }</a>
                </td>
            </tr>
            """
        }
        return rows.replace("\n", "")
    }

    /**
     * 每周一11点禁用连续失败6个月的流水线
     */
    @Scheduled(cron = "0 0 11 ? * MON")
    fun disableConsecutiveFailures6mPipelines() {
        if (!enableFlag) return
        val lockKey = "DISABLE_CONSECUTIVE_FAILURES_6M_PIPELINES"
        val redisLock = RedisLock(redisOperation, lockKey, 600)
        if (!redisLock.tryLock()) {
            return
        }
        try {
            logger.info("start disableConsecutiveFailures6mPipelines")
            val currentStatisticsTime = LocalDate.now().atStartOfDay()

            processByProjectIdsInPages(
                fetchProjectIds = { limit, offset ->
                    pipelineMetricsInfoDao.listConsecutiveFailures6mProjectIds(
                        dslContext = dslContext,
                        limit = limit,
                        offset = offset
                    )
                },
                processProject = processProject@{ projectId ->
                    // 获取该项目的连续失败流水线
                    val records = pipelineMetricsInfoDao.listConsecutiveFailures6mPipelines(
                        dslContext = dslContext,
                        statisticsTime = currentStatisticsTime,
                        projectId = projectId
                    )

                    if (records.isEmpty()) return@processProject

                    // 获取项目白名单
                    val whitelist = pipelineMetricsInfoDao.listAutoDisableWhitelist(dslContext, projectId).toSet()

                    // 获取已禁用的流水线
                    val disabledPipelines = client.get(ServiceTXPipelineResource::class).listDisabledPipelines(
                            projectId
                        ).data ?: emptyList<String>()

                    // 过滤非白名单且未禁用的流水线
                    val pipelineIds = records
                        .filterNot { whitelist.contains(it.pipelineId) }
                        .filterNot { disabledPipelines.contains(it.pipelineId) }
                        .map { it.pipelineId }

                    val manager = client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
                        token = tokenService.getSystemToken(),
                        projectCode = projectId,
                        resourceType = ResourceTypeId.PROJECT,
                        resourceCode = projectId,
                        group = BkAuthGroup.MANAGER
                    ).data?.random() ?: return@processProject

                    if (pipelineIds.isNotEmpty()) {
                        val result = client.get(ServiceTXPipelineResource::class).lockPipeline(
                            userId = manager,
                            projectId = projectId,
                            pipelineIds = pipelineIds,
                            enable = false
                        )

                        if (result.isNotOk() || result.data != true) {
                            logger.warn("Failed to disable pipelines in project $projectId: ${result.message}")
                        } else {
                            logger.info("Successfully disabled ${pipelineIds.size} pipelines in project $projectId")
                        }
                    }
                }
            )

            logger.info("end disableConsecutiveFailures6mPipelines")
        } catch (ignored: Throwable) {
            logger.warn("Error in disableConsecutiveFailures6mPipelines message: ${ignored.message}")
        } finally {
            redisLock.unlock()
        }
    }
}
