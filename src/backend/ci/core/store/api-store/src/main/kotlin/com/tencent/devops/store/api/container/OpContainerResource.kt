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

import com.tencent.devops.store.pojo.container.Container
import com.tencent.devops.store.pojo.container.ContainerRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE_CONTAINER", description = "OP-流水线-构建容器")
@Path("/op/pipeline/container")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpContainerResource {

    @Operation(summary = "添加流水线构建容器")
    @POST
    @Path("/")
    fun add(
        @Parameter(description = "流水线构建容器请求报文体", required = true)
        pipelineContainerRequest: ContainerRequest
    ): Result<Boolean>

    @Operation(summary = "更新流水线构建容器信息")
    @PUT
    @Path("/{id}")
    fun update(
        @Parameter(description = "容器ID", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "流水线构建容器请求报文体", required = true)
        pipelineContainerRequest: ContainerRequest
    ): Result<Boolean>

    @Operation(summary = "获取所有的流水线构建容器信息")
    @GET
    @Path("/")
    fun listAllContainers(): Result<List<Container>>

    @Operation(summary = "根据ID获取流水线构建容器信息")
    @GET
    @Path("/{id}")
    fun getContainerById(
        @Parameter(description = "容器ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Container?>

    @Operation(summary = "根据ID删除流水线构建容器信息")
    @DELETE
    @Path("/{id}")
    fun deleteContainerById(
        @Parameter(description = "容器ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
