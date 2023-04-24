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
 *
 */

package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_RESOURCE"], description = "用户态-iam资源映射")
@Path("/user/auth/resource/{projectId}/{resourceType}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthResourceResource {

    @GET
    @Path("{resourceCode}/hasManagerPermission")
    @ApiOperation("是否有资源管理操作的权限")
    fun hasManagerPermission(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<Boolean>

    @GET
    @Path("{resourceCode}/isEnablePermission")
    @ApiOperation("是否启用权限管理")
    fun isEnablePermission(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<Boolean>

    @GET
    @Path("{resourceCode}/listGroup")
    @ApiOperation("获取用户组列表")
    @Suppress("LongParameterList")
    fun listGroup(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String,
        @ApiParam("第几页")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页多少条")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Pagination<IamGroupInfoVo>>

    @GET
    @Path("{resourceCode}/groupMember")
    @ApiOperation("获取用户所属组")
    fun listUserBelongGroup(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<List<IamGroupMemberInfoVo>>

    @PUT
    @Path("{resourceCode}/enable")
    @ApiOperation("开启权限管理")
    fun enable(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<Boolean>

    @PUT
    @Path("{resourceCode}/disable")
    @ApiOperation("关闭权限管理")
    fun disable(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<Boolean>

    @GET
    @Path("listResources")
    @ApiOperation("获取资源列表")
    @SuppressWarnings("LongParameterList")
    fun listResources(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源名称")
        @QueryParam("resourceName")
        resourceName: String?,
        @ApiParam("第几页")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页多少条")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Pagination<AuthResourceInfo>>

    @GET
    @Path("{resourceCode}/getResource")
    @ApiOperation("获取资源实例信息")
    fun getResource(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("资源类型")
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("资源ID")
        @PathParam("resourceCode")
        resourceCode: String
    ): Result<AuthResourceInfo>
}
