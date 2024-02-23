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

package com.tencent.devops.auth.api.user

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_DEPT", description = "组织架构")
@Path("/user/dept")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDeptResource {

    @GET
    @Path("/levels/{level}")
    @Operation(summary = "按组织级别获取组织列表")
    fun getDeptByLevel(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @PathParam("level")
        @Parameter(description = "组织级别", required = true)
        level: Int
    ): Result<DeptInfoVo?>

    @GET
    @Path("/parents/{parentId}")
    @Operation(summary = "按组织级别获取组织列表")
    fun getDeptByParent(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @PathParam("parentId")
        @Parameter(description = "父组织Id", required = true)
        parentId: Int,
        @QueryParam("pageSize")
        @Parameter(description = "父组织Id", required = false)
        pageSize: Int?
    ): Result<DeptInfoVo?>

    @GET
    @Path("/names/{name}")
    @Operation(summary = "按组织级别获取组织列表")
    fun getUserAndDeptByName(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @PathParam("name")
        @Parameter(description = "模糊搜索名称", required = true)
        name: String,
        @QueryParam("type")
        @Parameter(description = "搜索类型", required = true)
        type: ManagerScopesEnum
    ): Result<List<UserAndDeptInfoVo?>>

    @GET
    @Path("/{deptId}/users")
    fun getDeptUsers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "access_token")
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String?,
        @PathParam("deptId")
        @Parameter(description = "组织Id", required = true)
        deptId: Int
    ): Result<List<String>?>
}
