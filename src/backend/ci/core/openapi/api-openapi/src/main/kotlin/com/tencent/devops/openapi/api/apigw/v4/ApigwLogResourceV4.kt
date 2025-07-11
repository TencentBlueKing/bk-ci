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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogLineNum
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "OPENAPI_LOG_V4", description = "OPENAPI-构建日志资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwLogResourceV4 {
    @Operation(summary = "根据构建ID获取初始化所有日志", tags = ["v4_app_log_init", "v4_user_log_init"])
    @GET
    @Path("/init_logs")
    fun getInitLogs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "对应elementId (e-开头)", required = false)
        @QueryParam("tag")
        elementId: String?,
        @Parameter(description = "对应containerHashId (c-开头)", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogs>

    @Operation(summary = "获取更多日志", tags = ["v4_app_log_more", "v4_user_log_more"])
    @GET
    @Path("/more_logs")
    fun getMoreLogs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "日志行数", required = false)
        @QueryParam("num")
        num: Int? = 100,
        @Parameter(description = "是否正序输出", required = false)
        @QueryParam("fromStart")
        fromStart: Boolean? = true,
        @Parameter(description = "起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "结尾行号", required = true)
        @QueryParam("end")
        end: Long,
        @Parameter(description = "对应elementId (e-开头)", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId (c-开头)", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogs>

    @Operation(summary = "获取某行后的日志", tags = ["v4_user_log_after", "v4_app_log_after"])
    @GET
    @Path("/after_line_logs")
    fun getAfterLogs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "起始行号,请分阶段获取构建日志，是否有后续日志需关注返回的hasMore字段。", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "对应elementId (e-开头)", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId (c-开头)", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogs>

    @Operation(
        summary = "下载日志接口(注意: 接口返回application/octet-stream数据，Request Header Accept 类型不一致将导致错误)",
        tags = ["v4_user_log_download", "v4_app_log_download"]
    )
    @GET
    @Path("/download_logs")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadLogs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "对应element ID (e-开头)", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId (c-开头)", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Response

    @Operation(summary = "获取插件的的日志状态", tags = ["v4_app_log_mode", "v4_user_log_mode"])
    @GET
    @Path("/log_mode")
    fun getLogMode(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "对应elementId (e-开头)", required = true)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogStatus>

    @Operation(summary = "获取当前构建的最大行号", tags = ["v4_app_log_line_num", "v4_user_log_line_num"])
    @GET
    @Path("/last_line_num")
    fun getLogLastLineNum(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID (p-开头)", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID (b-开头)", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogLineNum>
}
