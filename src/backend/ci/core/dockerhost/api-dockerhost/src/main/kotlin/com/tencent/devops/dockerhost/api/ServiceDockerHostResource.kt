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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Api(tags = ["DOCKER_HOST"], description = "DockerHost")
@Path("/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceDockerHostResource {

    @ApiOperation("Docker build")
    @POST
    @Path("/build/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{elementId}")
    fun dockerBuild(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "elementId", required = true)
        @PathParam("elementId")
        elementId: String?,
        @ApiParam("镜像名称", required = true)
        dockerBuildParam: DockerBuildParam,
        @Context request: HttpServletRequest
    ): Result<Boolean>

    @ApiOperation("Docker build")
    @GET
    @Path("/build/{vmSeqId}/{buildId}")
    fun getDockerBuildStatus(
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Context request: HttpServletRequest
    ): Result<Pair<Status, String>>

    @ApiOperation("验证镜像是否合法")
    @POST
    @Path("/build/image/buildIds/{buildId}/check")
    fun checkImage(
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("验证镜像合法性请求报文体", required = true)
        checkImageRequest: CheckImageRequest,
        @ApiParam("containerId", required = false)
        @QueryParam("containerId")
        containerId: String?,
        @ApiParam("containerHashId", required = false)
        @QueryParam("containerHashId")
        containerHashId: String?
    ): Result<CheckImageResponse?>

    @ApiOperation("Docker run")
    @POST
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
    fun dockerRun(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("插件ID", required = false)
        @QueryParam("taskId")
        pipelineTaskId: String?,
        @ApiParam("镜像名称", required = true)
        dockerRunParam: DockerRunParam,
        @Context request: HttpServletRequest
    ): Result<DockerRunResponse>

    @ApiOperation("get docker log")
    @GET
    @Path("/runlog/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}/{logStartTimeStamp}")
    fun getDockerRunLogs(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @ApiParam("logStartTimeStamp", required = true)
        @PathParam("logStartTimeStamp")
        logStartTimeStamp: Int,
        @ApiParam("printLog", required = false)
        @QueryParam("printLog")
        printLog: Boolean? = true,
        @Context request: HttpServletRequest
    ): Result<DockerLogsResponse>

    @ApiOperation("stop docker run")
    @DELETE
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}")
    fun dockerStop(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @Context request: HttpServletRequest
    ): Result<Boolean>

    @ApiOperation("启动流水线构建")
    @POST
    @Path("/build/start")
    fun startBuild(
        @ApiParam("构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<String>

    @ApiOperation("终止流水线构建")
    @DELETE
    @Path("/build/end")
    fun endBuild(
        @ApiParam("构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<Boolean>

    @ApiOperation("获取docker母机负载")
    @GET
    @Path("/host/load")
    fun getDockerHostLoad(): Result<DockerHostLoad>

    @ApiOperation("查询容器状态")
    @GET
    @Path("/container/{containerId}/status")
    fun getContainerStatus(
        @ApiParam("容器ID", required = true)
        @PathParam("containerId")
        containerId: String
    ): Result<Boolean>
}
