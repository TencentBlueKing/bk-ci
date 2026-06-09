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
 *
 */

package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.vo.AuthorizationHealthVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareVO
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_MEMBER_INSIGHT_V4", description = "OPENAPI-权限成员洞察")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/project/{projectId}/member_insight")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
@BkApigwApi(version = "v4")
interface ApigwAuthMemberInsightResourceV4 {

    @GET
    @Path("/members/{memberId}/analysis")
    @Operation(
        summary = "成员权限分析报告",
        tags = ["v4_app_analyze_auth_member_permissions", "v4_user_analyze_auth_member_permissions"]
    )
    fun analyzeUserPermissions(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标成员ID(用户名等)", required = true)
        @PathParam("memberId")
        memberId: String
    ): Result<UserPermissionAnalysisVO>

    @GET
    @Path("/resources/{resourceType}/{resourceCode}/permissions_matrix")
    @Operation(
        summary = "资源权限矩阵",
        tags = [
            "v4_app_get_auth_resource_permissions_matrix",
            "v4_user_get_auth_resource_permissions_matrix"
        ]
    )
    fun getResourcePermissionsMatrix(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code", required = true)
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<ResourcePermissionsMatrixVO>

    @GET
    @Path("/diagnose")
    @Operation(
        summary = "权限诊断:分析成员为什么没有某权限",
        tags = ["v4_app_diagnose_auth_permission", "v4_user_diagnose_auth_permission"]
    )
    fun diagnosePermission(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标成员ID", required = true)
        @QueryParam("memberId")
        memberId: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @Parameter(description = "权限动作(如 pipeline_execute)", required = true)
        @QueryParam("action")
        action: String
    ): Result<PermissionDiagnoseVO>

    @GET
    @Path("/compare")
    @Operation(
        summary = "权限对比:比较两个用户的权限差异",
        tags = ["v4_app_compare_auth_permissions", "v4_user_compare_auth_permissions"]
    )
    fun comparePermissions(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户A的ID", required = true)
        @QueryParam("userIdA")
        userIdA: String,
        @Parameter(description = "用户B的ID", required = true)
        @QueryParam("userIdB")
        userIdB: String,
        @Parameter(description = "限定的资源类型,不传则比较全部", required = false)
        @QueryParam("resourceType")
        resourceType: String? = null
    ): Result<PermissionCompareVO>

    @GET
    @Path("/authorization/health_check")
    @Operation(
        summary = "项目授权健康检查:扫描授权风险",
        tags = ["v4_app_check_auth_health", "v4_user_check_auth_health"]
    )
    fun checkAuthorizationHealth(
        @Parameter(
            description = "应用Code(OpenAPI调用方标识)",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<AuthorizationHealthVO>
}
