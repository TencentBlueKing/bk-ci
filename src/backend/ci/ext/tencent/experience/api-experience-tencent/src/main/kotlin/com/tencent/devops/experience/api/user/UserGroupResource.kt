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

package com.tencent.devops.experience.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.Group
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.enums.ProjectGroup
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_GROUP"], description = "版本体验-体验组列表")
@Path("/user/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGroupResource {
    @ApiOperation("获取体验组列表")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目(不传默认全部返回)", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("是否返回公开体验组", required = false, defaultValue = "false")
        @QueryParam("returnPublic")
        returnPublic: Boolean?
    ): Result<Page<GroupSummaryWithPermission>>

    @ApiOperation("获取项目用户组人员")
    @Path("/{projectId}/projectUsers")
    @GET
    fun getProjectUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组", required = false)
        @QueryParam("projectGroup")
        projectGroup: ProjectGroup?
    ): Result<List<String>>

    @ApiOperation("获取项目用户组信息和组所有人员")
    @Path("/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @ApiOperation("创建体验组")
    @Path("/{projectId}/")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验组", required = true)
        group: GroupCreate
    ): Result<Boolean>

    @ApiOperation("获取体验组")
    @Path("/{projectId}/{groupHashId}")
    @GET
    fun get(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<Group>

    @ApiOperation("获取体验组用户")
    @Path("/{projectId}/{groupHashId}/users")
    @GET
    fun getUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<GroupUsers>

    @ApiOperation("修改体验组")
    @Path("/{projectId}/{groupHashId}")
    @PUT
    fun edit(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String,
        @ApiParam("体验组", required = true)
        group: GroupUpdate
    ): Result<Boolean>

    @ApiOperation("删除体验组")
    @Path("/{projectId}/{groupHashId}")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<Boolean>
}
