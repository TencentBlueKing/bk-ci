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

import com.tencent.devops.store.pojo.app.ContainerApp
import com.tencent.devops.store.pojo.app.ContainerAppInfo
import com.tencent.devops.store.pojo.app.ContainerAppRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_CONTAINER_APP"], description = "OP-容器-编译环境")
@Path("/op/pipeline/container/app")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpContainerAppResource {

    @ApiOperation("添加编译环境")
    @POST
    @Path("/")
    fun addContainerApp(
        @ApiParam("容器编译环境请求实体", required = true)
        containerAppRequest: ContainerAppRequest
    ): Result<Boolean>

    @ApiOperation("删除编译环境")
    @DELETE
    @Path("/{id}")
    fun deleteContainerAppById(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("更新编译环境")
    @PUT
    @Path("/{id}")
    fun updateContainerApp(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int,
        @ApiParam("容器编译环境请求实体", required = true)
        containerAppRequest: ContainerAppRequest
    ): Result<Boolean>

    @ApiOperation("获取所有编译环境信息")
    @GET
    @Path("/")
    fun listContainerApps(): Result<List<ContainerApp>>

    @ApiOperation("根据ID获取编译环境信息")
    @GET
    @Path("/{id}")
    fun getContainerAppById(
        @ApiParam("编译环境ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<ContainerAppInfo?>
}
