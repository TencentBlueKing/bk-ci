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

package com.tencent.devops.log.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogLineNum
import com.tencent.devops.common.log.pojo.QueryLogStatus
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 *
 * Powered By Tencent
 */
@Tag(name = "SERVICE_LOG", description = "服务-日志资源")
@Path("/service/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceLogResource {

    @Operation(summary = "根据构建ID获取初始化所有日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun getInitLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String? = null,
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

    @Operation(summary = "获取更多日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/more")
    fun getMoreLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
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
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId", required = false)
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

    @Operation(summary = "获取某行前的日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/after")
    fun getAfterLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId", required = false)
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

    @Operation(summary = "下载日志接口")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "对应element ID", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "对应containerHashId", required = false)
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

    @Operation(summary = "获取插件的的日志状态")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/mode")
    fun getLogMode(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "对应elementId", required = true)
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

    @Operation(summary = "获取当前构建的最大行号")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/last_line_num")
    fun getLogLastLineNum(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<QueryLogLineNum>
}
