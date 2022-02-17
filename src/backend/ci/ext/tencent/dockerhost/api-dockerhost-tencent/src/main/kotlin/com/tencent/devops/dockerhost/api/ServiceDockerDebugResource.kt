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
import com.tencent.devops.dispatch.docker.pojo.ContainerInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["DOCKER_DEBUG"], description = "docker debug")
@Path("/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceDockerDebugResource {

    @ApiOperation("启动流水线调试")
    @POST
    @Path("/debug/start")
    fun startDebug(
        @ApiParam("容器信息", required = true)
        dockerStartDebugInfo: ContainerInfo
    ): Result<String>

    @ApiOperation("获取调试url")
    @GET
    @Path("/debug/getWsUrl")
    fun getWebSocketUrl(
        @ApiParam("蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("蓝盾构建ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("容器ID", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<String>

    @ApiOperation("终止流水线调试")
    @POST
    @Path("/debug/end")
    fun endDebug(
        @ApiParam("容器信息", required = true)
        dockerEndDebugInfo: ContainerInfo
    ): Result<Boolean>
}
