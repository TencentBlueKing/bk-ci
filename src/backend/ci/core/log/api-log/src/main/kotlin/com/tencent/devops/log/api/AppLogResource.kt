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
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.log.pojo.enums.LogType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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
@Api(tags = ["APP_LOG"], description = "用户-日志资源")
@Path("/app/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface AppLogResource {

    @ApiOperation("根据构建ID获取初始化所有日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun getInitLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<PageQueryLogs>

    @ApiOperation("获取更多日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/more")
    fun getMoreLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("日志行数", required = false)
        @QueryParam("num")
        num: Int? = 100,
        @ApiParam("是否正序输出", required = false)
        @QueryParam("fromStart")
        fromStart: Boolean? = true,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @ApiParam("结尾行号", required = true)
        @QueryParam("end")
        end: Long,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @ApiOperation("获取某行后的日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/after")
    fun getAfterLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @ApiOperation("获取某行前指定行数的日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/before")
    fun getBeforeLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("结束行号", required = true)
        @QueryParam("end")
        end: Long,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("返回日志条数", required = false)
        @QueryParam("size")
        size: Int?,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @ApiOperation("下载日志接口")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("对应element ID", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Response

    @ApiOperation("根据构建ID获取初始化最后日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/end")
    fun getEndLogsPage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("返回日志条数", required = true)
        @QueryParam("size")
        size: Int,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<EndPageQueryLogs>

    @ApiOperation("根据构建ID获取初始化底部指定行数的日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/bottom")
    fun getBottomLogs(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @ApiParam("过滤日志级别", required = false)
        @QueryParam("logType")
        logType: LogType? = null,
        @ApiParam("返回日志条数", required = false)
        @QueryParam("size")
        size: Int?,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("指定subTag", required = false)
        @QueryParam("subTag")
        subTag: String?,
        @ApiParam("对应jobId", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>
}
