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

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.template.ServiceTemplateInstanceResource
import com.tencent.devops.quality.api.v2.pojo.response.CountOverviewResponse
import com.tencent.devops.quality.dao.CountInterceptDao
import com.tencent.devops.quality.dao.CountPipelineDao
import com.tencent.devops.quality.dao.CountRuleDao
import com.tencent.devops.quality.pojo.CountDailyIntercept
import com.tencent.devops.quality.pojo.CountPipelineIntercept
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service@Suppress("ALL")
class QualityCountService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleService: QualityRuleService,
    private val indicatorService: QualityIndicatorService,
    private val countInterceptDao: CountInterceptDao,
    private val countPipelineDao: CountPipelineDao,
    private val countRuleDao: CountRuleDao,
    private val client: Client
) {
    fun getOverview(userId: String, projectId: String): CountOverviewResponse {
        val ruleCount = ruleService.count(projectId)
        val indicatorCount = indicatorService.userCount(projectId)
        val interceptCount = countInterceptDao.countRuleIntercept(dslContext, projectId)

        // 剔除已删除的流水线
        val pipelineSet = mutableSetOf<String>()
        val templateSet = mutableSetOf<String>()
        val ruleList = ruleService.serviceListRules(projectId)
        ruleList.forEach { pipelineSet.addAll(it.range) }
        ruleList.forEach { templateSet.addAll(it.templateRange) }

        val pipelineCount = getPipelineIdToNameMap(projectId, pipelineSet).size
        val templatePipelineCount = client.get(ServiceTemplateInstanceResource::class).countTemplateInstance(projectId, templateSet).data ?: 0
        return CountOverviewResponse(
            ruleCount = ruleCount.toInt(),
            indicatoCount = indicatorCount.toInt(),
            interceptCount = interceptCount.toInt(),
            pipelineCount = pipelineCount + templatePipelineCount
        )
    }

    fun getPipelineIntercept(userId: String, projectId: String): List<CountPipelineIntercept> {
        val result = countPipelineDao.listByInterceptCount(dslContext, projectId)
        val pipelineIdList = result.map { it.value1() }
        val pipelineIdToNameMap = getPipelineIdToNameMap(projectId, pipelineIdList.toSet())

        val pipelineInterceptList = mutableListOf<CountPipelineIntercept>()
        result.forEach {
            val pipelineId = it.value1()
            if (pipelineIdToNameMap.containsKey(pipelineId)) {
                val pipelineName = pipelineIdToNameMap[pipelineId]!!
                val count = it.value2().toInt()
                pipelineInterceptList.add(CountPipelineIntercept(pipelineId, pipelineName, count))
            }
            if (pipelineInterceptList.size == INTERCEPT_PIPELINE_LIST_SIZE) return pipelineInterceptList
        }
        for (count in 1..(INTERCEPT_PIPELINE_LIST_SIZE - pipelineInterceptList.size)) {
            pipelineInterceptList.add(CountPipelineIntercept("", "no pipeline", 0))
        }

        return pipelineInterceptList
    }

    fun getDailyIntercept(userId: String, projectId: String): List<CountDailyIntercept> {
        val end = LocalDate.now()
        val start = end.minusDays(6L)
        val result = countInterceptDao.list(dslContext, projectId, start, end)
        val map = mutableMapOf<LocalDate, Pair<Int, Int>>()
        result.forEach {
            map[it.date] = Pair(it.ruleInterceptCount, it.count)
        }

        val list = mutableListOf<CountDailyIntercept>()
        var date = start
        while (!date.isAfter(end)) {
            val countPair = if (map.containsKey(date)) {
                map[date]!!
            } else {
                Pair(0, 0)
            }
            list.add(CountDailyIntercept(
                date = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                count = countPair.first,
                pipelineExecuteCount = countPair.second
            ))

            date = date.plusDays(1)
        }

        return list
    }

    fun countIntercept(projectId: String, pipelineId: String, ruleId: Long, interceptResult: Boolean) {
        val createTime = LocalDateTime.now()
        dslContext.transaction { configuration ->
            val date = LocalDate.of(createTime.year, createTime.month, createTime.dayOfMonth)
            val transactionContext = DSL.using(configuration)

            // T_COUNT_INTERCEPT
            val countInterceptRecord = countInterceptDao.getOrNull(transactionContext, projectId, date)
            val interceptId = if (countInterceptRecord == null) {
                countInterceptDao.create(transactionContext, projectId, date, 1)
            } else {
                countInterceptDao.plusCount(transactionContext, countInterceptRecord.id)
                countInterceptRecord.id
            }

            if (!interceptResult) countInterceptDao.plusRuleInterceptCount(dslContext, interceptId)

            // T_COUNT_PIPELINE
            val countPipelineRecord = countPipelineDao.getOrNull(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                date = date
            )
            val pipelineRecordId = if (countPipelineRecord == null) {
                countPipelineDao.create(
                    dslContext = transactionContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    date = date,
                    count = 1,
                    lastInterceptTime = createTime
                )
            } else {
                countPipelineDao.plusCount(transactionContext, countPipelineRecord.id, createTime)
                countPipelineRecord.id
            }

            if (!interceptResult) countPipelineDao.plusInterceptCount(dslContext, pipelineRecordId)

            // T_COUNT_RULE
            val countRuleRecord = countRuleDao.getOrNull(transactionContext, projectId, ruleId, date)
            val countRuleId = if (countRuleRecord == null) {
                countRuleDao.create(transactionContext, projectId, ruleId, date, 1, createTime)
            } else {
                countRuleDao.plusCount(transactionContext, countRuleRecord.id)
                countRuleRecord.id
            }

            if (!interceptResult) countRuleDao.plusInterceptCount(dslContext, countRuleId)
        }
    }

    private fun getPipelineIdToNameMap(projectId: String, pipelineIdSet: Set<String>): Map<String, String> {
        return client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIdSet).data!!
    }

    companion object {
        private const val INTERCEPT_PIPELINE_LIST_SIZE = 5
    }
}
