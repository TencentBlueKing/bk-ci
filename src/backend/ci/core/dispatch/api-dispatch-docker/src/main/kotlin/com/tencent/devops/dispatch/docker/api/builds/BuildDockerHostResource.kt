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

package com.tencent.devops.dispatch.docker.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_DOCKER_HOST", description = "构建-构建执行DOCKER_HOST资源")
@Path("/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)@Suppress("ALL")
interface BuildDockerHostResource {

    @GET
    @Path("/resource-config/pipelines/{pipelineId}/vmSeqs/{vmSeqId}")
    @Operation(summary = "获取蓝盾项目的docker性能配置")
    fun getResourceConfig(
        @Parameter(description = "蓝盾流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "JOB ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<DockerResourceOptionsVO>

    @GET
    @Path("/qpc/projects/{projectId}/builds/{buildId}/vmSeqs/{vmSeqId}")
    @Operation(summary = "获取蓝盾项目的docker性能配置")
    fun getQpcGitProjectList(
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "JOB ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "POOLNo", required = true)
        @QueryParam("poolNo")
        poolNo: Int
    ): Result<List<String>>

    @Operation(summary = "上报日志信息")
    @POST
    @Path("/log")
    fun log(
        @Parameter(description = "buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "red", required = true)
        @QueryParam("red")
        red: Boolean,
        @Parameter(description = "message", required = true)
        @QueryParam("message")
        message: String,
        @Parameter(description = "tag", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "jobId", required = false)
        @QueryParam("jobId")
        jobId: String?
    ): Result<Boolean>?

    @Operation(summary = "上报日志信息")
    @POST
    @Path("/postlog")
    fun postLog(
        @Parameter(description = "buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "red", required = false)
        @QueryParam("red")
        red: Boolean,
        @Parameter(description = "message", required = true)
        message: String,
        @Parameter(description = "tag", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "jobId", required = false)
        @QueryParam("jobId")
        jobId: String?
    ): Result<Boolean>?

    @Operation(summary = "获取公共镜像")
    @GET
    @Path("/public/images")
    fun getPublicImages(): Result<List<ImageRepoInfo>>

    @POST
    @Path("/dockerIp/{dockerIp}/refresh")
    @Operation(summary = "刷新Docker构建机状态")
    fun refresh(
        @Parameter(description = "构建机信息", required = true)
        @PathParam("dockerIp")
        dockerIp: String,
        @Parameter(description = "构建机信息", required = true)
        dockerIpInfoVO: DockerIpInfoVO
    ): Result<Boolean>
}
