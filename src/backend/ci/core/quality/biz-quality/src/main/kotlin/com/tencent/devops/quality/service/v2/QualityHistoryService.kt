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
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.QualityRuleIntercept
import com.tencent.devops.common.quality.pojo.QualityRuleInterceptRecord
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.model.quality.tables.records.THistoryRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.pojo.QualityRuleBuildHisOpt
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.util.ThresholdOperationUtil
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
@Suppress("ALL")
class QualityHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleService: QualityRuleService,
    private val qualityRuleBuildHisService: QualityRuleBuildHisService,
    private val historyDao: HistoryDao,
    private val qualityRuleBuildHisOperationService: QualityRuleBuildHisOperationService,
    private val client: Client,
    private val objectMapper: ObjectMapper
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
        val buildIdToNameMap = getBuildIdToNameMap(buildIdList.toSet())

        val list = interceptHistoryList.filter {
            // 过滤掉已删除的流水线
            pipelineIdToNameMap.containsKey(it.pipelineId)
        }.map {
            val result = RuleInterceptResult.valueOf(it.result)
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)

            val sb = StringBuilder()
            if (result == RuleInterceptResult.PASS) {
                sb.append("已通过：")
            } else {
                sb.append("已拦截：")
            }

            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append("${intercept.indicatorName}当前值(${intercept.actualValue})，" +
                    "期望$thresholdOperationName${intercept.value}\n")
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

    fun serviceListByBuildId(projectId: String, pipelineId: String, buildId: String): List<QualityRuleIntercept> {
        return historyDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        ).map {
            QualityRuleIntercept(
                pipelineId = it.pipelineId,
                pipelineName = "",
                buildId = it.buildId,
                ruleHashId = "",
                ruleName = "",
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
        return historyDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        ).filter { ruleIds?.contains(HashUtil.encodeLongId(it.ruleId)) ?: false }.map {
            QualityRuleIntercept(
                pipelineId = it.pipelineId,
                pipelineName = "",
                buildId = it.buildId,
                ruleHashId = "",
                ruleName = "",
                interceptTime = it.createTime.timestampmilli(),
                result = RuleInterceptResult.valueOf(it.result),
                checkTimes = it.checkTimes,
                resultMsg = objectMapper.readValue(it.interceptList)
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
        val recordList = serviceList(projectId, pipelineId, null, ruleId, ruleInterceptResult, startLocalDateTime, endLocalDateTime, offset, limit)

        val ruleIdList = recordList.map { it.ruleId }
        val ruleIdToNameMap = ruleService.serviceListRuleByIds(projectId = projectId, ruleIds = ruleIdList.toSet())
            .map { it.hashId to it.name }.toMap()
        val pipelineIdList = recordList.map { it.pipelineId }
        val pipelineIdToNameMap = getPipelineByIds(projectId = projectId, pipelineIdSet = pipelineIdList.toSet())
            .map { it.pipelineId to it }.toMap()
        val buildIdList = recordList.map { it.buildId }
        val buildIdToNameMap = getBuildIdToNameMap(buildIdList.toSet())

        val list = recordList.map {
            val sb = StringBuilder()
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append("${intercept.indicatorName}当前值(${intercept.actualValue})，")
                    .append("期望$thresholdOperationName${intercept.value}\n")
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
                ruleName = ruleIdToNameMap[hisRuleHashId] ?: "",
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
        return recordList.map {
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
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
                val buildName = getBuildName(interceptHistory.buildId)
                val time = interceptHistory.createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
                if (result == RuleInterceptResult.PASS) {
                    "$pipelineName(#$buildName)在${time}验证通过"
                } else {
                    "$pipelineName(#$buildName)在${time}验证被拦截"
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

    private fun getBuildIdToNameMap(buildIdSet: Set<String>): Map<String, String> {
        return client.get(ServicePipelineResource::class).getBuildNoByBuildIds(buildIdSet).data ?: mapOf()
    }

    private fun getBuildName(buildId: String): String {
        val map = getBuildIdToNameMap(setOf(buildId))
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
}
