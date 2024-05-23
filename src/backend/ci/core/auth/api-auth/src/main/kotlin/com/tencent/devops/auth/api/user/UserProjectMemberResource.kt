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

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerGroupMemberVo
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_PROJECT_MEMBER", description = "用户组—用户")
@Path("/user/project/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectMemberResource {
    @POST
    @Path("/projectIds/{projectId}/roleIds/{roleId}")
    @Operation(summary = "项目下添加指定组组员")
    fun createRoleMember(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "角色Id", required = true)
        @PathParam("roleId")
        roleId: Int,
        @Parameter(description = "是否为管理员分组", required = true)
        @QueryParam("managerGroup")
        managerGroup: Boolean,
        @Parameter(description = "添加用户集合", required = true)
        members: List<RoleMemberDTO>
    ): Result<Boolean>

    @GET
    @Path("/projectIds/{projectId}/roleIds/{roleId}")
    @Operation(summary = "查询项目下指定用户组用户")
    fun getRoleMember(
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "角色Id", required = true)
        @PathParam("roleId")
        roleId: Int,
        @Parameter(description = "页数", required = true)
        @QueryParam("path")
        page: Int?,
        @Parameter(description = "页面大小", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ManagerGroupMemberVo>

    @GET
    @Path("projectIds/{projectId}/members/all")
    @Operation(summary = "获取项目下所有用户")
    fun getProjectAllMember(
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "页数", required = true)
        @QueryParam("path")
        page: Int?,
        @Parameter(description = "页面大小", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ProjectMembersVO?>

    @DELETE
    @Path("/projectIds/{projectId}/roleIds/{roleId}")
    @Operation(summary = "删除项目下指定用户组用户")
    fun deleteRoleMember(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "角色Id", required = true)
        @PathParam("roleId")
        roleId: Int,
        @Parameter(description = "是否为管理员分组", required = true)
        @QueryParam("managerGroup")
        managerGroup: Boolean,
        @Parameter(description = "待删除用户或组织Id", required = true)
        @QueryParam("id")
        members: String,
        @Parameter(description = "组员类型 user:单用户, dept:组织", required = true)
        @QueryParam("type")
        type: ManagerScopesEnum
    ): Result<Boolean>

    @GET
    @Path("projectIds/{projectId}/user/groups")
    @Operation(summary = "获取指定用户指定项目下的用户组")
    fun getUserAllGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目标识", required = true)
        @PathParam("projectId")
        projectId: Int,
        @Parameter(description = "待搜用户", required = true)
        searchUserId: String
    ): Result<List<ManagerRoleGroupInfo>?>

    @GET
    @Path("/projectIds/{projectId}/checkManager")
    @Operation(summary = "判断是否是项目管理员或CI管理员")
    fun checkManager(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目Id", required = true)
        projectId: String
    ): Result<Boolean>
}
