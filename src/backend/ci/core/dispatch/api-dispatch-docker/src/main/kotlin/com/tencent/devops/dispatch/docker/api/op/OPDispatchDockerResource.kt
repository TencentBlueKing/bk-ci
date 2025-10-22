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

package com.tencent.devops.dispatch.docker.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.DockerIpListPage
import com.tencent.devops.dispatch.docker.pojo.DockerIpUpdateVO
import com.tencent.devops.dispatch.docker.pojo.HostDriftLoad
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_DISPATCH_IDC", description = "OP-IDC构建机管理接口")
@Path("/op/dispatchDocker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)@Suppress("ALL")
interface OPDispatchDockerResource {

    @GET
    @Path("/getDockerIpList")
    @Operation(summary = "获取Docker构建机列表")
    fun listDispatchDocker(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<DockerIpListPage<DockerIpInfoVO>>

    @POST
    @Path("/add")
    @Operation(summary = "批量新增Docker构建机")
    fun createDispatchDocker(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "IDC构建机信息", required = true)
        dockerIpInfoVOs: List<DockerIpInfoVO>
    ): Result<Boolean>

    @PUT
    @Path("/update/{dockerIp}")
    @Operation(summary = "更新Docker构建机状态")
    fun updateDispatchDocker(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "IDC构建机ID", required = true)
        @PathParam("dockerIp")
        dockerIp: String,
        @Parameter(description = "IDC构建机信息", required = true)
        dockerIpUpdateVO: DockerIpUpdateVO
    ): Result<Boolean>

    @PUT
    @Path("/update/all/enable")
    @Operation(summary = "重置所有Docker构建机状态可用")
    fun updateAllDispatchDockerEnable(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Boolean>

    @DELETE
    @Path("/delete/{dockerIp}")
    @Operation(summary = "删除Docker构建机")
    fun deleteDispatchDocker(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("dockerIp")
        dockerIp: String
    ): Result<Boolean>

    @DELETE
    @Path("/dockerBuildBinding/delete/{pipelineId}/{vmSeqId}")
    @Operation(summary = "删除Docker构建绑定关系")
    fun removeDockerBuildBinding(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建序列号", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>

    @GET
    @Path("/load-config/list")
    @Operation(summary = "获取Docker构建机负载配置")
    fun getDockerHostLoadConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Map<String, DockerHostLoadConfig>>

    @POST
    @Path("/load-config/add")
    @Operation(summary = "新增Docker构建机负载配置")
    fun createDockerHostLoadConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "创建IDC构建机所需信息", required = true)
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Result<Boolean>

    @GET
    @Path("/docker/threshold/list")
    @Operation(summary = "获取docker漂移负载阈值")
    fun getDockerDriftThreshold(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Map<String, String>>

    @POST
    @Path("/docker/threshold/update")
    @Operation(summary = "更新docker漂移负载阈值")
    fun updateDockerDriftThreshold(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "阈值", required = true)
        hostDriftLoad: HostDriftLoad
    ): Result<Boolean>
}
