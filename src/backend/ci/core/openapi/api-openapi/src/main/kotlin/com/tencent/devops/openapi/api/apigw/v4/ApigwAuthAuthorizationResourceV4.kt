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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.pojo.pipeline.ResetPipelineAuthorizationReq
import com.tencent.devops.openapi.pojo.pipeline.ResetPipelineAuthorizationResp
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_AUTHORIZATION_V4", description = "OPENAPI-权限授权管理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/authorization/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
@Suppress("LongParameterList")
interface ApigwAuthAuthorizationResourceV4 {
    @GET
    @Path("/{resourceType}/{resourceCode}/get_resource_authorization")
    @Operation(
        summary = "获取资源授予记录",
        tags = ["v4_app_get_resource_authorization", "v4_user_get_resource_authorization"]
    )
    fun getResourceAuthorization(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @PathParam("resourceCode")
        @Parameter(description = "资源code", required = true)
        resourceCode: String
    ): Result<ResourceAuthorizationResponse>

    @PUT
    @Path("/pipelines/{pipelineId}/reset_authorization")
    @Operation(
        summary = "重置流水线授权人",
        tags = ["v4_app_reset_pipeline_authorization", "v4_user_reset_pipeline_authorization"]
    )
    fun resetPipelineAuthorization(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "重置流水线授权请求体", required = true)
        request: ResetPipelineAuthorizationReq
    ): Result<ResetPipelineAuthorizationResp>

    @GET
    @Path("/pipelines/list_authorization")
    @Operation(
        summary = "获取流水线代持人列表",
        tags = ["v4_app_list_pipeline_authorization", "v4_user_list_pipeline_authorization"]
    )
    fun listPipelineAuthorization(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线名称(模糊匹配)", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "代持人", required = false)
        @QueryParam("handoverFrom")
        handoverFrom: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        @DefaultValue("1")
        page: Int,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        @DefaultValue("20")
        pageSize: Int
    ): Result<SQLPage<ResourceAuthorizationResponse>>
}
