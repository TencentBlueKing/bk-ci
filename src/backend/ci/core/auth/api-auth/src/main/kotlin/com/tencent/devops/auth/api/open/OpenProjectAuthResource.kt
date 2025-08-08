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

package com.tencent.devops.auth.api.open

import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_GIT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_SERVICE_PROJECT", description = "权限--项目相关接口")
@Path("/open/service/auth/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenProjectAuthResource {
    @GET
    @Path("/{projectCode}/users/byGroup")
    @Operation(summary = "获取项目成员 (需要对接的权限中心支持该功能才可以)")
    fun getProjectUsers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("group")
        @Parameter(description = "用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users")
    @Operation(summary = "拉取项目所有成员，并按项目角色组分组成员信息返回")
    fun getProjectGroupAndUserList(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String
    ): Result<List<BkAuthGroupAndUserList>>

    @GET
    @Path("/users/{userId}")
    @Operation(summary = "获取用户有管理权限的项目Code")
    fun getUserProjects(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("userId")
        @Parameter(description = "用户userId", required = true)
        userId: String
    ): Result<List<String>>

    @GET
    @Path("/users/{userId}/{action}")
    @Operation(summary = "获取用户有某种项目资源类型权限的项目Code")
    fun getUserProjectsByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("userId")
        @Parameter(description = "用户userId", required = true)
        userId: String,
        @PathParam("action")
        @Parameter(description = "项目资源类型action", required = true)
        action: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users/{userId}/isProjectUsers")
    @Operation(summary = "判断是否某个项目中某个组角色的成员")
    fun isProjectUser(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @PathParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("group")
        @Parameter(description = "用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/users/{userId}/checkUserInProjectLevelGroup")
    @Operation(summary = "是否该用户在项目级别的组中")
    fun checkUserInProjectLevelGroup(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @PathParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/users/{userId}/checkProjectManager")
    @Operation(summary = "判断是否是项目管理员")
    fun checkProjectManager(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @PathParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String
    ): Result<Boolean>

    @GET
    @Path("/projectIds/{projectId}/checkManager")
    @Operation(summary = "判断是否是项目管理员或CI管理员")
    fun checkManager(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目Id", required = true)
        projectId: String
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/createUser")
    @Operation(summary = "添加单个用户到指定项目指定分组")
    fun createProjectUser(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @QueryParam("userId")
        @Parameter(description = "用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("roleCode")
        @Parameter(description = "用户组Code", required = true)
        roleCode: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/roles")
    @Operation(summary = "获取项目角色")
    fun getProjectRoles(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("projectId")
        @Parameter(description = "项目Id", required = true)
        projectId: String
    ): Result<List<BKAuthProjectRolesResources>>

    @GET
    @Path("/{projectCode}/getProjectPermissionInfo")
    @Operation(summary = "获取项目权限信息")
    fun getProjectPermissionInfo(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String
    ): Result<ProjectPermissionInfoVO>
}
