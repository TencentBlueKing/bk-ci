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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.BK_SUCCESS_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_COST_TIME_SUM
import com.tencent.devops.metrics.constant.Constants.BK_TOTAL_EXECUTE_COUNT_SUM
import com.tencent.devops.metrics.constant.MetricsMessageCode
import com.tencent.devops.metrics.dao.PipelineOverviewDao
import com.tencent.devops.metrics.pojo.`do`.PipelineBuildTimeStatisticsDO
import com.tencent.devops.metrics.pojo.`do`.PipelineSumInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineTrendInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineOverviewDTO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineOverviewQO
import com.tencent.devops.metrics.service.PipelineOverviewManageService
import com.tencent.devops.metrics.utils.QueryParamCheckUtil
import com.tencent.devops.model.metrics.tables.TPipelineOverviewData
import java.math.BigDecimal
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineOverviewServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineOverviewDao: PipelineOverviewDao,
    private val metricsConfig: MetricsConfig
) : PipelineOverviewManageService {
    override fun queryPipelineSumInfo(queryPipelineOverviewDTO: QueryPipelineOverviewDTO): PipelineSumInfoDO? {
        val queryPipelineSumInfoCount = pipelineOverviewDao.queryPipelineSumInfoCount(
            dslContext,
            QueryPipelineOverviewQO(
                queryPipelineOverviewDTO.projectId,
                queryPipelineOverviewDTO.baseQueryReq
            )
        )
        // 查询记录过多，提醒用户缩小查询范围
        if (queryPipelineSumInfoCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        val result = pipelineOverviewDao.queryPipelineSumInfo(
            dslContext,
            QueryPipelineOverviewQO(
                queryPipelineOverviewDTO.projectId,
                queryPipelineOverviewDTO.baseQueryReq
            )
        )
        val totalExecuteCountSum = result?.get(BK_TOTAL_EXECUTE_COUNT_SUM, BigDecimal::class.java)?.toLong()
        val successExecuteCountSum = result?.get(BK_SUCCESS_EXECUTE_COUNT_SUM, BigDecimal::class.java)?.toLong()
        val totalCostTimeSum = result?.get(BK_TOTAL_COST_TIME_SUM, BigDecimal::class.java)?.toLong()
        if (totalExecuteCountSum != null && totalCostTimeSum != null) {
            return PipelineSumInfoDO(
                totalSuccessRate = if (successExecuteCountSum == null || successExecuteCountSum == 0L) 0.0
                else String.format("%.2f", successExecuteCountSum.toDouble() / totalExecuteCountSum * 100).toDouble(),
                totalAvgCostTime = String.format(
                    "%.2f",
                    totalCostTimeSum.toDouble() / totalExecuteCountSum
                ).toDouble(),
                successExecuteCount = successExecuteCountSum ?: 0,
                totalExecuteCount = totalExecuteCountSum,
                totalCostTime = totalCostTimeSum
            )
        }
        return null
    }

    override fun queryPipelineTrendInfo(queryPipelineOverviewDTO: QueryPipelineOverviewDTO): List<PipelineTrendInfoDO> {
        val baseQueryReq = queryPipelineOverviewDTO.baseQueryReq
        val projectId = queryPipelineOverviewDTO.projectId
        val pageSize = PageUtil.MAX_PAGE_SIZE
        var pageNum = 1
        val pipelineBuildTimeStatisticsMap = mutableMapOf<String, PipelineBuildTimeStatisticsDO>()
        val t = TPipelineOverviewData.T_PIPELINE_OVERVIEW_DATA
        do {
            val result = pipelineOverviewDao.queryPipelineTrendInfo(
                dslContext,
                QueryPipelineOverviewQO(projectId, baseQueryReq, pageNum, pageSize)
            )

            for (trendInfo in result) {
                val statisticsTime = trendInfo.get(t.STATISTICS_TIME)
                val totalExecuteCount = trendInfo.get(t.TOTAL_EXECUTE_COUNT)
                val failExecuteCount = trendInfo.get(t.FAIL_EXECUTE_COUNT)
                val totalAvgCostTime = trendInfo.get(t.TOTAL_AVG_COST_TIME)
                val failAvgCostTime = trendInfo.get(t.FAIL_AVG_COST_TIME)
                // 计算流水线总耗时和失败耗时
                val totalTime = totalExecuteCount * totalAvgCostTime
                val failTotalTime = failAvgCostTime?.let { failExecuteCount * failAvgCostTime } ?: 0L
                val pipelineBuildTimeStatisticsDo = pipelineBuildTimeStatisticsMap.getOrDefault(
                    "$statisticsTime",
                    PipelineBuildTimeStatisticsDO(
                        statisticsTime = statisticsTime,
                        totalCostTime = 0L,
                        failCostTime = 0L,
                        totalExecuteCount = 0L,
                        failedExecuteCount = 0L
                    )
                )
                pipelineBuildTimeStatisticsDo.totalExecuteCount += totalExecuteCount
                pipelineBuildTimeStatisticsDo.totalCostTime += totalTime
                pipelineBuildTimeStatisticsDo.failedExecuteCount += failExecuteCount
                pipelineBuildTimeStatisticsDo.failCostTime += failTotalTime
                if (!pipelineBuildTimeStatisticsMap.containsKey("$statisticsTime")) {
                    pipelineBuildTimeStatisticsMap["$statisticsTime"] = pipelineBuildTimeStatisticsDo
                }
            }
            pageNum++
        } while (result.size == pageSize)
        return pipelineBuildTimeStatisticsMap.map {
            val buildTimeStatisticsDo = it.value
            val totalAvgCostTime = buildTimeStatisticsDo.totalCostTime / buildTimeStatisticsDo.totalExecuteCount
            val failAvgCostTime = if (buildTimeStatisticsDo.failedExecuteCount == 0L) {
                0L
            } else {
                buildTimeStatisticsDo.failCostTime / buildTimeStatisticsDo.failedExecuteCount
            }
            PipelineTrendInfoDO(
                statisticsTime = buildTimeStatisticsDo.statisticsTime,
                totalExecuteCount = buildTimeStatisticsDo.totalExecuteCount,
                failedExecuteCount = buildTimeStatisticsDo.failedExecuteCount,
                totalAvgCostTime = QueryParamCheckUtil.toMinutes(totalAvgCostTime),
                failAvgCostTime = QueryParamCheckUtil.toMinutes(failAvgCostTime)
            )
        }
    }
}
