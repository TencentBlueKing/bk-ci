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

package com.tencent.devops.store.api.container

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResp
import com.tencent.devops.store.pojo.container.ContainerType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_CONTAINER", description = "流水线-构建容器")
@Path("/user/pipeline/container")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserContainerResource {

    @Operation(summary = "获取构建容器资源信息")
    @GET
    @Path("/projects/{projectCode}/containers/{containerId}/oss/{os}")
    fun getContainerResource(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "容器ID", required = true)
        @PathParam("containerId")
        containerId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "资源类型", required = false)
        @QueryParam("buildType")
        buildType: BuildType
    ): Result<ContainerResource?>

    @Operation(summary = "获取构建容器资源信息")
    @GET
    @Path("/projects/{projectCode}/oss/{os}")
    fun getContainerResource(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "资源类型", required = false)
        @QueryParam("buildType")
        buildType: BuildType
    ): Result<ContainerResource?>

    @Operation(summary = "获取所有的流水线构建容器信息")
    @GET
    @Path("/{projectCode}")
    fun getAllContainerInfos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<ContainerResp>>

    @Operation(summary = "根据容器类型获取流水线构建容器信息")
    @GET
    @Path("/{projectCode}/{type}")
    fun getContainerInfoByType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "容器类型(trigger:触发器 vmBuild:构建环境 normal:无编译环境)", required = true)
        @PathParam("type")
        type: String
    ): Result<List<ContainerResp>>

    @Operation(summary = "根据容器类型和操作系统获取流水线构建容器信息")
    @GET
    @Path("/{projectCode}/{type}/{os}")
    fun getContainerInfoByTypeAndOs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "容器类型(trigger:触发器 vmBuild:构建环境 normal:无编译环境)", required = true)
        @PathParam("type")
        type: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ContainerResp>>

    @Operation(summary = "获取全部流水线构建容器信息")
    @GET
    @Path("/all")
    fun getAllContainers(): Result<List<ContainerType>?>
}
