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

package com.tencent.devops.engine.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.process.pojo.BuildJobResult
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "ENGINE_BUILD_JOB", description = "引擎-构建机请求|此接口不接受服务间Feign，只接受构建机处请求")
@Path("/build/worker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildJobResource {
    @Operation(summary = "构建机器启动成功")
    @PUT
    @Path("/started")
    fun jobStarted(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "网络问题导致的重试次数", required = false)
        @QueryParam("retryCount")
        retryCount: String
    ): Result<BuildVariables>

    @Operation(summary = "构建机请求获取任务")
    @GET
    @Path("/claim")
    fun claimTask(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String
    ): Result<BuildTask>

    @Operation(summary = "构建机Job完成任务")
    @POST
    @Path("/complete")
    fun completeTask(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "执行结果", required = true)
        result: BuildTaskResult
    ): Result<Boolean>

    @Operation(summary = "结束构建机Job")
    @POST
    @Path("/end")
    fun jobEnd(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "执行结果", required = false)
        result: BuildJobResult? = null
    ): Result<Boolean>

    @Operation(summary = "Job超时触发")
    @POST
    @Path("/timeout")
    fun jobTimeout(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String
    ): Result<Boolean>

    @Operation(summary = "Job心跳请求")
    @POST
    @Path("/heartbeat")
    fun jobHeartbeat(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String
    ): Result<Boolean>

    @Operation(summary = "Job心跳请求V1版")
    @POST
    @Path("/heartbeat/v1")
    @Suppress("LongParameterList")
    fun jobHeartbeatV1(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int? = null,
        @Parameter(description = "心跳请求报文体", required = false)
        jobHeartbeatRequest: JobHeartbeatRequest? = null
    ): Result<HeartBeatInfo>

    @Operation(summary = "job异常上报并停止构建") // #5046 增加启动时异常上报，并停止构建，如果网络通的话
    @POST
    @Path("/submit_error")
    fun submitError(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        @BkField(required = true)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        @BkField(required = true)
        vmSeqId: String,
        @Parameter(description = "执行结果", required = true)
        errorInfo: ErrorInfo
    ): Result<Boolean>

    @Operation(summary = "获取当前构建的构建详情页")
    @GET
    @Path("/detail_url")
    fun getBuildDetailUrl(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String
    ): Result<String>

    @Operation(summary = "获取当前运行Job的配置信息")
    @GET
    @Path("/dispatch_config")
    fun getBuildDispatchType(
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        @BkField(required = true)
        vmSeqId: String
    ): Result<String?>
}
