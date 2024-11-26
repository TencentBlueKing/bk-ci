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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.DisplayName
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_NODE", description = "用户-节点")
@Path("/user/envnode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserNodeResource {

    @Operation(summary = "是否拥有创建节点的权限")
    @Path("/{projectId}/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "删除节点")
    @POST
    @Path("/{projectId}/deleteNodes")
    fun deleteNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点列表", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "获取项目节点列表")
    @GET
    @Path("/{projectId}")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "获取项目节点列表")
    @GET
    @Path("/{projectId}/listNew")
    fun listNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int? = 1,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int? = 20,
        @Parameter(description = "IP", required = false)
        @QueryParam("nodeIp")
        nodeIp: String?,
        @Parameter(description = "别名", required = false)
        @QueryParam("displayName")
        displayName: String?,
        @Parameter(description = "创建人", required = false)
        @QueryParam("createdUser")
        createdUser: String?,
        @Parameter(description = "最后修改人", required = false)
        @QueryParam("lastModifiedUser")
        lastModifiedUser: String?,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keywords")
        keywords: String?,
        @Parameter(description = "节点类型", required = false)
        @QueryParam("nodeType")
        nodeType: NodeType?
    ): Result<Page<NodeWithPermission>>

    @Operation(summary = "获取用户有权限使用的服务器列表")
    @GET
    @Path("/{projectId}/listUsableServerNodes")
    fun listUsableServerNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "修改节点导入人")
    @POST
    @Path("/{projectId}/{nodeHashId}/changeCreatedUser")
    fun changeCreatedUser(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 HashId", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @Operation(summary = "修改节点名称")
    @POST
    @Path("/{projectId}/{nodeHashId}/updateDisplayName")
    fun updateDisplayName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 HashId", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "显示名称", required = true)
        displayName: DisplayName
    ): Result<Boolean>
}
