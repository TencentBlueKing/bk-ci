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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.SharedProjectInfo
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.enums.EnvType
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

@Tag(name = "USER_ENVIRONMENT", description = "用户-环境服务")
@Path("/user/environment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserEnvironmentResource {
    @Operation(summary = "是否拥有创建环境的权限")
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

    @Operation(summary = "创建环境")
    @POST
    @Path("/{projectId}")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境信息", required = true)
        environment: EnvCreateInfo
    ): Result<EnvironmentId>

    @Operation(summary = "修改环境")
    @POST
    @Path("/{projectId}/{envHashId}")
    fun update(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "环境修改信息", required = true)
        environment: EnvUpdateInfo
    ): Result<Boolean>

    @Operation(summary = "获取环境列表")
    @GET
    @Path("/{projectId}")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境名称", required = true)
        @QueryParam("envName")
        envName: String?,
        @Parameter(description = "环境类型", required = true)
        @QueryParam("envType")
        envType: EnvType?,
        @Parameter(description = "节点", required = true)
        @QueryParam("nodeHashId")
        nodeHashId: String?
    ): Result<List<EnvWithPermission>>

    @Operation(summary = "根据类型获取环境列表")
    @GET
    @Path("/{projectId}/types/{envType}")
    fun listByType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境类型", required = true)
        @PathParam("envType")
        envType: EnvType
    ): Result<List<EnvWithNodeCount>>

    @Operation(summary = "根据OS获取第三方构建环境列表")
    @GET
    @Path("/{projectId}/buildEnvs")
    fun listBuildEnvs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @QueryParam("os")
        os: OS
    ): Result<List<EnvWithNodeCount>>

    @Operation(summary = "获取环境信息")
    @GET
    @Path("/{projectId}/{envHashId}")
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String
    ): Result<EnvWithPermission>

    @Operation(summary = "删除环境")
    @DELETE
    @Path("/{projectId}/{envHashId}")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取环境的节点列表")
    @POST
    @Path("/{projectId}/{envHashId}/listNodes")
    fun listNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "获取环境的节点列表")
    @GET
    @Path("/{projectId}/{envHashId}/listNodesNew")
    fun listNodesNew(
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
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String
    ): Result<Page<NodeBaseInfo>>

    @Operation(summary = "添加节点到环境")
    @POST
    @Path("/{projectId}/{envHashId}/addNodes")
    fun addNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "从环境删除节点")
    @POST
    @Path("/{projectId}/{envHashId}/deleteNodes")
    fun deleteNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "获取用户有权限使用的环境列表")
    @GET
    @Path("/{projectId}/listUsableServerEnvs")
    fun listUsableServerEnvs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<EnvWithPermission>>

    @Operation(summary = "获取用户有权限且没添加进环境共享列表的ProjectId")
    @GET
    @Path("/{projectId}/{envHashId}/list_user_project")
    fun listUserShareEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "关键字搜索", required = false)
        @QueryParam("search")
        search: String? = null,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "步长", required = false)
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<SharedProjectInfo>>

    @Operation(summary = "分页获取环境共享列表")
    @GET
    @Path("/{projectId}/{envHashId}/list")
    fun listShareEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "项目名称", required = false)
        @QueryParam("name")
        name: String? = null,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "步长", required = false)
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<SharedProjectInfo>>

    @Operation(summary = "设置环境共享")
    @POST
    @Path("/{projectId}/{envHashId}/share")
    fun setShareEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "共享的项目列表", required = true)
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean>

    @Operation(summary = "按环境删除环境共享")
    @DELETE
    @Path("/{projectId}/{envHashId}/share")
    fun deleteShareEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String
    ): Result<Boolean>

    @Operation(summary = "按项目删除环境共享")
    @DELETE
    @Path("/{projectId}/{envHashId}/{sharedProjectId}/sharedProject")
    fun deleteShareEnvBySharedProj(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "共享的项目id", required = true)
        @PathParam("sharedProjectId")
        sharedProjectId: String
    ): Result<Boolean>

    @Operation(summary = "停用或者启用节点")
    @PUT
    @Path("/{projectId}/{envHashId}/enableNode/{nodeHashId}")
    fun enableNodeEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @PathParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 hashId", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "启动或者停用", required = true)
        @QueryParam("enableNode")
        enableNode: Boolean
    ): Result<Boolean>
}
