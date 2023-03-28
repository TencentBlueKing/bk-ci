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

package com.tencent.devops.metrics.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.metrics.pojo.`do`.ComplianceInfoDO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.PipelineSumInfoVO
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO
import com.tencent.devops.metrics.pojo.vo.ThirdPlatformOverviewInfoVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_METRICS"], description = "METRICS")
@Path("/service/metrics/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMetricsResource {

    @ApiOperation("查询流水线汇总信息")
    @Path("/summary_pipeline")
    @POST
    fun queryPipelineSumInfo(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @ApiParam("userId", required = true)
        @BkField(required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("查询条件", required = false)
        baseQueryReq: BaseQueryReqVO?
    ): Result<PipelineSumInfoVO>

    @ApiOperation("获取第三方汇总信息")
    @Path("/summary_third_party")
    @GET
    fun queryPipelineSummaryInfo(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @ApiParam("开始时间", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @ApiParam("结束时间", required = false)
        @QueryParam("endTime")
        endTime: String?
    ): Result<ThirdPlatformOverviewInfoVO>

    @ApiOperation("查询插件合规率信息")
    @Path("/compliance_atom")
    @POST
    fun queryAtomComplianceInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @ApiParam("插件code", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @ApiParam("查询区间视图", required = true)
        queryIntervalVO: QueryIntervalVO
    ): Result<ComplianceInfoDO?>
}
