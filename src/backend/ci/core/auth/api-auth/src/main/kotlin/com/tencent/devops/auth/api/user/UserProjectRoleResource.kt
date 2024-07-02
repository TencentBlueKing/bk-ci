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
 *
 */

package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.DefaultGroup
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.pojo.vo.GroupInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "USER_PROJECT_ROLE", description = "项目-用户组")
@Path("/user/project/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectRoleResource {
    @POST
    @Path("/projectIds/{projectId}/")
    @Operation(summary = "项目下添加指定组")
    fun createProjectRole(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "项目标识", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户组信息", required = true)
        groupInfo: ProjectRoleDTO
    ): Result<String>

    @PUT
    @Path("/projectIds/{projectId}/roleIds/{roleId}")
    @Operation(summary = "用户组重命名")
    fun updateProjectRole(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "角色Id", required = true)
        @PathParam("roleId")
        roleId: Int,
        @Parameter(description = "用户组信息", required = true)
        groupInfo: ProjectRoleDTO
    ): Result<Boolean>

    @GET
    @Path("/projectIds/{projectId}")
    @Operation(summary = "获取用户组")
    fun getProjectRoles(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int
    ): Result<List<GroupInfoVo>>

    @DELETE
    @Path("/projectIds/{projectId}/roles/{roleId}")
    @Operation(summary = "删除用户组")
    fun deleteProjectRole(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "角色Id", required = true)
        @PathParam("roleId")
        roleId: Int
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectId}/manager/hasPermission")
    @Operation(summary = "是否有项目管理操作的权限")
    fun hashPermission(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int
    ): Result<Boolean>

    @GET
    @Path("/default/role")
    fun getDefaultRole(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<DefaultGroup>>
}
