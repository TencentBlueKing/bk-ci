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

package com.tencent.devops.auth.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_GIT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_SERVICE_PROJECT"], description = "权限--项目相关接口")
@Path("/open/service/auth/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectAuthResource {

    @GET
    @Path("/{projectCode}/users/byGroup")
    @ApiOperation("获取项目成员 (需要对接的权限中心支持该功能才可以)")
    fun getProjectUsers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("group")
        @ApiParam("用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users")
    @ApiOperation("拉取项目所有成员，并按项目角色组分组成员信息返回")
    fun getProjectGroupAndUserList(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String
    ): Result<List<BkAuthGroupAndUserList>>

    @GET
    @Path("/users/{userId}")
    @ApiOperation("获取用户有管理权限的项目Code")
    fun getUserProjects(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("userId")
        @ApiParam("用户userId", required = true)
        userId: String
    ): Result<List<String>>

    @GET
    @Path("/users/{userId}/{action}")
    @ApiOperation("获取用户有某种项目资源类型权限的项目Code")
    fun getUserProjectsByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("userId")
        @ApiParam("用户userId", required = true)
        userId: String,
        @PathParam("action")
        @ApiParam("项目资源类型action", required = true)
        action: String
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/users/{userId}/isProjectUsers")
    @ApiOperation("判断是否某个项目中某个组角色的成员")
    fun isProjectUser(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("userId")
        @ApiParam("用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("group")
        @ApiParam("用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/users/{userId}/checkProjectManager")
    @ApiOperation("判断是否是项目管理员")
    fun checkProjectManager(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("userId")
        @ApiParam("用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String
    ): Result<Boolean>

    @GET
    @Path("/projectIds/{projectId}/checkManager")
    @ApiOperation("判断是否是项目管理员或CI管理员")
    fun checkManager(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @ApiParam("项目Id", required = true)
        projectId: String
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/createUser")
    @ApiOperation("添加用户到指定项目指定分组")
    fun createProjectUser(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @QueryParam("userId")
        @ApiParam("用户Id", required = true)
        userId: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("roleCode")
        @ApiParam("用户组Code", required = true)
        roleCode: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/roles")
    @ApiOperation("获取项目角色")
    fun getProjectRoles(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("projectId")
        @ApiParam("项目Id", required = true)
        projectId: String
    ): Result<List<BKAuthProjectRolesResources>>
}
