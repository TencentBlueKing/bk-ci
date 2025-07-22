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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVerGroupReferenceDO
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupYamlStringPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
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
import jakarta.ws.rs.core.Response

@Tag(name = "USER_PUBLIC_VAR_GROUP", description = "用户-公共变量组")
@Path("/user/pipeline/public/var/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPublicVarGroupResource {

    @Operation(summary = "添加公共变量组")
    @POST
    @Path("/projects/{projectId}/add")
    fun addGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "公共变量组请求报文", required = true)
        publicVarGroup: PublicVarGroupVO
    ): Result<Boolean>

    @Operation(summary = "查询公共变量组列表")
    @POST
    @Path("/projects/{projectId}/list")
    fun getGroups(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Page<PublicVarGroupDO>>

    @Operation(summary = "编辑变量组")
    @PUT
    @Path("/{groupId}")
    fun updateGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "变量组更新数据", required = true)
        group: PublicVarGroupVO
    ): Result<Boolean>

    @Operation(summary = "导入公共变量组(YAML格式)")
    @POST
    @Path("/projects/{projectId}/import")
    fun importGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "YAML文件", required = true)
        yaml: PublicVarGroupYamlStringPO
    ): Result<String>

    @Operation(summary = "导出公共变量组(YAML格式)")
    @GET
    @Path("/{groupId}/export")
    fun exportGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String
    ): Response

    @Operation(summary = "发布变量组")
    @POST
    @Path("/{groupName}/publish")
    fun publishGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "版本描述", required = true)
        @QueryParam("versionDesc")
        versionDesc: String
    ): Result<Boolean>

    @Operation(summary = "删除变量组")
    @DELETE
    @Path("/{groupName}")
    fun deleteGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String
    ): Result<Boolean>

    @Operation(summary = "获取引用变量组的流水线/模板列表")
    @GET
    @Path("/{groupId}/references")
    fun getReferences(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量组名称", required = true)
        @PathParam("groupName")
        groupName: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PublicVerGroupReferenceDO>>
}
