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

package com.tencent.devops.ai.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 运营联调：将构建 Agent 的 BuildTools（与 Agent 内调用逻辑一致）的文本结果通过 OP 接口暴露。
 * 调用方需在 Header 传入操作人 [AUTH_HEADER_USER_ID]，行为与 Agent 工具内「当前用户」一致。
 */
@Tag(name = "OP_AI_BUILD_AGENT_TOOLS", description = "运营-构建 Agent 工具联调")
@Path("/op/ai/build-agent-tools")
@Produces(MediaType.APPLICATION_JSON)
interface OpAiBuildAgentToolsResource {

    // region Pipeline query

    @Operation(summary = "[Pipeline] searchPipelines")
    @POST
    @Path("/pipeline/search-pipelines")
    fun searchPipelines(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("keyword") keyword: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<String>

    @Operation(summary = "[Pipeline] getPipelineInfo")
    @POST
    @Path("/pipeline/get-pipeline-info")
    fun getPipelineInfo(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String
    ): Result<String>

    @Operation(summary = "[Pipeline] getPipelineStatus")
    @POST
    @Path("/pipeline/get-pipeline-status")
    fun getPipelineStatus(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String
    ): Result<String>

    // endregion

    // region Build operate

    @Operation(summary = "[Build] getManualStartupInfo")
    @POST
    @Path("/build/get-manual-startup-info")
    fun getManualStartupInfo(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String
    ): Result<String>

    @Operation(summary = "[Build] triggerBuild")
    @POST
    @Path("/build/trigger-build")
    fun triggerBuild(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("params") params: String?
    ): Result<String>

    @Operation(summary = "[Build] retryBuild")
    @POST
    @Path("/build/retry-build")
    fun retryBuild(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String
    ): Result<String>

    @Operation(summary = "[Build] stopBuild")
    @POST
    @Path("/build/stop-build")
    fun stopBuild(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String
    ): Result<String>

    // endregion

    // region Build query

    @Operation(summary = "[Build] getBuildHistory")
    @POST
    @Path("/build/get-build-history")
    fun getBuildHistory(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?,
        @QueryParam("status") status: String?,
        @QueryParam("startUser") startUser: String?
    ): Result<String>

    @Operation(summary = "[Build] getBuildDetail")
    @POST
    @Path("/build/get-build-detail")
    fun getBuildDetail(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String
    ): Result<String>

    @Operation(summary = "[Build] getBuildStatus")
    @POST
    @Path("/build/get-build-status")
    fun getBuildStatus(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String
    ): Result<String>

    @Operation(summary = "[Build] getBuildVars")
    @POST
    @Path("/build/get-build-vars")
    fun getBuildVars(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String
    ): Result<String>

    // endregion

    // region Logs

    @Operation(summary = "[Log] getBuildLogs")
    @POST
    @Path("/log/get-build-logs")
    fun getBuildLogs(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String,
        @QueryParam("buildId") buildId: String,
        @QueryParam("tag") tag: String?,
        @QueryParam("stepId") stepId: String?,
        @QueryParam("logType") logType: String?,
        @QueryParam("jobId") jobId: String?
    ): Result<String>

    // endregion
}
