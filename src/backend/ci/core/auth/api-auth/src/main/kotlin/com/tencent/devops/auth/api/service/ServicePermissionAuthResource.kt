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

import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_GIT_TYPE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
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

@Api(tags = ["AUTH_SERVICE_PERMISSION"], description = "权限--权限校验以及资源操作相关接口")
@Path("/open/service/auth/permission")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface ServicePermissionAuthResource {

    @GET
    @Path("/projects/{projectCode}/action/validate")
    @ApiOperation("校验用户是否有具体操作的权限")
    fun validateUserActionPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("资源类型", required = true)
        action: String
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/resource/validate")
    @ApiOperation("校验用户是否有具体资源的操作权限")
    fun validateUserResourcePermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("资源类型", required = true)
        action: String,
        @QueryParam("projectCode")
        @ApiParam("项目编码", required = true)
        projectCode: String,
        // 此处resourceCode实际为resourceType
        @QueryParam("resourceCode")
        @ApiParam("资源类型", required = false)
        resourceCode: String?
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/relation/validate")
    @ApiOperation("校验用户是否有具体资源实例的操作权限")
    fun validateUserResourcePermissionByRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型", required = true)
        action: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceCode")
        @ApiParam("资源code", required = true)
        resourceCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型", required = true)
        resourceType: String,
        @QueryParam("relationResourceType")
        @ApiParam("关联资源,一般为Project", required = false)
        relationResourceType: String? = null
    ): Result<Boolean>

    @POST
    @Path("/projects/{projectCode}/instance/validate")
    @ApiOperation("校验用户是否有具体资源实例的操作权限")
    fun validateUserResourcePermissionByInstance(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型", required = true)
        action: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        resource: AuthResourceInstance
    ): Result<Boolean>

    @POST
    @Path("/projects/{projectCode}/relation/validate/batch")
    @ApiOperation("批量校验用户是否有具体资源实例的操作权限")
    fun batchValidateUserResourcePermissionByRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceCode")
        @ApiParam("资源code", required = true)
        resourceCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型", required = true)
        resourceType: String,
        @QueryParam("relationResourceType")
        @ApiParam("关联资源,一般为Project", required = false)
        relationResourceType: String? = null,
        @ApiParam("action类型列表", required = true)
        action: List<String>
    ): Result<Boolean>

    @GET
    @Path("/projects/{projectCode}/action/instanceAndParent")
    @ApiOperation("获取用户所拥有指定权限下的指定类型资源和类型父资源code列表")
    fun getUserResourceAndParentByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型")
        action: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String
    ): Result<Map<String, List<String>>>

    @GET
    @Path("/projects/{projectCode}/actions/instance/map")
    @ApiOperation("获取用户某项目下多操作的资源实例列表")
    fun getUserResourcesByPermissions(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型")
        action: List<String>,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String
    ): Result<Map<AuthPermission, List<String>>>

    @GET
    @Path("/projects/{projectCode}/action/instance")
    @ApiOperation("获取用户某项目下指定操作的资源实例列表")
    fun getUserResourceByPermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型")
        action: String,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String
    ): Result<List<String>>

    @POST
    @Path("/projects/{projectCode}/actions/instance/filter")
    @ApiOperation("过滤用户某项目下多操作的资源实例列表")
    fun filterUserResourcesByPermissions(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @QueryParam("action")
        @ApiParam("action类型")
        actions: List<String>,
        @PathParam("projectCode")
        @ApiParam("项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Result<Map<AuthPermission, List<String>>>

    @Path("/projects/{projectCode}/create/relation")
    @POST
    @ApiOperation("创建权限中心资源")
    fun resourceCreateRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Id")
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源Code")
        resourceCode: String,
        @QueryParam("resourceName")
        @ApiParam("资源名称")
        resourceName: String
    ): Result<Boolean>

    @Path("/projects/{projectCode}/modify/relation")
    @PUT
    @ApiOperation("修改权限中心资源")
    fun resourceModifyRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Id")
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源Code")
        resourceCode: String,
        @QueryParam("resourceName")
        @ApiParam("资源名称")
        resourceName: String
    ): Result<Boolean>

    @Path("/projects/{projectCode}/delete/relation")
    @DELETE
    @ApiOperation("删除权限中心资源")
    fun resourceDeleteRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Id")
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源Code")
        resourceCode: String
    ): Result<Boolean>

    @Path("/projects/{projectCode}/cancel/relation")
    @PUT
    @ApiOperation("取消权限中心资源")
    fun resourceCancelRelation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("操作用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @HeaderParam(AUTH_HEADER_GIT_TYPE)
        @ApiParam("系统类型")
        type: String? = null,
        @PathParam("projectCode")
        @ApiParam("项目Id")
        projectCode: String,
        @QueryParam("resourceType")
        @ApiParam("资源类型")
        resourceType: String,
        @QueryParam("resourceCode")
        @ApiParam("资源Code")
        resourceCode: String
    ): Result<Boolean>

    @Path("/projects/{projectCode}/grant")
    @POST
    @ApiOperation("授权实例级别权限")
    fun grantInstancePermission(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("操作用户ID", required = true)
        userId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @ApiParam("认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @ApiParam("项目Id")
        projectCode: String,
        grantInstance: GrantInstanceDTO
    ): Result<Boolean>
}
