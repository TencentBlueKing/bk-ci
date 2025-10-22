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

package com.tencent.devops.dispatch.docker.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerHostZone
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.SpecialDockerHostVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_DOCKER_HOST", description = "服务-获取构建容器信息")
@Path("/service/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("dispatch") // 指明接入到哪个微服务
interface ServiceDockerHostResource {

    @Operation(summary = "获取dockerhost列表")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Page<DockerHostZone>

    @Operation(summary = "更新构建信息")
    @PUT
    @Path("/builds/{buildId}/vmseqs/{vmSeqId}")
    fun updateContainerId(
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: Int,
        @Parameter(description = "容器信息", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<Boolean>

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

    @POST
    @Path("/specialDockerHost/add")
    @Operation(summary = "批量新增专机配置")
    fun createSpecialDockerHost(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "专机配置列表", required = true)
        specialDockerHostVOs: List<SpecialDockerHostVO>
    ): Result<Boolean>
}
