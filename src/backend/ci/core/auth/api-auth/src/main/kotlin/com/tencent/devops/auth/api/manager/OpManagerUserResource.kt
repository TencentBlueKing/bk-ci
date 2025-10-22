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

package com.tencent.devops.auth.api.manager

import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.auth.pojo.WhiteEntify
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.auth.pojo.enum.UrlType
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_MANAGER_USER", description = "权限-管理员")
@Path("/op/auth/manager/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpManagerUserResource {

    @POST
    @Path("/")
    @Operation(summary = "新增管理员到组织")
    fun createManagerUser(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        managerUserDTO: ManagerUserDTO
    ): Result<String>

    @POST
    @Path("/batch/create")
    @Operation(summary = "批量新增管理员到组织")
    fun batchCreateManagerUser(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "目标用户,支持以“,”隔开", required = true)
        @QueryParam("managerUserId")
        managerUserId: String,
        @Parameter(description = "timeout", required = true)
        @QueryParam("timeout")
        timeout: Int,
        @Parameter(description = "授权Id,支持以“,”隔开", required = true)
        @QueryParam("managerIds")
        managerIds: String
    ): Result<Boolean>

    @DELETE
    @Path("/managers/{managerId}")
    @Operation(summary = "删除管理员")
    fun deleteManagerUser(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权Id", required = true)
        @PathParam("managerId")
        managerId: Int,
        @Parameter(description = "待回收用户", required = true)
        @QueryParam("deleteUser")
        deleteUser: String
    ): Result<Boolean>

    @DELETE
    @Path("/batch/delete")
    @Operation(summary = "批量删除管理员")
    fun batchDeleteManagerUser(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权Id,支持以“,”隔开", required = true)
        @QueryParam("managerIds")
        managerIds: String,
        @Parameter(description = "待回收用户,支持以“,”隔开", required = true)
        @QueryParam("deleteUsers")
        deleteUsers: String
    ): Result<Boolean>

    @GET
    @Path("/managers/{managerId}/alive/list")
    @Operation(summary = "有效期内管理员列表")
    fun managerAliveUserList(
        @Parameter(description = "授权Id", required = true)
        @PathParam("managerId")
        managerId: Int
    ): Result<List<ManagerUserEntity>?>

    @GET
    @Path("/managers/{managerId}/history/list")
    @Operation(summary = "已超时管理员列表")
    fun managerHistoryUserList(
        @Parameter(description = "授权Id", required = true)
        @PathParam("managerId")
        managerId: Int,
        @Parameter(description = "页数", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "页大小", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ManagerUserEntity>?>

    @GET
    @Path("/{userId}")
    @Operation(summary = "用户管理员信息,并刷新内存信息")
    fun getManagerInfo(
        @Parameter(description = "用户Id", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Map<String/*organizationId*/, UserPermissionInfo>?>

    @POST
    @Path("/white")
    @Operation(summary = "添加管理授权白名单用户")
    fun createWhiteUser(
        @Parameter(description = "授权Id", required = true)
        @QueryParam("managerId")
        managerId: Int,
        @Parameter(description = "用户Id串, 支持以“,”分割", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @DELETE
    @Path("/white")
    @Operation(summary = "删除管理授权白名单用户")
    fun deleteWhiteUser(
        @Parameter(description = "白名单Id, 支持以“,”分割", required = true)
        @QueryParam("ids")
        ids: String
    ): Result<Boolean>

    @GET
    @Path("/white/managerIds/{managerId}/list/")
    @Operation(summary = "获取管理员策略下白名单列表")
    fun listWhiteUser(
        @Parameter(description = "管理员策略Id", required = true)
        @PathParam("managerId")
        managerId: Int
    ): Result<List<WhiteEntify>?>

    @GET
    @Path("/manager/url/types/{type}/managerIds/{managerId}")
    @Operation(summary = "获取授权/取消授权链接")
    fun getUrl(
        @Parameter(description = "获取链接类型: 授权链接, 取消授权链接", required = true)
        @PathParam("type")
        type: UrlType,
        @Parameter(description = "管理员策略Id", required = true)
        @PathParam("managerId")
        managerId: Int
    ): Result<String>
}
