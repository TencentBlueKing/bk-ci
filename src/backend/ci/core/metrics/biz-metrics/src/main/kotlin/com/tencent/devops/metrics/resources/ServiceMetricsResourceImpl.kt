/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.metrics.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.metrics.pojo.`do`.ComplianceInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineOverviewDTO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineSummaryInfoDTO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.MaxJobConcurrencyVO
import com.tencent.devops.metrics.pojo.vo.PipelineSumInfoVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO
import com.tencent.devops.metrics.pojo.vo.ThirdPlatformOverviewInfoVO
import com.tencent.devops.metrics.service.AtomStatisticsManageService
import com.tencent.devops.metrics.service.DispatchJobMetricsService
import com.tencent.devops.metrics.service.PipelineOverviewManageService
import com.tencent.devops.metrics.service.ProjectBuildSummaryService
import com.tencent.devops.metrics.service.ThirdPartyManageService
import com.tencent.devops.metrics.utils.QueryParamCheckUtil

@RestResource
class ServiceMetricsResourceImpl constructor(
    private val thirdPartyManageService: ThirdPartyManageService,
    private val pipelineOverviewManageService: PipelineOverviewManageService,
    private val atomStatisticsManageService: AtomStatisticsManageService,
    private val projectBuildSummaryService: ProjectBuildSummaryService,
    private val dispatchJobMetricsService: DispatchJobMetricsService
) : ServiceMetricsResource {

    @BkApiPermission([BkApiHandleType.PROJECT_MEMBER_CHECK])
    override fun queryPipelineSumInfo(
        projectId: String,
        userId: String,
        baseQueryReq: BaseQueryReqVO?
    ): Result<PipelineSumInfoVO> {
        val queryReq = baseQueryReq ?: BaseQueryReqVO()
        if (queryReq.startTime.isNullOrBlank()) {
            queryReq.startTime = QueryParamCheckUtil.getStartDateTime()
        }
        if (queryReq.endTime.isNullOrBlank()) {
            queryReq.endTime = QueryParamCheckUtil.getEndDateTime()
        }
        QueryParamCheckUtil.checkDateInterval(queryReq.startTime!!, queryReq.endTime!!)
        return Result(
            PipelineSumInfoVO(
                pipelineOverviewManageService.queryPipelineSumInfo(
                    QueryPipelineOverviewDTO(
                        projectId = projectId,
                        userId = userId,
                        baseQueryReq = queryReq
                    )
                )
            )
        )
    }

    @BkApiPermission([BkApiHandleType.PROJECT_MEMBER_CHECK])
    override fun queryPipelineSummaryInfo(
        projectId: String,
        userId: String,
        startTime: String?,
        endTime: String?
    ): Result<ThirdPlatformOverviewInfoVO> {
        val startDateTime = if (!startTime.isNullOrBlank()) startTime else QueryParamCheckUtil.getStartDateTime()
        val endDateTime = if (!endTime.isNullOrBlank()) endTime else QueryParamCheckUtil.getEndDateTime()
        QueryParamCheckUtil.checkDateInterval(startDateTime, endDateTime)
        return Result(
            thirdPartyManageService.queryPipelineSummaryInfo(
                QueryPipelineSummaryInfoDTO(
                    projectId = projectId,
                    userId = userId,
                    startTime = startDateTime,
                    endTime = endDateTime
                )
            )
        )
    }

    override fun queryAtomComplianceInfo(
        userId: String,
        atomCode: String,
        queryIntervalVO: QueryIntervalVO
    ): Result<ComplianceInfoDO?> {
        return Result(
            atomStatisticsManageService.queryAtomComplianceInfo(
                userId = userId,
                atomCode = atomCode,
                queryIntervalVO = queryIntervalVO
            )
        )
    }

    override fun getProjectActiveUserCount(
        baseQueryReq: BaseQueryReqVO
    ): Result<ProjectUserCountV0?> {
        return Result(
            projectBuildSummaryService.getProjectActiveUserCount(
                baseQueryReq = baseQueryReq
            )
        )
    }

    override fun getMaxJobConcurrency(dispatchJobReq: BaseQueryReqVO): Result<MaxJobConcurrencyVO?> {
        return Result(
            dispatchJobMetricsService.getMaxJobConcurrency(
                dispatchJobReq = dispatchJobReq
            )
        )
    }
}
