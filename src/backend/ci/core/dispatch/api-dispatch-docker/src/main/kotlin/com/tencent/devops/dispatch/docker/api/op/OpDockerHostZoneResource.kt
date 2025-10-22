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
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.dispatch.docker.pojo.DockerHostZoneWithPage
import com.tencent.devops.dispatch.docker.pojo.SpecialDockerHostVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_DOCKERHOST_ZONE", description = "DockerHost母机管理")
@Path("/op/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpDockerHostZoneResource {

    @Operation(summary = "添加DockerHost母机")
    @POST
    @Path("/create")
    fun create(
        @Parameter(description = "DockerHost母机", required = true)
        @QueryParam("hostIp")
        hostIp: String,
        @Parameter(description = "DockerHost母机区域", required = true)
        @QueryParam("zone")
        zone: Zone,
        @Parameter(description = "备注", required = true)
        @QueryParam("remark")
        remark: String?
    ): Result<Boolean>

    @Operation(summary = "删除DockerHost母机")
    @POST
    @Path("/delete")
    fun delete(
        @Parameter(description = "DockerHost母机IP", required = true)
        @QueryParam("hostIp")
        hostIp: String
    ): Result<Boolean>

    @Operation(summary = "列出DockerHost母机")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<DockerHostZoneWithPage>

    @Operation(summary = "启用DockerHost当构建机")
    @POST
    @Path("/enable")
    fun enable(
        @Parameter(description = "DockerHost母机IP", required = true)
        @QueryParam("hostIp")
        hostIp: String,
        @Parameter(description = "enable", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @GET
    @Path("/specialDockerHost/list")
    @Operation(summary = "批量新增专机配置")
    fun listSpecialDockerHost(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<SpecialDockerHostVO>>

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

    @PUT
    @Path("/specialDockerHost/update")
    @Operation(summary = "更新专机配置")
    fun updateSpecialDockerHost(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "专机配置", required = true)
        specialDockerHostVO: SpecialDockerHostVO
    ): Result<Boolean>

    @DELETE
    @Path("/specialDockerHost/delete/{projectId}")
    @Operation(summary = "删除专机配置")
    fun deleteSpecialDockerHost(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
