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

package com.tencent.devops.dispatch.kubernetes.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildStatusResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobLogResp
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchJobReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_DISPATCH_BASE_JOB", description = "构建-DISPATCH-BASE JOB资源操作")
@Path("/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildBaseJobResource {

    @Operation(summary = "启动job")
    @POST
    @Path("/job/create")
    fun createJob(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "Job结构", required = true)
        jobReq: DispatchJobReq
    ): Result<DispatchTaskResp>

    @Operation(summary = "获取job状态")
    @GET
    @Path("/job/{jobName}/status")
    fun getJobStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "jobName", required = true)
        @PathParam("jobName")
        jobName: String
    ): Result<DispatchBuildStatusResp>

    @Operation(summary = "获取job日志")
    @GET
    @Path("/job/{jobName}/logs")
    fun getJobLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "jobName", required = true)
        @PathParam("jobName")
        jobName: String,
        @Parameter(description = "sinceTime", required = true)
        @QueryParam("sinceTime")
        sinceTime: Int?
    ): Result<DispatchJobLogResp>

    @Operation(summary = "构建并推送镜像接口")
    @POST
    @Path("/image/buildPush")
    fun buildAndPushImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建结构", required = true)
        buildImageReq: DispatchBuildImageReq
    ): Result<DispatchTaskResp>
}
