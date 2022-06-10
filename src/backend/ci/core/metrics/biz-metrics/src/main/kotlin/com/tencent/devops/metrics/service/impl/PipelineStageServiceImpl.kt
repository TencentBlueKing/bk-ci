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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME
import com.tencent.devops.metrics.constant.Constants.BK_PIPELINE_NAME
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.constant.QueryParamCheckUtil
import com.tencent.devops.metrics.constant.QueryParamCheckUtil.DATE_FORMATTER
import com.tencent.devops.metrics.constant.QueryParamCheckUtil.toMinutes
import com.tencent.devops.metrics.dao.PipelineStageDao
import com.tencent.devops.metrics.service.PipelineStageManageService
import com.tencent.devops.metrics.pojo.`do`.PipelineStageCostTimeInfoDO
import com.tencent.devops.metrics.pojo.`do`.StageAvgCostTimeInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineOverviewDTO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineStageTrendInfoQO
import com.tencent.devops.metrics.pojo.vo.StageTrendSumInfoVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class PipelineStageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineStageDao: PipelineStageDao
): PipelineStageManageService {

    override fun queryPipelineStageTrendInfo(
        queryPipelineOverviewDTO: QueryPipelineOverviewDTO
    ): List<StageTrendSumInfoVO> {

        var stageTrendSumInfos : MutableMap<String, List<StageAvgCostTimeInfoDO>>
        val tags = pipelineStageDao.getStageTag(dslContext, queryPipelineOverviewDTO.projectId)
        val startTime = queryPipelineOverviewDTO.baseQueryReq.startTime
        val endTime = queryPipelineOverviewDTO.baseQueryReq.endTime
        val betweenDate = QueryParamCheckUtil.getBetweenDate(startTime!!, endTime!!).toMutableList()
        var pipelineNames: MutableSet<String>
        return tags.map { tag ->
            stageTrendSumInfos = mutableMapOf<String, List<StageAvgCostTimeInfoDO>>()
            pipelineNames = mutableSetOf()
            val result = pipelineStageDao.queryPipelineStageTrendInfo(
                dslContext,
                QueryPipelineStageTrendInfoQO(
                    queryPipelineOverviewDTO.projectId,
                    queryPipelineOverviewDTO.baseQueryReq,
                    tag
                )
            )
            logger.info("PipelineStageServiceImpl tag:$tag result: $result ")
            result.map {
                val pipelineName = it[BK_PIPELINE_NAME] as String
                val avgCostTime = toMinutes((it[BK_AVG_COST_TIME] as Long))
                val statisticsTime = (it[BK_STATISTICS_TIME] as LocalDateTime).toLocalDate()
                if (!stageTrendSumInfos.containsKey(it[BK_PIPELINE_NAME] as String)) {
                    val listOf = mutableListOf(
                        StageAvgCostTimeInfoDO(statisticsTime, avgCostTime)
                    )
                    stageTrendSumInfos[pipelineName] = listOf
                } else {
                    val listOf = stageTrendSumInfos[pipelineName]!!.toMutableList()
                    listOf.add(
                        StageAvgCostTimeInfoDO(statisticsTime, avgCostTime)
                    )
                    stageTrendSumInfos[pipelineName] = listOf
                }
                pipelineNames.add(pipelineName)
                betweenDate.removeIf{s -> s == statisticsTime.format(DATE_FORMATTER) }
            }
            logger.info("PipelineStageServiceImpl  stageTrendSumInfos:$stageTrendSumInfos")
            pipelineNames.forEach { pipelineName ->
                val stageAvgCostTimeInfos = stageTrendSumInfos[pipelineName]!!.toMutableList()
                betweenDate.forEach { date ->
                    stageAvgCostTimeInfos.add(StageAvgCostTimeInfoDO(DateTimeUtil.stringToLocalDate(date)!!, 0.0))
                }
                stageTrendSumInfos[pipelineName] =
                    stageAvgCostTimeInfos.stream().sorted(
                        Comparator.comparing(StageAvgCostTimeInfoDO::statisticsTime)
                    ).collect(Collectors.toList())
            }
            val pipelineStageCostTimeInfoDOs = stageTrendSumInfos.map {
                PipelineStageCostTimeInfoDO(it.key, it.value)
            }
            StageTrendSumInfoVO(tag, pipelineStageCostTimeInfoDOs)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineStageServiceImpl::class.java)
    }
}