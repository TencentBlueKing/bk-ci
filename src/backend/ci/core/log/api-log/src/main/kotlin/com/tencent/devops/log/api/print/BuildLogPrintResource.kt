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

package com.tencent.devops.log.api.print

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 *
 * Powered By Tencent
 */
@Tag(name = "BUILD_LOG_PRINT", description = "构建-日志资源")
@Path("/build/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface BuildLogPrintResource {

    @Operation(summary = "写入一条日志")
    @POST
    @Path("/")
    fun addLogLine(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "一条日志", required = true)
        logMessage: LogMessage
    ): Result<Boolean>

    @Operation(summary = "写入一条红色高亮日志")
    @POST
    @Path("/red")
    fun addRedLogLine(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "一条日志", required = true)
        logMessage: LogMessage
    ): Result<Boolean>

    @Operation(summary = "写入一条黄色高亮日志")
    @POST
    @Path("/yellow")
    fun addYellowLogLine(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "一条日志", required = true)
        logMessage: LogMessage
    ): Result<Boolean>

    @Operation(summary = "写入多条日志")
    @POST
    @Path("/multi")
    fun addLogMultiLine(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "多条日志列表", required = true)
        logMessages: List<LogMessage>
    ): Result<Boolean>

    @Operation(summary = "写入日志状态")
    @POST
    @Path("/status")
    fun addLogStatus(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "分辨插件的tag，默认填对应插件id", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "插件内的分类的子tag，默认为空", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @Parameter(description = "container的34位id", required = false)
        @QueryParam("jobId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "日志存储模式", required = false)
        @QueryParam("logMode")
        logMode: String?,
        @Parameter(description = "job id", required = false)
        @QueryParam("userJobId")
        jobId: String?,
        @Parameter(description = "step id", required = false)
        @QueryParam("stepId")
        stepId: String?
    ): Result<Boolean>

    @Operation(summary = "更新日志状态")
    @PUT
    @Path("/status")
    fun updateLogStatus(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "是否已构建完成", required = true)
        @QueryParam("finished")
        finished: Boolean,
        @Parameter(description = "分辨插件的tag，默认填对应插件id", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "插件内的分类的子tag，默认为空", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @Parameter(description = "container的34位id", required = false)
        @QueryParam("jobId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "日志存储模式", required = false)
        @QueryParam("logMode")
        logMode: String?,
        @Parameter(description = "job id", required = false)
        @QueryParam("userJobId")
        jobId: String?,
        @Parameter(description = "step id", required = false)
        @QueryParam("stepId")
        stepId: String?
    ): Result<Boolean>

    @Operation(summary = "更新日志存储模式的流转状态")
    @POST
    @Path("/mode")
    fun updateLogStorageMode(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int,
        @Parameter(description = "所有插件的日志存储结果", required = true)
        propertyList: List<TaskBuildLogProperty>
    ): Result<Boolean>

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
        @Parameter(description = "指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @Operation(summary = "获取某行后的日志")
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
        @Parameter(description = "指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @Parameter(description = "对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>
}
