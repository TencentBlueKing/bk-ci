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

package com.tencent.devops.dispatch.docker.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DebugStartParam
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoad
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
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_DOCKER_HOST", description = "用户-获取构建容器信息")
@Path("/user/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDockerHostResource {

    @Operation(summary = "启动调试容器")
    @POST
    @Path("/startDebug")
    fun startDebug(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        debugStartParam: DebugStartParam
    ): Result<Boolean>?

    @Operation(summary = "根据pipelineId和vmSeqId获取容器信息")
    @GET
    @Path("/getDebugStatus/{projectId}/{pipelineId}/{vmSeqId}")
    fun getDebugStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<ContainerInfo>?

    @Operation(summary = "终止调试容器")
    @POST
    @Path("/stopDebug/{projectId}/{pipelineId}/{vmSeqId}")
    fun stopDebug(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>?

    @Operation(summary = "根据buildId获取容器信息")
    @GET
    @Path("/getContainerInfo/{projectId}/{pipelineId}/{buildId}/{vmSeqId}")
    fun getContainerInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "容器Id", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<ContainerInfo>?

    @Operation(summary = "获取灰度开启webConsole的项目")
    @GET
    @Path("/getGreyWebConsoleProject")
    fun getGreyWebConsoleProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<String>>

    @Operation(summary = "清理流水线上次使用的构建机信息")
    @POST
    @Path("/cleanIp/{projectId}/{pipelineId}/{vmSeqId}")
    fun cleanIp(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>?

    @GET
    @Path("/dockerhost-load")
    @Operation(summary = "获取dockerhost集群平均负载信息")
    fun getDockerHostLoad(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<DockerHostLoad>
}
