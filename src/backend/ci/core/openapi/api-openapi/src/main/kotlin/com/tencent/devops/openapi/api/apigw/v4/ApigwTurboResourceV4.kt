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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.vo.ProjectResourceUsageVO
import com.tencent.devops.turbo.vo.ResourceCostSummary
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_TURBO_V4", description = "编译加速open api接口")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/turbo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwTurboResourceV4 {

    @GET
    @Operation(summary = "新版编译加速获取加速方案列表", tags = ["v4_app_turbo_new_plan_list", "v4_user_turbo_new_plan_list"])
    @Path("/projectId/{projectId}/turbo_plan_list")
    fun getTurboPlanByProjectIdAndCreatedDate(
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "开始日期", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @Parameter(description = "结束日期", required = false)
        @QueryParam("endTime")
        endTime: String?,
        @Parameter(description = "页数", required = false)
        @QueryParam(value = "pageNum")
        pageNum: Int?,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<Page<TurboPlanStatRowVO>>

    @POST
    @Operation(summary = "新版编译加速获取加速历史列表", tags = ["v4_app_turbo_new_record_list", "v4_user_turbo_new_record_list"])
    @Path("/projectId/{projectId}/history_list")
    fun getTurboRecordHistoryList(
        @Parameter(description = "页数", required = false)
        @QueryParam(value = "pageNum")
        pageNum: Int?,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "排序字段", required = false)
        @QueryParam("sortField")
        sortField: String?,
        @Parameter(description = "排序类型", required = false)
        @QueryParam("sortType")
        sortType: String?,
        @Parameter(description = "编译加速历史请求数据信息", required = true)
        turboRecordModel: TurboRecordModel,
        @Parameter(description = "蓝盾项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<Page<TurboRecordHistoryVO>>

    @GET
    @Operation(summary = "新版编译加速获取方案详情", tags = ["v4_app_turbo_new_plan", "v4_user_turbo_new_plan"])
    @Path("/projectId/{projectId}/turbo_plan_detail")
    fun getTurboPlanDetailByPlanId(
        @Parameter(description = "方案id", required = true)
        @QueryParam("planId")
        planId: String,
        @Parameter(description = "蓝盾项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Response<TurboPlanDetailVO>

    @POST
    @Operation(summary = "新增加速方案", tags = ["v4_app_turbo_plan_add_new", "v4_user_turbo_plan_add_new"])
    @Path("/projectId/{projectId}/addTurboPlan")
    fun addNewTurboPlan(
        @Parameter(description = "蓝盾项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户信息", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "新增加速方案请求数据信息", required = true)
        @Valid
        turboPlanModel: TurboPlanModel
    ): Response<String?>

    @GET
    @Operation(summary = "DevCloud专用资源统计查询接口", tags = ["v4_app_server_resources", "v4_user_server_resources"])
    @Path("/server_resources")
    fun getServerResourcesSummary(
        @Parameter(description = "起始统计日期")
        @QueryParam(value = "startDate")
        startDate: String?,
        @Parameter(description = "截止统计日期")
        @QueryParam("endDate")
        endDate: String?,
        @Parameter(description = "页数")
        @QueryParam(value = "pageNum")
        pageNum: Int?,
        @Parameter(description = "每页多少条")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Response<Page<ProjectResourceUsageVO>>

    @GET
    @Operation(summary = "触发项目资源统计上报任务", tags = ["v4_app_server_resources_upload_auto"])
    @Path("/triggerAutoUpload/{month}")
    fun triggerAutoUpload(
        @Parameter(description = "用户信息")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id")
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "所属周期月份")
        @PathParam("month")
        month: String,
        @Parameter(description = "起始统计日期")
        @QueryParam(value = "startDate")
        startDate: String?,
        @Parameter(description = "截止统计日期")
        @QueryParam("endDate")
        endDate: String?
    ): Response<Boolean>

    @POST
    @Operation(summary = "手动上报项目资源统计数据", tags = ["v4_app_server_resources_upload_manual"])
    @Path("/manualUpload")
    fun triggerManualUpload(
        @Parameter(description = "用户信息")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目id")
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "待上报的数据")
        @Valid
        summary: ResourceCostSummary
    ): Response<Boolean>
}
