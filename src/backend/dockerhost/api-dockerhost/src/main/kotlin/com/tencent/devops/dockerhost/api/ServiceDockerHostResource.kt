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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.Status
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["DOCKER_HOST"], description = "DockerHost")
@Path("/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceDockerHostResource {


    @ApiOperation("Docker build")
    @POST
    @Path("/build/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
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
            @ApiParam("镜像名称", required = true)
            dockerBuildParam: DockerBuildParam
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
        buildId: String
    ): Result<Pair<Status, String?>>

    @ApiOperation("startBuild")
    @POST
    @Path("/startBuild")
    fun startBuild(
            @ApiParam("dockerHost构建信息", required = true)
            dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<Boolean>

    @ApiOperation("endBuild")
    @POST
    @Path("/endBuild")
    fun endBuild(
            @ApiParam("dockerHost构建信息", required = true)
            dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<Boolean>


}