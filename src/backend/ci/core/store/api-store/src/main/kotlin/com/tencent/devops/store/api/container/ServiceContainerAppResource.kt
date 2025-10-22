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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_CONTAINER_APP", description = "服务-容器-app")
@Path("/service/containers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceContainerAppResource {

    @Operation(summary = "获取container对应的所有APP")
    @GET
    @Path("/apps")
    fun listApp(
        @Parameter(description = "系统", required = true)
        @QueryParam("os")
        os: String
    ): Result<List<ContainerAppWithVersion>>

    @Operation(summary = "获取container对应的APP")
    @GET
    @Path("/getApps")
    fun getApp(
        @Parameter(description = "系统", required = true)
        @QueryParam("os")
        os: String
    ): Result<List<BuildEnv>>

    @Operation(summary = "构建机环境变量")
    @GET
    @Path("/getBuildEnv")
    fun getBuildEnv(
        @Parameter(description = "环境变量名称", required = true)
        @QueryParam("name")
        name: String,
        @Parameter(description = "环境变量版本", required = true)
        @QueryParam("version")
        version: String,
        @Parameter(description = "操作系统名称", required = true)
        @QueryParam("os")
        os: String
    ): Result<BuildEnv?>
}
