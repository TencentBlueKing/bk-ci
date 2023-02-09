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

package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleSummaryWithPermission
import com.tencent.devops.quality.pojo.RuleInterceptHistory
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPENAPI_QUALITY_V3"], description = "QUALITY-质量红线相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/quality")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwQualityResourceV3 {

    @ApiOperation("获取拦截规则列表", tags = ["v3_app_quality_rule_list", "v3_user_quality_rule_list"])
    @Path("/rules/list")
    @GET
    fun listRule(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(最大100条)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<QualityRuleSummaryWithPermission>>

    @ApiOperation("创建拦截规则", tags = ["v3_user_quality_rule_create", "v3_app_quality_rule_create"])
    @Path("/rules/create")
    @POST
    fun createRule(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("规则内容", required = true)
        rule: RuleCreateRequest
    ): Result<String>

    @ApiOperation("更新拦截规则列表", tags = ["v3_user_quality_rule_update", "v3_app_quality_rule_update"])
    @Path("/rules/{ruleHashId}/update")
    @PUT
    fun updateRule(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String,
        @ApiParam("规则内容", required = true)
        rule: RuleUpdateRequest
    ): Result<Boolean>

    @ApiOperation("删除拦截规则列表", tags = ["v3_user_quality_rule_delete", "v3_app_quality_rule_delete"])
    @Path("/rules/{ruleHashId}/delete")
    @DELETE
    fun deleteRule(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("规则ID", required = true)
        @PathParam("ruleHashId")
        ruleHashId: String
    ): Result<Boolean>

    @ApiOperation("获取拦截记录", tags = ["v3_app_quality_intercepts_list", "v3_user_quality_intercepts_list"])
    @Path("/intercepts/list")
    @GET
    fun listIntercepts(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("规则ID", required = false)
        @QueryParam("ruleHashId")
        ruleHashId: String?,
        @ApiParam("状态", required = false, type = "ENUM(PASS, FAIL)")
        @QueryParam("interceptResult")
        interceptResult: RuleInterceptResult?,
        @ApiParam("开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @ApiParam("截止时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("页号", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(最大100条)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>

    @ApiOperation(
        "获取Stream红线列表",
        tags = ["v3_app_quality_rule_build_history", "v3_user_quality_rule_build_history"]
    )
    @Path("/ruleBuildHis/list")
    @GET
    fun listBuildHisRule(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("规则ID", required = false)
        @QueryParam("ruleHashId")
        ruleHashId: String?,
        @ApiParam("开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @ApiParam("截止时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @ApiParam("页号", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(最大100条)", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RuleInterceptHistory>>
}
