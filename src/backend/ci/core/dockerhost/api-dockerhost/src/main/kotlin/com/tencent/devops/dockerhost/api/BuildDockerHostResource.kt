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
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.Status
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

@Tag(name = "DOCKER_HOST", description = "DockerHost")
@Path("/dockernew")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface BuildDockerHostResource {

    @Operation(summary = "Docker build")
    @POST
    @Path("/build/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
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
        @QueryParam("elementId")
        elementId: String,
        @Parameter(description = "是否开启镜像扫描", required = false)
        @QueryParam("scanFlag")
        scanFlag: Boolean?,
        @Parameter(description = "镜像构建参数", required = true)
        dockerBuildParam: DockerBuildParam
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
        buildId: String
    ): Result<Pair<Status, String>>
}
