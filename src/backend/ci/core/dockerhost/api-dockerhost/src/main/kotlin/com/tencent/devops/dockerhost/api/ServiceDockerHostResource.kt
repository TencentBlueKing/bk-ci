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

package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.pojo.CheckImageRequest
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerHostLoad
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.Status
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "DOCKER_HOST", description = "DockerHost")
@Path("/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceDockerHostResource {

    @Operation(summary = "Docker build")
    @POST
    @Path("/build/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{elementId}")
    fun dockerBuild(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "elementId", required = true)
        @PathParam("elementId")
        elementId: String?,
        @Parameter(description = "镜像名称", required = true)
        dockerBuildParam: DockerBuildParam,
        @Context request: HttpServletRequest
    ): Result<Boolean>

    @Operation(summary = "Docker build")
    @GET
    @Path("/build/{vmSeqId}/{buildId}")
    fun getDockerBuildStatus(
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Context request: HttpServletRequest
    ): Result<Pair<Status, String>>

    @Operation(summary = "验证镜像是否合法")
    @POST
    @Path("/build/image/buildIds/{buildId}/check")
    fun checkImage(
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "验证镜像合法性请求报文体", required = true)
        checkImageRequest: CheckImageRequest,
        @Parameter(description = "containerId", required = false)
        @QueryParam("containerId")
        containerId: String?,
        @Parameter(description = "containerHashId", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?
    ): Result<CheckImageResponse?>

    @Operation(summary = "Docker run")
    @POST
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
    fun dockerRun(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "插件ID", required = false)
        @QueryParam("taskId")
        pipelineTaskId: String?,
        @Parameter(description = "镜像名称", required = true)
        dockerRunParam: DockerRunParam,
        @Context request: HttpServletRequest
    ): Result<DockerRunResponse>

    @Operation(summary = "get docker log")
    @GET
    @Path("/runlog/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}/{logStartTimeStamp}")
    fun getDockerRunLogs(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @Parameter(description = "logStartTimeStamp", required = true)
        @PathParam("logStartTimeStamp")
        logStartTimeStamp: Int,
        @Parameter(description = "printLog", required = false)
        @QueryParam("printLog")
        printLog: Boolean? = true,
        @Context request: HttpServletRequest
    ): Result<DockerLogsResponse>

    @Operation(summary = "stop docker run")
    @DELETE
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}")
    fun dockerStop(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @Context request: HttpServletRequest
    ): Result<Boolean>

    @Operation(summary = "启动流水线构建")
    @POST
    @Path("/build/start")
    fun startBuild(
        @Parameter(description = "构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<String>

    @Operation(summary = "终止流水线构建")
    @DELETE
    @Path("/build/end")
    fun endBuild(
        @Parameter(description = "构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<Boolean>

    @Operation(summary = "获取docker母机负载")
    @GET
    @Path("/host/load")
    fun getDockerHostLoad(): Result<DockerHostLoad>

    @Operation(summary = "查询容器状态")
    @GET
    @Path("/container/{containerId}/status")
    fun getContainerStatus(
        @Parameter(description = "容器ID", required = true)
        @PathParam("containerId")
        containerId: String
    ): Result<Boolean>
}
