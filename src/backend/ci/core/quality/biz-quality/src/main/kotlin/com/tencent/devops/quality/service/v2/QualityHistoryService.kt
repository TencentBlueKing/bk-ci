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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.QualityRuleIntercept
import com.tencent.devops.common.quality.pojo.QualityRuleInterceptRecord
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.quality.tables.records.THistoryRecord
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.quality.constant.BK_BLOCKED
import com.tencent.devops.quality.constant.BK_CURRENT_VALUE
import com.tencent.devops.quality.constant.BK_PASSED
import com.tencent.devops.quality.constant.BK_VALIDATION_INTERCEPTED
import com.tencent.devops.quality.constant.BK_VALIDATION_PASSED
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.dao.v2.QualityRuleReviewerDao
import com.tencent.devops.quality.pojo.QualityRuleBuildHisOpt
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.util.QualityUrlUtils
import com.tencent.devops.quality.util.ThresholdOperationUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class QualityHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleService: QualityRuleService,
    private val qualityRuleBuildHisService: QualityRuleBuildHisService,
    private val historyDao: HistoryDao,
    private val qualityRuleDao: QualityRuleDao,
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityRuleBuildHisOperationService: QualityRuleBuildHisOperationService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val qualityUrlUtils: QualityUrlUtils,
    private val qualityRuleReviewerDao: QualityRuleReviewerDao
) {

    private val logger = LoggerFactory.getLogger(QualityHistoryService::class.java)

    fun userGetRuleIntercept(
        userId: String,
        projectId: String,
        offset: Int,
        limit: Int
    ): Pair<Long, List<QualityRuleIntercept>> {
        val recordList = historyDao.listIntercept(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = null,
            ruleId = null,
            startTime = null,
            endTime = null,
            offset = offset,
            limit = limit
        )
        val count = historyDao.countIntercept(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = null,
            ruleId = null,
            startTime = null,
            endTime = null
        )

        // 批量查询名称信息
        val pipelineIdNameMap = client.get(ServicePipelineResource::class).getPipelineNameByIds(
            projectId = projectId,
            pipelineIds = recordList.map { it.pipelineId }.toSet()).data
            ?: mapOf()
        val ruleIdMap = ruleService.serviceListRuleByIds(
            projectId = projectId,
            ruleIds = recordList.map { it.ruleId }).map { it.hashId to it }.toMap()

        val list = recordList.map {
            val hashId = HashUtil.encodeLongId(it.ruleId)
            QualityRuleIntercept(
                pipelineId = it.pipelineId,
                pipelineName = pipelineIdNameMap[it.pipelineId] ?: "",
                buildId = it.buildId,
                ruleHashId = hashId,
                ruleName = ruleIdMap[hashId]?.name ?: "",
                interceptTime = it.createTime.timestampmilli(),
                result = RuleInterceptResult.valueOf(it.result),
                checkTimes = it.checkTimes,
                resultMsg = objectMapper.readValue(it.interceptList)
            )
        }
        return Pair(count, list)
    }

    fun userGetInterceptHistory(
        userId: String,
        projectId: String,
        ruleHashId: String,
        offset: Int,
        limit: Int
    ): Pair<Long, List<RuleInterceptHistory>> {
        val record = ruleService.serviceGet(ruleHashId)
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        val count = historyDao.count(dslContext, projectId, ruleId)
        val interceptHistoryList = historyDao.listByRuleId(dslContext, projectId, ruleId, offset, limit)

        val pipelineIdList = interceptHistoryList.map { it.pipelineId }
        val pipelineIdToNameMap = getPipelineIdToNameMap(projectId, pipelineIdList.toSet())
        val buildIdList = interceptHistoryList.map { it.buildId }
        val buildIdToNameMap = getBuildIdToNameMap(projectId, buildIdList.toSet())

        val list = interceptHistoryList.filter {
            // 过滤掉已删除的流水线
            pipelineIdToNameMap.containsKey(it.pipelineId)
        }.map {
            val result = RuleInterceptResult.valueOf(it.result)
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)

            val sb = StringBuilder()
            if (result == RuleInterceptResult.PASS) {
                sb.append(MessageUtil.getMessageByLocale(BK_PASSED, I18nUtil.getLanguage(userId)))
            } else {
                sb.append(MessageUtil.getMessageByLocale(BK_BLOCKED, I18nUtil.getLanguage(userId)))
            }

            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append(
                        MessageUtil.getMessageByLocale(
                            BK_CURRENT_VALUE,
                            I18nUtil.getLanguage(userId),
                            arrayOf(
                                intercept.indicatorName,
                                "${intercept.actualValue}",
                                "$thresholdOperationName${intercept.value}"
                            )
                        ) + "\n"
                )
            }
            val remark = sb.toString()

            RuleInterceptHistory(
                hashId = HashUtil.encodeLongId(it.id),
                num = it.projectNum,
                timestamp = it.createTime.timestamp(),
                interceptResult = result,
                ruleHashId = HashUtil.encodeLongId(ruleId),
                ruleName = record.name,
                pipelineId = it.pipelineId,
                pipelineName = pipelineIdToNameMap[it.pipelineId] ?: "",
                buildId = it.buildId,
                buildNo = buildIdToNameMap[it.buildId] ?: "",
                checkTimes = it.checkTimes,
                remark = remark,
                pipelineIsDelete = false
            )
        }
        return Pair(count, list)
    }

    fun serviceListByRuleId(projectId: String, ruleId: Long, offset: Int, limit: Int): Result<THistoryRecord> {
        return historyDao.listByRuleId(
            dslContext = dslContext,
            projectId = projectId,
            ruleId = ruleId,
            offset = offset,
            limit = limit
        )
    }

    fun serviceListByBuildIdAndResult(
        projectId: String,
        pipelineId: String,
        buildId: String,
        result: String
    ): Result<THistoryRecord> {
        return historyDao.listByBuildIdAndResult(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            result = result
        )
    }

    fun serviceListByBuildId(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<QualityRuleIntercept> {
        val interceptHistory = historyDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
        val ruleIdSet = interceptHistory.map { it.ruleId }.toSet()
        val ruleMap = qualityRuleDao.list(
            dslContext = dslContext,
            projectId = projectId,
            ruleIds = ruleIdSet
        )?.map { it.id to it }?.toMap()
        return interceptHistory.sortedByDescending { it.checkTimes }.distinctBy { it.ruleId }.map {
            logger.info("QUALITY|get intercept history: ${it.buildId}, check_time: ${it.checkTimes}")
            QualityRuleIntercept(
                pipelineId = it.pipelineId,
                pipelineName = "",
                buildId = it.buildId,
                ruleHashId = HashUtil.encodeLongId(it.ruleId),
                ruleName = ruleMap?.get(it.ruleId)?.name ?: "",
                interceptTime = it.createTime.timestampmilli(),
                result = RuleInterceptResult.valueOf(it.result),
                checkTimes = it.checkTimes,
                resultMsg = objectMapper.readValue(it.interceptList)
            )
        }
    }

    fun serviceListByRuleAndBuildId(
        projectId: String,
        pipelineId: String,
        buildId: String,
        ruleIds: Collection<String>?
    ): List<QualityRuleIntercept> {
        val interceptHistory = historyDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
        val ruleIdSet = interceptHistory.map { it.ruleId }.toSet()
        val ruleMap = qualityRuleBuildHisDao.list(
            dslContext = dslContext,
            ruleIds = ruleIdSet
        )?.map { it.id to it }?.toMap()
        return interceptHistory.filter { ruleIds?.contains(HashUtil.encodeLongId(it.ruleId)) ?: false }
            .sortedByDescending { it.checkTimes }.distinctBy { it.ruleId }.map {
            logger.info("QUALITY|get rule intercept history: ${it.buildId}, check_time: ${it.checkTimes}")
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
            interceptList.forEach { record ->
                if (CodeccUtils.isCodeccAtom(record.indicatorType)) {
                    record.logPrompt = qualityUrlUtils.getCodeCCUrl(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        detail = record.detail,
                        client = client,
                        logPrompt = null
                    )
                }
            }
            QualityRuleIntercept(
                pipelineId = it.pipelineId,
                pipelineName = "",
                buildId = it.buildId,
                ruleHashId = "",
                ruleName = ruleMap[it.ruleId]?.ruleName ?: "",
                interceptTime = it.createTime.timestampmilli(),
                result = RuleInterceptResult.valueOf(it.result),
                checkTimes = it.checkTimes,
                resultMsg = interceptList
            )
        }
    }

    fun serviceCount(
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Long {
        return historyDao.count(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            ruleId = ruleId,
            result = result,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun serviceList(
        projectId: String,
        pipelineId: String?,
        buildId: String?,
        ruleId: Long?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        offset: Int?,
        limit: Int?
    ): Result<THistoryRecord> {
        return historyDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            ruleId = ruleId,
            result = result,
            startTime = startTime,
            endTime = endTime,
            offset = offset,
            limit = limit
        )
    }

    fun batchServiceList(
        projectId: String,
        pipelineId: String?,
        buildId: String?,
        checkTimes: Int?,
        ruleIds: Set<Long>?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        offset: Int?,
        limit: Int?
    ): Result<THistoryRecord> {
        return historyDao.batchList(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            checkTimes = checkTimes,
            ruleIds = ruleIds,
            result = result,
            startTime = startTime,
            endTime = endTime,
            offset = offset,
            limit = limit
        )
    }

    fun listInterceptHistory(
        userId: String,
        projectId: String,
        pipelineId: String?,
        ruleHashId: String?,
        interceptResult: RuleInterceptResult?,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): Pair<Long, List<RuleInterceptHistory>> {
        val ruleId = if (ruleHashId == null) null else HashUtil.decodeIdToLong(ruleHashId)
        val ruleInterceptResult = interceptResult?.name
        val startLocalDateTime = if (startTime == null) {
            null
        } else {
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneId.systemDefault())
            time.minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong()).minusSeconds(time.second.toLong())
        }
        val endLocalDateTime = if (endTime == null) {
            null
        } else {
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(endTime), ZoneId.systemDefault())
            time.plusDays(1).minusHours(time.hour.toLong())
                .minusMinutes(time.minute.toLong()).minusSeconds(time.second.toLong())
        }

        val count = serviceCount(projectId, pipelineId, ruleId, ruleInterceptResult, startLocalDateTime, endLocalDateTime)
        val recordList = serviceList(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = null,
            ruleId = ruleId,
            result = ruleInterceptResult,
            startTime = startLocalDateTime,
            endTime = endLocalDateTime,
            offset = offset,
            limit = limit
        )

        val ruleIdList = recordList.map { it.ruleId }
        val ruleIdToNameMap = qualityRuleDao.list(
            dslContext = dslContext,
            projectId = projectId,
            ruleIds = ruleIdList.toSet()
        )?.map { it.id to it.name }?.toMap()
        val pipelineIdList = recordList.map { it.pipelineId }
        val pipelineIdToNameMap = getPipelineByIds(projectId = projectId, pipelineIdSet = pipelineIdList.toSet())
            .map { it.pipelineId to it }.toMap()
        val buildIdList = recordList.map { it.buildId }
        val buildIdToNameMap = getBuildIdToNameMap(projectId, buildIdList.toSet())

        val list = recordList.map {
            val sb = StringBuilder()
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append(
                        MessageUtil.getMessageByLocale(
                            BK_CURRENT_VALUE,
                            I18nUtil.getLanguage(userId),
                            arrayOf(
                                intercept.indicatorName,
                                "${intercept.actualValue}",
                                "$thresholdOperationName${intercept.value}"
                            )
                        ) + "\n"
                    )
            }
            val remark = sb.toString()
            val hisRuleHashId = HashUtil.encodeLongId(it.ruleId)
            val pipeline = pipelineIdToNameMap[it.pipelineId]
            val qualityReview = if (it.result == RuleInterceptResult.INTERCEPT_PASS.name ||
                it.result == RuleInterceptResult.INTERCEPT.name) {
                with(qualityRuleReviewerDao.get(
                    dslContext = dslContext,
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    buildId = it.buildId,
                    ruleId = it.ruleId
                )) {
                    QualityRuleBuildHisOpt(
                        ruleHashId = hisRuleHashId,
                        gateOptUser = this?.reviewer,
                        gateOptTime = this?.reviewTime.toString()
                    )
                }
            } else { null }
            RuleInterceptHistory(
                hashId = HashUtil.encodeLongId(it.id),
                num = it.projectNum,
                timestamp = it.createTime.timestamp(),
                interceptResult = RuleInterceptResult.valueOf(it.result),
                ruleHashId = hisRuleHashId,
                ruleName = ruleIdToNameMap?.get(it.ruleId) ?: "",
                pipelineId = it.pipelineId,
                pipelineName = pipeline?.pipelineName ?: "",
                buildId = it.buildId,
                buildNo = buildIdToNameMap[it.buildId] ?: "",
                checkTimes = it.checkTimes,
                remark = remark,
                pipelineIsDelete = pipeline?.isDelete ?: false,
                qualityRuleBuildHisOpt = qualityReview
            )
        }
        return Pair(count, list)
    }

    fun listInterceptHistoryForBuildHis(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildId: String?,
        checkTimes: Int?,
        ruleHashIds: Set<String>
    ): List<RuleInterceptHistory> {
        val ruleBuildIds = ruleHashIds.map { HashUtil.decodeIdToLong(it) }.toSet()

        logger.info("start to list intercept history for pipeline: " +
            "$projectId, $pipelineId, $buildId, ${ruleBuildIds.firstOrNull()}")

        val ruleBuildHis = qualityRuleBuildHisService.list(ruleBuildIds)
        val ruleBuildHisOpts = qualityRuleBuildHisOperationService.listQualityRuleBuildOpt(ruleBuildIds)
        val ruleIdToNameMap = ruleBuildHis.map { it.hashId to it.name }.toMap()
        val recordList = batchServiceList(projectId, pipelineId, buildId, checkTimes, ruleBuildIds,
            null, null, null, null, null)
        if (recordList.isEmpty()) {
            return ruleBuildHis.map { rule ->
                val ruleInterceptRecords = rule.indicators.map {
                    QualityRuleInterceptRecord(
                        indicatorId = it.hashId,
                        indicatorName = it.cnName,
                        indicatorType = null,
                        operation = it.operation,
                        value = it.threshold,
                        actualValue = null,
                        controlPoint = "",
                        pass = false,
                        detail = null,
                        logPrompt = null
                    )
                }
                RuleInterceptHistory(
                    hashId = rule.hashId,
                    num = 0,
                    timestamp = LocalDateTime.now().timestamp(),
                    interceptResult = RuleInterceptResult.UNCHECK,
                    ruleHashId = "",
                    ruleName = rule.name,
                    pipelineId = pipelineId ?: "",
                    pipelineName = "",
                    buildId = buildId ?: "",
                    buildNo = "",
                    checkTimes = 0,
                    remark = "",
                    interceptList = ruleInterceptRecords
                )
            }
        } else {
            return recordList.map {
                val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
                interceptList.forEach { record ->
                    if (CodeccUtils.isCodeccAtom(record.indicatorType)) {
                        record.logPrompt = qualityUrlUtils.getCodeCCUrl(
                            projectId = projectId,
                            pipelineId = pipelineId ?: "",
                            buildId = buildId ?: "",
                            detail = record.detail,
                            client = client,
                            logPrompt = record.logPrompt
                        )
                    }
                }
                val hisRuleHashId = HashUtil.encodeLongId(it.ruleId)
                val buildHis = ruleBuildHis.firstOrNull { it.hashId == hisRuleHashId }
                val ruleBuildHisOpt = ruleBuildHisOpts.firstOrNull { it.ruleHashId == hisRuleHashId }
                val ruleResult = if (buildHis?.status != null) buildHis.status!!.name else it.result
                RuleInterceptHistory(
                    hashId = HashUtil.encodeLongId(it.id),
                    num = it.projectNum,
                    timestamp = it.createTime.timestamp(),
                    interceptResult = RuleInterceptResult.valueOf(ruleResult),
                    ruleHashId = hisRuleHashId,
                    ruleName = ruleIdToNameMap[hisRuleHashId] ?: "",
                    pipelineId = it.pipelineId,
                    pipelineName = "",
                    buildId = it.buildId,
                    buildNo = "",
                    checkTimes = it.checkTimes,
                    remark = "",
                    interceptList = interceptList,
                    qualityRuleBuildHisOpt = QualityRuleBuildHisOpt(
                        HashUtil.encodeLongId(it.id),
                        buildHis?.gateKeepers ?: null,
                        ruleBuildHisOpt?.stageId ?: "",
                        ruleBuildHisOpt?.gateOptUser ?: "",
                        ruleBuildHisOpt?.gateOptTime ?: ""
                    )
                )
            }
        }
    }

    fun userGetInterceptRecent(projectId: String, ruleId: String): String? {
        // 查询拦截状态
        val historyList = serviceListByRuleId(
            projectId = projectId,
            ruleId = HashUtil.decodeIdToLong(ruleId),
            offset = 0,
            limit = 1
        )
        return if (historyList.size < 1) {
            null
        } else {
            val pipelineIdNameMap = getPipelineIdToNameMap(projectId, historyList.map { it.pipelineId }.toSet())
            val interceptHistory = historyList.firstOrNull { pipelineIdNameMap.containsKey(it.pipelineId) }
            if (interceptHistory != null) {
                val result = RuleInterceptResult.valueOf(interceptHistory.result)
                val pipelineName = pipelineIdNameMap[interceptHistory.pipelineId]
                val buildName = getBuildName(projectId, interceptHistory.buildId)
                val time = interceptHistory.createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
                if (result == RuleInterceptResult.PASS) {
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_VALIDATION_PASSED,
                        params = arrayOf("$pipelineName", buildName, time)
                    )
                } else {
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_VALIDATION_INTERCEPTED,
                        params = arrayOf("$pipelineName", buildName, time)
                    )
                }
            } else {
                null
            }
        }
    }

    private fun getPipelineByIds(projectId: String, pipelineIdSet: Set<String>): List<SimplePipeline> {
        return client.get(ServicePipelineResource::class).getPipelineByIds(projectId, pipelineIdSet).data!!
    }

    private fun getPipelineIdToNameMap(projectId: String, pipelineIdSet: Set<String>): Map<String, String> {
        return client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIdSet).data!!
    }

    private fun getBuildIdToNameMap(projectId: String, buildIdSet: Set<String>): Map<String, String> {
        return client.get(ServicePipelineResource::class).getBuildNoByBuildIds(buildIdSet, projectId).data ?: mapOf()
    }

    private fun getBuildName(projectId: String, buildId: String): String {
        val map = getBuildIdToNameMap(projectId, setOf(buildId))
        return map[buildId] ?: ""
    }

    fun serviceCreate(
        projectId: String,
        ruleId: Long,
        pipelineId: String,
        buildId: String,
        result: String,
        interceptList: String,
        createTime: LocalDateTime,
        updateTime: LocalDateTime
    ): Int {
        return historyDao.create(
            dslContext = dslContext,
            projectId = projectId,
            ruleId = ruleId,
            pipelineId = pipelineId,
            buildId = buildId,
            result = result,
            interceptList = interceptList,
            createTime = createTime,
            updateTime = createTime
        )
    }

    fun listQualityRuleBuildHisIntercept(
        userId: String,
        projectId: String,
        pipelineId: String?,
        ruleHashId: String?,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): Pair<Long, List<RuleInterceptHistory>> {
        val ruleId = if (ruleHashId == null) null else HashUtil.decodeIdToLong(ruleHashId)
        val startLocalDateTime = if (startTime == null) {
            null
        } else {
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneId.systemDefault())
            time.minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong()).minusSeconds(time.second.toLong())
        }
        val endLocalDateTime = if (endTime == null) {
            null
        } else {
            val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(endTime), ZoneId.systemDefault())
            time.plusDays(1).minusHours(time.hour.toLong())
                .minusMinutes(time.minute.toLong()).minusSeconds(time.second.toLong())
        }

        val count = serviceCount(projectId, pipelineId, ruleId, null, startLocalDateTime, endLocalDateTime)
        val recordList = serviceList(projectId, pipelineId, null, ruleId, null, startLocalDateTime, endLocalDateTime, offset, limit)

        val ruleIdList = recordList.map { it.ruleId }
        val ruleIdToRuleMap = qualityRuleBuildHisService.list(ruleIdList).map {
            HashUtil.decodeIdToLong(it.hashId) to it
        }.toMap()

        val pipelineIdList = recordList.map { it.pipelineId }
        val pipelineIdToNameMap = getPipelineByIds(projectId = projectId, pipelineIdSet = pipelineIdList.toSet())
            .map { it.pipelineId to it }.toMap()
        val buildIdList = recordList.map { it.buildId }
        val buildIdToNameMap = getBuildIdToNameMap(projectId, buildIdList.toSet())

        recordList.filter { it.result == RuleInterceptResult.FAIL.name }.forEach { record ->
            val buildHisRuleStatus = ruleIdToRuleMap[record.ruleId]?.status
            if (buildHisRuleStatus != null) {
                record.result = buildHisRuleStatus.name
            }
        }

        val list = recordList.map {
            val sb = StringBuilder()
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append(
                    MessageUtil.getMessageByLocale(
                        BK_CURRENT_VALUE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(
                            intercept.indicatorName,
                            "${intercept.actualValue}",
                            "$thresholdOperationName${intercept.value}"
                        )
                    ) + "\n"
                )
            }
            val remark = sb.toString()
            val hisRuleHashId = HashUtil.encodeLongId(it.ruleId)
            val pipeline = pipelineIdToNameMap[it.pipelineId]
            RuleInterceptHistory(
                hashId = HashUtil.encodeLongId(it.id),
                num = it.projectNum,
                timestamp = it.createTime.timestamp(),
                interceptResult = RuleInterceptResult.valueOf(it.result),
                ruleHashId = hisRuleHashId,
                ruleName = ruleIdToRuleMap[it.ruleId]?.name ?: "",
                pipelineId = it.pipelineId,
                pipelineName = pipeline?.pipelineName ?: "",
                buildId = it.buildId,
                buildNo = buildIdToNameMap[it.buildId] ?: "",
                checkTimes = it.checkTimes,
                remark = remark,
                pipelineIsDelete = pipeline?.isDelete ?: false
            )
        }
        return Pair(count, list)
    }
}
