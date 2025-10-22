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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_QUALITY_V4", description = "QUALITY-质量红线相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/quality")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwQualityResourceV4 {

    @Operation(summary = "获取拦截规则列表", tags = ["v4_app_quality_rule_list", "v4_user_quality_rule_list"])
    @Path("/rule")
    @GET
    fun listRule(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页目", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>>

    @Operation(summary = "创建拦截规则", tags = ["v4_user_quality_rule_create", "v4_app_quality_rule_create"])
    @Path("/rule")
    @POST
    fun createRule(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则内容", required = true)
        rule: RuleCreateRequest
    ): Result<String>

    @Operation(summary = "更新拦截规则列表", tags = ["v4_user_quality_rule_update", "v4_app_quality_rule_update"])
    @Path("/rule")
    @PUT
    fun updateRule(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则ID", required = true)
        @QueryParam("ruleHashId")
        ruleHashId: String,
        @Parameter(description = "规则内容", required = true)
        rule: RuleUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "删除拦截规则列表", tags = ["v4_user_quality_rule_delete", "v4_app_quality_rule_delete"])
    @Path("/rule")
    @DELETE
    fun deleteRule(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则ID", required = true)
        @QueryParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取拦截记录", tags = ["v4_app_quality_intercepts_list", "v4_user_quality_intercepts_list"])
    @Path("/intercept_list")
    @GET
    fun listIntercepts(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "规则ID", required = false)
        @QueryParam("ruleHashId")
        ruleHashId: String?,
        @Parameter(description = "状态", required = false, schema = Schema(type = "ENUM(PASS, FAIL)"))
        @QueryParam("interceptResult")
        interceptResult: RuleInterceptResult?,
        @Parameter(description = "开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "截止时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "页号", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>

    @Operation(
        summary = "获取Stream红线列表",
        tags = ["v4_app_quality_rule_build_history", "v4_user_quality_rule_build_history"]
    )
    @Path("/rule_build_history_list")
    @GET
    fun listBuildHisRule(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "规则ID", required = false)
        @QueryParam("ruleHashId")
        ruleHashId: String?,
        @Parameter(description = "开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "截止时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "页号", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>
}
