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

package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
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

@Api(tags = ["OP_REMOTE_DEV"], description = "OP-REMOTE-DEV")
@Path("/op")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRemoteDevResource {

    @ApiOperation("新增工作空间模板")
    @POST
    @Path("/wstemplate/add")
    fun addWorkspaceTemplate(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "模板信息", required = true)
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean>

    @ApiOperation("获取工作空间模板")
    @GET
    @Path("/wstemplate/list")
    fun getWorkspaceTemplateList(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WorkspaceTemplate>>

    @ApiOperation("更新工作空间模板")
    @PUT
    @Path("/wstemplate/update/template/{wsTemplateId}")
    fun updateWorkspaceTemplate(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "模板ID", required = true)
        @PathParam("wsTemplateId")
        workspaceTemplateId: Long,
        @ApiParam(value = "模板信息", required = true)
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean>

    @ApiOperation("删除工作空间模板")
    @DELETE
    @Path("/wstemplate/delete/template/{wsTemplateId}")
    fun deleteWorkspaceTemplate(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "模板信息", required = true)
        @PathParam("wsTemplateId")
        wsTemplateId: Long
    ): Result<Boolean>

    @ApiOperation("计费刷新")
    @POST
    @Path("/init_billing")
    fun initBilling(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "freeTime (单位分钟)", required = true)
        @QueryParam("freeTime")
        freeTime: Int
    ): Result<Boolean>

    @ApiOperation("更新用户级别设置")
    @POST
    @Path("/user_setting")
    fun updateUserSetting(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: List<OPUserSetting>
    ): Result<Boolean>

    @ApiOperation("获取用户设置")
    @POST
    @Path("/get_user_setting")
    fun getUserSetting(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<RemoteDevUserSettings>

    @ApiOperation("更新用户组织架构")
    @POST
    @Path("/refresh/all")
    fun refreshUserInfo(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @ApiOperation("添加客户端白名单用户")
    @POST
    @Path("/whiteList/add")
    fun addWhiteListUser(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "需要添加的白名单用户，多个用;分隔", required = true)
        @QueryParam("whiteListUser")
        whiteListUser: String
    ): Result<Boolean>

    @ApiOperation("添加云桌面白名单用户")
    @POST
    @Path("/GPUWhiteList/add")
    fun addGPUWhiteListUser(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "需要添加的白名单用户，多个用;分隔", required = true)
        @QueryParam("whiteListUser")
        whiteListUser: String
    ): Result<Boolean>

    @ApiOperation("新增镜像配置")
    @POST
    @Path("/image/spec")
    fun addImageSpec(
        spec: ImageSpec
    ): Result<Boolean>

    @ApiOperation("删除镜像配置")
    @DELETE
    @Path("/image/spec")
    fun deleteImageSpec(
        @QueryParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("修改镜像配置")
    @PUT
    @Path("/image/spec")
    fun updateImageSpec(
        @QueryParam("id")
        id: Int,
        spec: ImageSpec
    ): Result<Boolean>

    @ApiOperation("镜像配置列表")
    @GET
    @Path("/image/spec")
    fun listImageSpec(): Result<List<ImageSpec>?>
}
