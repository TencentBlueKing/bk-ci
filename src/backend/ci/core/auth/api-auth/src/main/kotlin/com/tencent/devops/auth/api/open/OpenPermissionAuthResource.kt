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

package com.tencent.devops.auth.api.open

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_GIT_TYPE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
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

@Tag(name = "AUTH_OPEN_PERMISSION", description = "权限--权限校验以及资源操作相关接口")
@Path("/open/service/auth/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface OpenPermissionAuthResource {

    @GET
    @Path("/projects/{projectCode}/action/validate")
    @Operation(summary = "校验用户是否有具体操作的权限")
    fun validateUserActionPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "资源类型", required = true)
        action: String
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/resource/validate")
    @Operation(summary = "校验用户是否有具体资源的操作权限")
    fun validateUserResourcePermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "资源类型", required = true)
        action: String,
        @QueryParam("projectCode")
        @Parameter(description = "项目编码", required = true)
        projectCode: String,
        // 此处resourceCode实际为resourceType
        @QueryParam("resourceCode")
        @Parameter(description = "资源类型", required = false)
        resourceCode: String?
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/relation/validate")
    @Operation(summary = "校验用户是否有具体资源实例的操作权限")
    fun validateUserResourcePermissionByRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型", required = true)
        action: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = true)
        resourceCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("relationResourceType")
        @Parameter(description = "关联资源,一般为Project", required = false)
        relationResourceType: String? = null
    ): Result<Boolean>

    @POST
    @Path("/projects/{projectCode}/instance/validate")
    @Operation(summary = "校验用户是否有具体资源实例的操作权限")
    fun validateUserResourcePermissionByInstance(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型", required = true)
        action: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        resource: AuthResourceInstance
    ): Result<Boolean>

    @POST
    @Path("/projects/{projectCode}/relation/validate/batch")
    @Operation(summary = "批量校验用户是否有具体资源实例的操作权限")
    fun batchValidateUserResourcePermissionByRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = true)
        resourceCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = true)
        resourceType: String,
        @QueryParam("relationResourceType")
        @Parameter(description = "关联资源,一般为Project", required = false)
        relationResourceType: String? = null,
        @Parameter(description = "action类型列表", required = true)
        action: List<String>
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/action/instanceAndParent")
    @Operation(summary = "获取用户所拥有指定权限下的指定类型资源和类型父资源code列表")
    fun getUserResourceAndParentByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型")
        action: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型")
        resourceType: String
    ): Result<Map<String, List<String>>>

    @GET
    @Path("/projects/{projectCode}/actions/instance/map")
    @Operation(summary = "获取用户某项目下多操作的资源实例列表")
    fun getUserResourcesByPermissions(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型")
        action: List<String>,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型")
        resourceType: String
    ): Result<Map<AuthPermission, List<String>>>

    @GET
    @Path("/projects/{projectCode}/action/instance")
    @Operation(summary = "获取用户某项目下指定操作的资源实例列表")
    fun getUserResourceByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型")
        action: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型")
        resourceType: String
    ): Result<List<String>>

    @POST
    @Path("/projects/{projectCode}/actions/instance/filter")
    @Operation(summary = "过滤用户某项目下多操作的资源实例列表")
    fun filterUserResourcesByPermissions(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @QueryParam("action")
        @Parameter(description = "action类型")
        actions: List<String>,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型")
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Result<Map<AuthPermission, List<String>>>

    @Path("/projects/{projectCode}/create/relation")
    @POST
    @Operation(summary = "创建权限中心资源")
    fun resourceCreateRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @Parameter(description = "系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @Parameter(description = "项目Id")
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型")
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源Code")
        resourceCode: String,
        @QueryParam("resourceName")
        @Parameter(description = "资源名称")
        resourceName: String
    ): Result<Boolean>
}
