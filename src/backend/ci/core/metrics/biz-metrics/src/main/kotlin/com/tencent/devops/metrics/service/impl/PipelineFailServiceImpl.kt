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
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_COUNT_SUM
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_NAME
import com.tencent.devops.metrics.constant.Constants.BK_ERROR_TYPE
import com.tencent.devops.metrics.constant.Constants.BK_QUERY_COUNT_MAX
import com.tencent.devops.metrics.constant.Constants.BK_STATISTICS_TIME
import com.tencent.devops.metrics.constant.MetricsMessageCode
import com.tencent.devops.metrics.constant.QueryParamCheckUtil.DATE_FORMATTER
import com.tencent.devops.metrics.constant.QueryParamCheckUtil.getBetweenDate
import com.tencent.devops.metrics.dao.PipelineFailDao
import com.tencent.devops.metrics.pojo.`do`.BaseQueryReqDO
import com.tencent.devops.metrics.pojo.`do`.ErrorCodeInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineBuildInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailDetailInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailStatisticsInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailDTO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailTrendInfoDTO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineFailQO
import com.tencent.devops.metrics.pojo.qo.QueryPipelineOverviewQO
import com.tencent.devops.metrics.pojo.vo.PipelineFailTrendInfoVO
import com.tencent.devops.metrics.service.PipelineFailManageService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Stream


@Service
class PipelineFailServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineFailDao: PipelineFailDao
): PipelineFailManageService {

    override fun queryPipelineFailTrendInfo(
        queryPipelineFailTrendDTO: QueryPipelineFailTrendInfoDTO
    ): List<PipelineFailTrendInfoVO> {
        logger.info("queryPipelineFailTrendInfoDTO: $queryPipelineFailTrendDTO")
        val startTime = queryPipelineFailTrendDTO.baseQueryReq.startTime
        val endTime = queryPipelineFailTrendDTO.baseQueryReq.endTime
        val typeInfos = pipelineFailDao.queryPipelineFailErrorTypeInfo(
            dslContext,
            QueryPipelineOverviewQO(
                projectId = queryPipelineFailTrendDTO.projectId,
                baseQueryReq = queryPipelineFailTrendDTO.baseQueryReq
            )
        )
        logger.info("queryPipelineFailTrendInfo:$typeInfos")
        val failTrendInfos = typeInfos.map { failTrendInfo ->
            val result = pipelineFailDao.queryPipelineFailTrendInfo(
                dslContext,
                QueryPipelineOverviewQO(
                    projectId = queryPipelineFailTrendDTO.projectId,
                    baseQueryReq = queryPipelineFailTrendDTO.baseQueryReq
                ),
                failTrendInfo.value1()
            )
            logger.info("queryPipelineFailTrendInfo:$result")
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
            val betweenDates = if(startTime.equals(endTime)) listOf(startTime)
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
            logger.info("queryPipelineFailTrendInfo failStatisticsInfos:$failStatisticsInfos")
            PipelineFailTrendInfoVO(
                errorType = failTrendInfo.value1(),
                name = failTrendInfo.value2()?: "",
                failInfos = failStatisticsInfos
            )
        }
        return failTrendInfos
    }

    override fun queryPipelineFailSumInfo(queryPipelineFailDTO: QueryPipelineFailDTO): List<PipelineFailInfoDO> {
        logger.info("queryPipelineFailTrendInfoDTO: $queryPipelineFailDTO")
        val result = pipelineFailDao.queryPipelineFailSumInfo(
            dslContext,
            QueryPipelineFailQO(
                queryPipelineFailDTO.projectId,
                BaseQueryReqDO(
                    queryPipelineFailDTO.pipelineIds,
                    queryPipelineFailDTO.pipelineLabelIds,
                    queryPipelineFailDTO.startTime,
                    queryPipelineFailDTO.endTime
                ),
                queryPipelineFailDTO.errorTypes
            )
        )
        logger.info("queryPipelineFailSumInfo: $result")
        return result.map {
            PipelineFailInfoDO(
                errorType = it[BK_ERROR_TYPE] as Int,
                name = it[BK_ERROR_NAME]?.toString(),
                errorCount = (it[BK_ERROR_COUNT_SUM] as BigDecimal).toInt()
            )
        }

    }

    override fun queryPipelineFailDetailInfo(queryPipelineFailDTO: QueryPipelineFailDTO): Page<PipelineFailDetailInfoDO> {
        logger.info("queryPipelineFailTrendInfoDTO: $queryPipelineFailDTO")
        // 查询符合查询条件的记录数
        val queryPipelineFailDetailCount = pipelineFailDao.queryPipelineFailDetailCount(
            dslContext,
            QueryPipelineFailQO(
                queryPipelineFailDTO.projectId,
                BaseQueryReqDO(
                    queryPipelineFailDTO.pipelineIds,
                    queryPipelineFailDTO.pipelineLabelIds,
                    queryPipelineFailDTO.startTime,
                    queryPipelineFailDTO.endTime
                ),
                queryPipelineFailDTO.errorTypes
            )
        )
        if (queryPipelineFailDetailCount > BK_QUERY_COUNT_MAX) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DETAILS_COUNT_BEYOND
            )
        }
        val result = pipelineFailDao.queryPipelineFailDetailInfo(
            dslContext,
            QueryPipelineFailQO(
                queryPipelineFailDTO.projectId,
                BaseQueryReqDO(
                    queryPipelineFailDTO.pipelineIds,
                    queryPipelineFailDTO.pipelineLabelIds,
                    queryPipelineFailDTO.startTime,
                    queryPipelineFailDTO.endTime
                ),
                queryPipelineFailDTO.errorTypes,
                    queryPipelineFailDTO.page,
                    queryPipelineFailDTO.pageSize
            )
        ).map {
            PipelineFailDetailInfoDO(
                PipelineBuildInfoDO(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    buildId = it.buildId,
                    buildNum = it.buildNum,
                    branch = it.branch
                ),
                startUser = it.startUser,
                startTime = it.startTime,
                endTime = it.endTime,
                errorInfo = ErrorCodeInfoDO(
                    errorType = it.errorType,
                    errorTypeName = it.errorTypeName,
                    errorCode = it.errorCode,
                    errorMsg = it.errorMsg
                )
            )
        }
        return Page(
            queryPipelineFailDTO.page,
            queryPipelineFailDTO.pageSize,
            count = queryPipelineFailDetailCount,
            records = result
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineFailServiceImpl::class.java)
    }
}