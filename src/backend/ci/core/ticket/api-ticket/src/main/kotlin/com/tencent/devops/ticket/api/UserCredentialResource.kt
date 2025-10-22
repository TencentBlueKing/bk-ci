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

package com.tencent.devops.ticket.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialSettingUpdate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.Permission
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

@Tag(name = "USER_CREDENTIAL", description = "用户-凭据资源")
@Path("/user/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserCredentialResource {
    @Operation(summary = "是否拥有创建凭据权限")
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

    @Operation(summary = "新增凭据")
    @Path("/{projectId}/")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据", required = true)
        credential: CredentialCreate
    ): Result<Boolean>

    @Operation(summary = "删除凭据")
    @Path("/{projectId}/{credentialId}")
    @DELETE
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String

    ): Result<Boolean>

    @Operation(summary = "获取凭据列表")
    @Path("/{projectId}/")
    @GET
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭证类型列表，用逗号分隔", required = false)
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): Result<Page<CredentialWithPermission>>

    @Operation(summary = "获取拥有对应权限凭据列表")
    @Path("/{projectId}/hasPermissionList")
    @GET
    fun hasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭证类型列表，用逗号分隔", required = false, example = "")
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: Permission,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): Result<Page<Credential>>

    @Operation(summary = "获取拥有对应权限凭据列表-不分页-插件UI调用")
    @Path("/{projectId}/getHasPermissionList")
    @GET
    fun getHasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭证类型列表，用逗号分隔", required = false, example = "")
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: Permission,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): Result<List<Credential>>

    @Operation(summary = "显示真实凭据")
    @Path("/{projectId}/{credentialId}/show")
    @GET
    fun show(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<CredentialWithPermission>

    @Operation(summary = "获取凭据以编辑")
    @Path("/{projectId}/{credentialId}/")
    @GET
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<CredentialWithPermission>

    @Operation(summary = "编辑凭据")
    @Path("/{projectId}/{credentialId}/")
    @PUT
    fun edit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @Parameter(description = "凭据", required = true)
        credential: CredentialUpdate
    ): Result<Boolean>

    @Operation(summary = "编辑凭据设置凭据")
    @Path("/{projectId}/{credentialId}/setting")
    @PUT
    fun editSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @Parameter(description = "凭据设置", required = true)
        credentialSetting: CredentialSettingUpdate
    ): Result<Boolean>
}
