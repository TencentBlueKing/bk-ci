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

package com.tencent.devops.metrics.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.metrics.pojo.`do`.ComplianceInfoDO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.MaxJobConcurrencyVO
import com.tencent.devops.metrics.pojo.vo.PipelineSumInfoVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import com.tencent.devops.metrics.pojo.vo.QueryIntervalVO
import com.tencent.devops.metrics.pojo.vo.ThirdPlatformOverviewInfoVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_METRICS", description = "METRICS")
@Path("/service/metrics/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMetricsResource {

    @Operation(summary = "查询流水线汇总信息")
    @Path("/summary_pipeline")
    @POST
    fun queryPipelineSumInfo(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "userId", required = true)
        @BkField(required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询条件", required = false)
        baseQueryReq: BaseQueryReqVO?
    ): Result<PipelineSumInfoVO>

    @Operation(summary = "获取第三方汇总信息")
    @Path("/summary_third_party")
    @GET
    fun queryPipelineSummaryInfo(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @Parameter(description = "开始时间", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @Parameter(description = "结束时间", required = false)
        @QueryParam("endTime")
        endTime: String?
    ): Result<ThirdPlatformOverviewInfoVO>

    @Operation(summary = "查询插件合规率信息")
    @Path("/compliance_atom")
    @POST
    fun queryAtomComplianceInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @Parameter(description = "插件code", required = true)
        @QueryParam("atomCode")
        atomCode: String,
        @Parameter(description = "查询区间视图", required = true)
        queryIntervalVO: QueryIntervalVO
    ): Result<ComplianceInfoDO?>

    @Operation(summary = "查询项目活跃用户数")
    @Path("/get_project_active_user_count")
    @POST
    fun getProjectActiveUserCount(
        @Parameter(description = "查询条件", required = false)
        baseQueryReq: BaseQueryReqVO
    ): Result<ProjectUserCountV0?>

    @Operation(summary = "获取job最大并发")
    @Path("/get_max_job_concurrency")
    @POST
    fun getMaxJobConcurrency(
        @Parameter(description = "查询条件", required = false)
        dispatchJobReq: BaseQueryReqVO
    ): Result<MaxJobConcurrencyVO?>
}
