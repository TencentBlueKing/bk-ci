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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.NodeBaseInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_AUTH_NODE", description = "服务-节点-权限中心")
@Path("/service/node/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface RemoteNodeResource {

    @Operation(summary = "分页获取节点列表")
    @GET
    @Path("/projects/{projectId}/list")
    fun listNodeForAuth(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @Parameter(description = "步长", required = false)
        @QueryParam("limit")
        limit: Int? = null
    ): Result<Page<NodeBaseInfo>>

    @Operation(summary = "获取节点信息")
    @GET
    @Path("/infos")
    fun getNodeInfos(
        @Parameter(description = "节点Id串", required = true)
        @QueryParam("nodeIds")
        nodeIds: List<String>
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "分页获取节点列表(名称模糊匹配)")
    @GET
    @Path("/projects/{projectId}/searchByDisplayName/")
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
        @QueryParam("displayName")
        displayName: String
    ): Result<Page<NodeBaseInfo>>
}
