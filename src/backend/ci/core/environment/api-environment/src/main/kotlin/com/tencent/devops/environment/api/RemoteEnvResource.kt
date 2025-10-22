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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvWithPermission
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ENVIRONMENT_AUTH", description = "服务-环境服务-权限中心")
@Path("/service/environment/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface RemoteEnvResource {

    @Operation(summary = "分页获取环境列表")
    @GET
    @Path("/projects/{projectId}/list/")
    fun listEnvForAuth(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @Parameter(description = "步长", required = false)
        @QueryParam("limit")
        limit: Int? = null
    ): Result<Page<EnvWithPermission>>

    @Operation(summary = "获取环境信息")
    @GET
    @Path("/infos")
    fun getEnvInfos(
        @Parameter(description = "节点Id串", required = true)
        @QueryParam("envIds")
        envIds: List<String>
    ): Result<List<EnvWithPermission>>

    @Operation(summary = "分页获取环境列表(名称模糊匹配)")
    @GET
    @Path("/projects/{projectId}/searchByName/")
    fun searchByName(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @Parameter(description = "步长", required = false)
        @QueryParam("limit")
        limit: Int? = null,
        @Parameter(description = "环境名称", required = true)
        @QueryParam("envName")
        envName: String
    ): Result<Page<EnvWithPermission>>
}
