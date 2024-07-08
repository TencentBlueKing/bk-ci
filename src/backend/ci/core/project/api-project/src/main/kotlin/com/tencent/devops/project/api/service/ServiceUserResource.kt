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

package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_USER", description = "用户信息接口")
@Path("/service/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceUserResource {
    @GET
    @Path("/cachedDetail")
    @Operation(summary = "从缓存中查询用户详细信息")
    fun getDetailFromCache(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<UserDeptDetail>

    @GET
    @Path("/projects/{projectCode}/roles")
    @Operation(summary = "获取项目指定角色用户")
    fun getProjectUserRoles(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "角色Id", required = true)
        @QueryParam("roleId")
        roleId: BkAuthGroup
    ): Result<List<String>>

    @POST
    @Path("/cachedDetail/list")
    @Operation(summary = "从缓存中查询用户详细信息列表")
    fun listDetailFromCache(
        @Parameter(description = "用户ID列表", required = true)
        userIds: List<String>
    ): Result<List<UserDeptDetail>>

    @GET
    @Path("/parentIds/{parentId}/usernames")
    @Operation(summary = "根据父节点ID获取用户列表")
    fun usernamesByParentId(
        @Parameter(description = "父节点ID", required = true)
        @PathParam("parentId")
        parentId: Int
    ): Result<List<String>>
}
