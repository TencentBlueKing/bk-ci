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
 *
 */

package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.vo.AuthProjectVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
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

@Tag(name = "USER_RESOURCE_AUTHORIZATION", description = "用户-权限-授权管理")
@Path("/user/auth/authorization/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthAuthorizationResource {

    @POST
    @Path("/{projectId}/listResourceAuthorization")
    @Operation(summary = "根据条件获取资源授权管理")
    fun listResourceAuthorization(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作渠道", required = true)
        @QueryParam("operateChannel")
        operateChannel: OperateChannel?,
        @Parameter(description = "查询条件", required = true)
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>>

    @GET
    @Path("/{projectId}/{resourceType}/getResourceAuthorization")
    @Operation(summary = "获取资源授权管理")
    fun getResourceAuthorization(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源code", required = true)
        @QueryParam("resourceCode")
        resourceCode: String
    ): Result<ResourceAuthorizationResponse>

    @GET
    @Path("/{projectId}/{resourceType}/checkAuthorizationWhenRemoveGroupMember")
    @Operation(summary = "当移出用户组时做授权检查")
    fun checkAuthorizationWhenRemoveGroupMember(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源code", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @Parameter(description = "成员ID", required = true)
        @QueryParam("memberId")
        memberId: String
    ): Result<Boolean>

    @POST
    @Path("/{projectId}/resetResourceAuthorization")
    @Operation(summary = "重置资源授权管理")
    fun resetResourceAuthorization(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源授权交接条件实体", required = true)
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>>

    @POST
    @Path("/{projectId}/resetAllResourceAuthorization")
    @Operation(summary = "重置资源授权管理")
    fun resetAllResourceAuthorization(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源授权交接条件实体", required = true)
        condition: ResetAllResourceAuthorizationReq
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("/listUserProjectsWithAuthorization")
    @Operation(summary = "获取用户授权相关的项目")
    fun listUserProjectsWithAuthorization(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String
    ): Result<List<AuthProjectVO>>
}
