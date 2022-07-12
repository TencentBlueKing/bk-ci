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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.metrics.constant.Constants.BK_AVG_COST_TIME
import com.tencent.devops.metrics.constant.Constants.BK_PIPELINE_NAME
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.utils.QueryParamCheckUtil
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.toMinutes
import com.tencent.devops.metrics.dao.PipelineStageDao
import com.tencent.devops.metrics.pojo.`do`.PipelineStageCostTimeInfoDO
import com.tencent.devops.metrics.pojo.`do`.StageAvgCostTimeInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineOverviewDTO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineStageTrendInfoQO
import com.tencent.devops.metrics.pojo.vo.StageTrendSumInfoVO
import com.tencent.devops.metrics.service.PipelineStageManageService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class PipelineStageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineStageDao: PipelineStageDao
) : PipelineStageManageService {

    override fun queryPipelineStageTrendInfo(
        queryPipelineOverviewDTO: QueryPipelineOverviewDTO
    ): List<StageTrendSumInfoVO> {
        var stageTrendSumInfos: MutableMap<String, MutableMap<String, StageAvgCostTimeInfoDO>>
        val tags = pipelineStageDao.getStageTag(dslContext, queryPipelineOverviewDTO.projectId)
        val baseQueryReq = queryPipelineOverviewDTO.baseQueryReq
        val startTime = baseQueryReq.startTime
        val endTime = baseQueryReq.endTime
        val betweenDate = QueryParamCheckUtil.getBetweenDate(startTime!!, endTime!!).toMutableList()
        var pipelineNames: MutableSet<String>
        return tags.map { tag -> // 根据stage标签分组获取数据
            val pipelineIds =
                if (baseQueryReq.pipelineIds.isNullOrEmpty() && baseQueryReq.pipelineLabelIds.isNullOrEmpty()) {
                    getStagePipelineIdByProject(
                        projectId = queryPipelineOverviewDTO.projectId,
                        startTime = startTime,
                        endTime = endTime,
                        tag = tag
                    )
            } else baseQueryReq.pipelineIds
            stageTrendSumInfos = mutableMapOf()
            pipelineNames = mutableSetOf()
            val result = pipelineStageDao.queryPipelineStageTrendInfo(
                dslContext,
                QueryPipelineStageTrendInfoQO(
                    projectId = queryPipelineOverviewDTO.projectId,
                    pipelineIds = pipelineIds,
                    pipelineLabelIds = queryPipelineOverviewDTO.baseQueryReq.pipelineLabelIds,
                    startTime = startTime,
                    endTime = endTime,
                    stageTag = tag
                )
            )
            //  将查询结果根据流水线分组封装
            result.map {
                val pipelineName = it[BK_PIPELINE_NAME] as String
                val avgCostTime = toMinutes((it[BK_AVG_COST_TIME] as Long))
                val statisticsTime = (it[BK_STATISTICS_TIME] as LocalDateTime).toLocalDate()
                if (!stageTrendSumInfos.containsKey(it[BK_PIPELINE_NAME] as String)) {
                    val mutableMapOf =
                        mutableMapOf("$statisticsTime" to StageAvgCostTimeInfoDO(statisticsTime, avgCostTime))
                    stageTrendSumInfos[pipelineName] = mutableMapOf
                } else {
                    val mutableMap = stageTrendSumInfos[pipelineName]!!
                    mutableMap["$statisticsTime"] = StageAvgCostTimeInfoDO(statisticsTime, avgCostTime)
                }
                pipelineNames.add(pipelineName)
            }
            val pipelineStageCostTimeInfoDOs = mutableListOf<PipelineStageCostTimeInfoDO>()
            //  对每组流水线数据中无数据的日期添加占位数据
            pipelineNames.forEach { pipelineName ->
                val stageAvgCostTimeInfos = stageTrendSumInfos[pipelineName]!!
                val pipelineStageInfos = mutableListOf<StageAvgCostTimeInfoDO>()
                betweenDate.forEach { date ->
                    if (stageAvgCostTimeInfos.containsKey(date)) {
                        pipelineStageInfos.add(stageAvgCostTimeInfos[date]!!)
                    } else {
                        pipelineStageInfos.add(
                            StageAvgCostTimeInfoDO(DateTimeUtil.stringToLocalDate(date)!!, 0.0)
                        )
                    }
                }
                pipelineStageCostTimeInfoDOs.add(PipelineStageCostTimeInfoDO(pipelineName, pipelineStageInfos))
            }
            StageTrendSumInfoVO(tag, pipelineStageCostTimeInfoDOs)
        }
    }

    private val stageDefaultPipelineIdsCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, List<String>>()

    fun getStagePipelineIdByProject(projectId: String, startTime: String, endTime: String, tag: String): List<String> {
        val value =
            stageDefaultPipelineIdsCache.getIfPresent("MetricsOverviewStageDefaultPipelineIds:$projectId:$tag")
        return if (value.isNullOrEmpty()) {
            val pipelineIds = pipelineStageDao.getStagePipelineIdByProject(
                dslContext = dslContext,
                projectId = projectId,
                startTime = startTime,
                endTime = endTime,
                tag = tag
            )
            stageDefaultPipelineIdsCache.put("MetricsOverviewStageDefaultPipelineIds:$projectId:$tag", pipelineIds)
            pipelineIds
        } else {
            value
        }
    }
}
