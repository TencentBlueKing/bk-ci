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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.constant.MetricsMessageCode
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.DATE_FORMATTER
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getBetweenDate
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getErrorTypeName
import com.tencent.devops.metrics.dao.PipelineFailDao
import com.tencent.devops.metrics.pojo.`do`.ErrorCodeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineBuildInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailDetailInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailStatisticsInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailDTO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailTrendInfoDTO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineFailQO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineOverviewQO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.PipelineFailTrendInfoVO
import com.tencent.devops.metrics.service.PipelineFailManageService
import com.tencent.devops.metrics.utils.MetricsUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PipelineFailServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineFailDao: PipelineFailDao,
    private val metricsConfig: MetricsConfig
) : PipelineFailManageService {

    override fun queryPipelineFailTrendInfo(
        queryPipelineFailTrendDTO: QueryPipelineFailTrendInfoDTO
    ): List<PipelineFailTrendInfoVO> {
        val startTime = queryPipelineFailTrendDTO.baseQueryReq.startTime
        val endTime = queryPipelineFailTrendDTO.baseQueryReq.endTime
        val typeInfos = pipelineFailDao.queryPipelineFailErrorTypeInfo(
            dslContext = dslContext,
            queryPipelineFailTrendQo = QueryPipelineOverviewQO(
                projectId = queryPipelineFailTrendDTO.projectId,
                baseQueryReq = queryPipelineFailTrendDTO.baseQueryReq
            )
        )
        return typeInfos.map { typeInfo ->
            val queryPipelineFailTrendInfoCount = pipelineFailDao.queryPipelineFailTrendInfoCount(
                dslContext = dslContext,
                queryPipelineFailTrendQo = QueryPipelineOverviewQO(
                    projectId = queryPipelineFailTrendDTO.projectId,
                    baseQueryReq = queryPipelineFailTrendDTO.baseQueryReq
                ),
                errorType = typeInfo
            )
            // 查询记录过多，提醒用户缩小查询范围
            if (queryPipelineFailTrendInfoCount > metricsConfig.queryCountMax) {
                throw ErrorCodeException(
                    errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                    params = arrayOf("${metricsConfig.queryCountMax}")
                )
            }
            val result = pipelineFailDao.queryPipelineFailTrendInfo(
                dslContext = dslContext,
                queryPipelineFailTrendQo = QueryPipelineOverviewQO(
                    projectId = queryPipelineFailTrendDTO.projectId,
                    baseQueryReq = queryPipelineFailTrendDTO.baseQueryReq
                ),
                errorType = typeInfo
            )
            val failStatisticsInfoMap = mutableMapOf<String, PipelineFailStatisticsInfoDO>()
                result.map { failStatisticsInfo ->
                    val statisticsTime = failStatisticsInfo[BK_STATISTICS_TIME] as LocalDateTime
                    failStatisticsInfoMap[statisticsTime.format(DATE_FORMATTER)] =
                        PipelineFailStatisticsInfoDO(
                            statisticsTime = statisticsTime.toLocalDate(),
                            errorCount = (failStatisticsInfo[BK_ERROR_COUNT_SUM] as BigDecimal).toInt()
                        )
            }
            //  当时间区间内存在某一天有某种错误类型数据不存在，返回数据设为0
            val betweenDates = if (startTime.equals(endTime)) listOf(startTime)
                else getBetweenDate(startTime!!, endTime!!)
            val failStatisticsInfos = betweenDates.map {
                if (failStatisticsInfoMap.containsKey(it)) {
                    failStatisticsInfoMap[it]!!
                } else {
                    PipelineFailStatisticsInfoDO(
                        statisticsTime = DateTimeUtil.stringToLocalDate(it)!!,
                        errorCount = 0
                    )
                }
            }
            PipelineFailTrendInfoVO(
                errorType = typeInfo,
                name = getErrorTypeName(typeInfo),
                failInfos = failStatisticsInfos
            )
        }
    }

    override fun queryPipelineFailSumInfo(queryPipelineFailDTO: QueryPipelineFailDTO): List<PipelineFailInfoDO> {
        val result = pipelineFailDao.queryPipelineFailSumInfo(
            dslContext,
            QueryPipelineFailQO(
                projectId = queryPipelineFailDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryPipelineFailDTO.pipelineIds,
                    pipelineLabelIds = queryPipelineFailDTO.pipelineLabelIds,
                    startTime = queryPipelineFailDTO.startTime,
                    endTime = queryPipelineFailDTO.endTime
                ),
                errorTypes = queryPipelineFailDTO.errorTypes
            )
        )
        return result.map {
            val errorType = it[BK_ERROR_TYPE] as Int
            PipelineFailInfoDO(
                errorType = errorType,
                name = getErrorTypeName(errorType),
                errorCount = (it[BK_ERROR_COUNT_SUM] as BigDecimal).toLong()
            )
        }
    }

    override fun queryPipelineFailDetailInfo(
        queryPipelineFailDTO: QueryPipelineFailDTO
    ): Page<PipelineFailDetailInfoDO> {
        // 查询符合查询条件的记录数
        val queryPipelineFailDetailCount =
            pipelineFailDao.queryPipelineFailDetailCount(
            dslContext = dslContext,
            queryPipelineFailQo = QueryPipelineFailQO(
                projectId = queryPipelineFailDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryPipelineFailDTO.pipelineIds,
                    pipelineLabelIds = queryPipelineFailDTO.pipelineLabelIds,
                    startTime = queryPipelineFailDTO.startTime,
                    endTime = queryPipelineFailDTO.endTime
                ),
                errorTypes = queryPipelineFailDTO.errorTypes
            )
        )
        // 查询记录过多，提醒用户缩小查询范围
        if (queryPipelineFailDetailCount > metricsConfig.queryCountMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND,
                params = arrayOf("${metricsConfig.queryCountMax}")
            )
        }
        val result = pipelineFailDao.queryPipelineFailDetailInfo(
            dslContext = dslContext,
            queryPipelineFailQo = QueryPipelineFailQO(
                projectId = queryPipelineFailDTO.projectId,
                baseQueryReq = BaseQueryReqVO(
                    pipelineIds = queryPipelineFailDTO.pipelineIds,
                    pipelineLabelIds = queryPipelineFailDTO.pipelineLabelIds,
                    startTime = queryPipelineFailDTO.startTime,
                    endTime = queryPipelineFailDTO.endTime
                ),
                errorTypes = queryPipelineFailDTO.errorTypes,
                    page = queryPipelineFailDTO.page,
                    pageSize = queryPipelineFailDTO.pageSize
            )
        )
        val detailInfos = if (result.isNotEmpty()) {
            result.map {
                val channelCode = it.channelCode
                // 根据渠道信息获取域名信息
                val domain = MetricsUtils.getDomain(channelCode)
                PipelineFailDetailInfoDO(
                    pipelineBuildInfo = PipelineBuildInfoDO(
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        pipelineName = it.pipelineName,
                        channelCode = channelCode,
                        domain = domain,
                        buildId = it.buildId,
                        buildNum = it.buildNum,
                        branch = it.branch
                    ),
                    startUser = it.startUser,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    errorInfo =
                    ErrorCodeInfoDO(
                        errorType = it.errorType,
                        errorTypeName = getErrorTypeName(it.errorType!!),
                        errorCode = it.errorCode!!,
                        errorMsg = it.errorMsg
                    )
                )
            }
        } else emptyList()

        return Page(
            page = queryPipelineFailDTO.page,
            pageSize = queryPipelineFailDTO.pageSize,
            count = queryPipelineFailDetailCount,
            records = detailInfos
        )
    }
}
