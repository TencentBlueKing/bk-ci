package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.quality.tables.records.THistoryRecord
import com.tencent.devops.process.api.ServicePipelineResource
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.quality.api.v2.pojo.QualityRuleIntercept
import com.tencent.devops.quality.api.v2.pojo.QualityRuleInterceptRecord
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import com.tencent.devops.quality.util.ThresholdOperationUtil
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class QualityHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleService: QualityRuleService,
    private val historyDao: HistoryDao,
    private val client: Client,
    private val objectMapper: ObjectMapper
) {

    fun userGetRuleIntercept(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<QualityRuleIntercept>> {
        val recordList = historyDao.listIntercept(dslContext, projectId, null, null, null, null, offset, limit)
        val count = historyDao.countIntercept(dslContext, projectId, null, null, null, null)

        // 批量查询名称信息
        val pipelineIdNameMap = client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, recordList.map { it.pipelineId }.toSet()).data
                ?: mapOf()
        val ruleIdMap = ruleService.serviceListRuleByIds(projectId, recordList.map { it.ruleId }).map { it.hashId to it }.toMap()

        val list = recordList.map {
            val hashId = HashUtil.encodeLongId(it.ruleId)
            QualityRuleIntercept(
                    it.pipelineId,
                    pipelineIdNameMap[it.pipelineId] ?: "",
                    it.buildId,
                    hashId,
                    ruleIdMap[hashId]?.name ?: "",
                    it.createTime.timestampmilli(),
                    RuleInterceptResult.valueOf(it.result),
                    objectMapper.readValue(it.interceptList)
            )
        }
        return Pair(count, list)
    }

    fun userGetInterceptHistory(userId: String, projectId: String, ruleHashId: String, offset: Int, limit: Int): Pair<Long, List<RuleInterceptHistory>> {
        val record = ruleService.serviceGet(ruleHashId)
        val ruleId = HashUtil.decodeIdToLong(ruleHashId)
        val count = historyDao.count(dslContext, ruleId)
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
                sb.append("${intercept.indicatorName}当前值(${intercept.actualValue})，期望$thresholdOperationName${intercept.value}\n")
            }
            val remark = sb.toString()

            RuleInterceptHistory(
                    HashUtil.encodeLongId(it.id),
                    it.projectNum,
                    it.createTime.timestamp(),
                    result,
                    HashUtil.encodeLongId(ruleId),
                    record.name,
                    it.pipelineId,
                    pipelineIdToNameMap[it.pipelineId] ?: "",
                    it.buildId,
                    buildIdToNameMap[it.buildId] ?: "",
                    remark,
                    false
            )
        }
        return Pair(count, list)
    }

    fun serviceListByRuleId(projectId: String, ruleId: Long, offset: Int, limit: Int): Result<THistoryRecord> {
        return historyDao.listByRuleId(dslContext, projectId, ruleId, offset, limit)
    }

    fun serviceListByBuildIdAndResult(projectId: String, pipelineId: String, buildId: String, result: String): Result<THistoryRecord> {
        return historyDao.listByBuildIdAndResult(dslContext, projectId, pipelineId, buildId, result)
    }

    fun serviceListByBuildId(projectId: String, pipelineId: String, buildId: String): List<QualityRuleIntercept> {
        return historyDao.listByBuildId(dslContext, projectId, pipelineId, buildId).map {
            QualityRuleIntercept(
                    it.pipelineId,
                    "",
                    it.buildId,
                    "",
                    "",
                    it.createTime.timestampmilli(),
                    RuleInterceptResult.valueOf(it.result),
                    objectMapper.readValue(it.interceptList)
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
        return historyDao.count(dslContext, projectId, pipelineId, ruleId, result, startTime, endTime)
    }

    fun serviceList(
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        offset: Int,
        limit: Int
    ): Result<THistoryRecord> {
        return historyDao.list(dslContext, projectId, pipelineId, ruleId, result, startTime, endTime, offset, limit)
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
            time.plusDays(1).minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong()).minusSeconds(time.second.toLong())
        }

        val count = serviceCount(projectId, pipelineId, ruleId, ruleInterceptResult, startLocalDateTime, endLocalDateTime)
        val recordList = serviceList(projectId, pipelineId, ruleId, ruleInterceptResult, startLocalDateTime, endLocalDateTime, offset, limit)

        val ruleIdList = recordList.map { it.ruleId }
        val ruleIdToNameMap = ruleService.serviceListRuleByIds(projectId, ruleIdList.toSet()).map { it.hashId to it.name }.toMap()
        val pipelineIdList = recordList.map { it.pipelineId }
        val pipelineIdToNameMap = getPipelineByIds(projectId, pipelineIdList.toSet()).map { it.pipelineId to it }.toMap()
        val buildIdList = recordList.map { it.buildId }
        val buildIdToNameMap = getBuildIdToNameMap(buildIdList.toSet())

        val list = recordList.map {
            val sb = StringBuilder()
            val interceptList = objectMapper.readValue<List<QualityRuleInterceptRecord>>(it.interceptList)
            interceptList.forEach { intercept ->
                val thresholdOperationName = ThresholdOperationUtil.getOperationName(intercept.operation)
                sb.append("${intercept.indicatorName}当前值(${intercept.actualValue})，期望$thresholdOperationName${intercept.value}\n")
            }
            val remark = sb.toString()
            val hisRuleHashId = HashUtil.encodeLongId(it.ruleId)
            val pipeline = pipelineIdToNameMap[it.pipelineId]
            RuleInterceptHistory(
                    HashUtil.encodeLongId(it.id),
                    it.projectNum,
                    it.createTime.timestamp(),
                    RuleInterceptResult.valueOf(it.result),
                    hisRuleHashId,
                    ruleIdToNameMap[hisRuleHashId] ?: "",
                    it.pipelineId,
                    pipeline?.pipelineName ?: "",
                    it.buildId,
                    buildIdToNameMap[it.buildId] ?: "",
                    remark,
                    pipeline?.isDelete ?: false
            )
        }
        return Pair(count, list)
    }

    fun userGetInterceptRecent(projectId: String, ruleId: String): String? {
        // 查询拦截状态
        val historyList = serviceListByRuleId(projectId, HashUtil.decodeIdToLong(ruleId), 0, 1)
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

    fun serviceCreate(projectId: String, ruleId: Long, pipelineId: String, buildId: String, result: String, interceptList: String, time: LocalDateTime, time1: LocalDateTime) {
        historyDao.create(dslContext, projectId, ruleId, pipelineId, buildId, result, interceptList, time, time)
    }
}
