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

package com.tencent.devops.store.api.container

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResp
import com.tencent.devops.store.pojo.container.ContainerType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_CONTAINER"], description = "流水线-构建容器")
@Path("/user/pipeline/container")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserContainerResource {

    @ApiOperation("获取构建容器资源信息")
    @GET
    @Path("/projects/{projectCode}/containers/{containerId}/oss/{os}")
    fun getContainerResource(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("容器ID", required = true)
        @PathParam("containerId")
        containerId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("资源类型", required = false)
        @QueryParam("buildType")
        buildType: BuildType
    ): Result<ContainerResource?>

    @ApiOperation("获取构建容器资源信息")
    @GET
    @Path("/projects/{projectCode}/oss/{os}")
    fun getContainerResource(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("资源类型", required = false)
        @QueryParam("buildType")
        buildType: BuildType
    ): Result<ContainerResource?>

    @ApiOperation("获取所有的流水线构建容器信息")
    @GET
    @Path("/{projectCode}")
    fun getAllContainerInfos(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<List<ContainerResp>>

    @ApiOperation("根据容器类型获取流水线构建容器信息")
    @GET
    @Path("/{projectCode}/{type}")
    fun getContainerInfoByType(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("容器类型(trigger:触发器 vmBuild:构建环境 normal:无编译环境)", required = true)
        @PathParam("type")
        type: String
    ): Result<List<ContainerResp>>

    @ApiOperation("根据容器类型和操作系统获取流水线构建容器信息")
    @GET
    @Path("/{projectCode}/{type}/{os}")
    fun getContainerInfoByTypeAndOs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("容器类型(trigger:触发器 vmBuild:构建环境 normal:无编译环境)", required = true)
        @PathParam("type")
        type: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ContainerResp>>

    @ApiOperation("获取全部流水线构建容器信息")
    @GET
    @Path("/all")
    fun getAllContainers(): Result<List<ContainerType>?>
}
