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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.log.pojo.message.LogMessage
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 *
 * Powered By Tencent
 */
@Suppress("LongParameterList")
@Tag(name = "SERVICE_LOG_PRINT", description = "服务-日志打印资源")
@Path("/service/logs/print")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceLogPrintResource {

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
        @Parameter(description = "job id或者container的34位id", required = false)
        @QueryParam("jobId")
        containerHashId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "插件的存储模式", required = false)
        @QueryParam("logStorageMode")
        logStorageMode: LogStorageMode? = null,
        @Parameter(description = "job id", required = false)
        @QueryParam("userJobId")
        jobId: String?,
        @Parameter(description = "step id", required = false)
        @QueryParam("stepId")
        stepId: String?
    ): Result<Boolean>
}
