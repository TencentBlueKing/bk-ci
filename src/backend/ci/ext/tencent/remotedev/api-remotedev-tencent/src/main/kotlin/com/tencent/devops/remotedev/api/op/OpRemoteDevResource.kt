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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.pojo.windows.WindowsPoolListFetchData
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_REMOTE_DEV", description = "OP-REMOTE-DEV")
@Path("/op")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRemoteDevResource {

    @Operation(summary = "新增工作空间模板")
    @POST
    @Path("/wstemplate/add")
    fun addWorkspaceTemplate(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean>

    @Operation(summary = "获取工作空间模板")
    @GET
    @Path("/wstemplate/list")
    fun getWorkspaceTemplateList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WorkspaceTemplate>>

    @Operation(summary = "更新工作空间模板")
    @PUT
    @Path("/wstemplate/update/template/{wsTemplateId}")
    fun updateWorkspaceTemplate(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("wsTemplateId")
        workspaceTemplateId: Long,
        @Parameter(description = "模板信息", required = true)
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean>

    @Operation(summary = "删除工作空间模板")
    @DELETE
    @Path("/wstemplate/delete/template/{wsTemplateId}")
    fun deleteWorkspaceTemplate(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板信息", required = true)
        @PathParam("wsTemplateId")
        wsTemplateId: Long
    ): Result<Boolean>

    @Operation(summary = "计费刷新")
    @POST
    @Path("/init_billing")
    fun initBilling(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "freeTime (单位分钟)", required = true)
        @QueryParam("freeTime")
        freeTime: Int
    ): Result<Boolean>

    @Operation(summary = "更新用户级别设置")
    @POST
    @Path("/user_setting")
    fun updateUserSetting(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: List<OPUserSetting>
    ): Result<Boolean>

    @Operation(summary = "续期体验时长")
    @POST
    @Path("/renewal_time")
    fun renewalExperienceDuration(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "续期时长", required = true)
        @QueryParam("renewalTime")
        renewalTime: Int
    ): Result<Boolean>

    @Operation(summary = "获取用户设置")
    @GET
    @Path("/get_user_setting")
    fun getUserSetting(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<RemoteDevUserSettings>

    @Operation(summary = "获取所有用户设置列表")
    @GET
    @Path("/get_all_user_settings")
    fun getAllUserSettings(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "指定查询的用户", required = false)
        @QueryParam("queryUser")
        queryUser: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "6666")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RemoteDevUserSettings>>

    @Operation(summary = "更新用户组织架构")
    @POST
    @Path("/refresh/all")
    fun refreshUserInfo(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @Operation(summary = "添加客户端白名单用户")
    @POST
    @Path("/whiteList/add")
    fun addWhiteListUser(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "需要添加的白名单用户，多个用;分隔", required = true)
        @QueryParam("whiteListUser")
        whiteListUser: String
    ): Result<Boolean>

    @Operation(summary = "添加云桌面白名单用户")
    @POST
    @Path("/GPUWhiteList/add")
    fun addGPUWhiteListUser(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "需要添加的白名单用户，多个用;分隔", required = true)
        @QueryParam("whiteListUser")
        whiteListUser: String
    ): Result<Boolean>

    @Operation(summary = "新增镜像配置")
    @POST
    @Path("/image/spec")
    fun addImageSpec(
        spec: ImageSpec
    ): Result<Boolean>

    @Operation(summary = "删除镜像配置")
    @DELETE
    @Path("/image/spec")
    fun deleteImageSpec(
        @QueryParam("id")
        id: Int
    ): Result<Boolean>

    @Operation(summary = "修改镜像配置")
    @PUT
    @Path("/image/spec")
    fun updateImageSpec(
        @QueryParam("id")
        id: Int,
        spec: ImageSpec
    ): Result<Boolean>

    @Operation(summary = "镜像配置列表")
    @GET
    @Path("/image/spec")
    fun listImageSpec(): Result<List<ImageSpec>?>

    @Operation(summary = "休眠工作空间")
    @GET
    @Path("/workspace_stop")
    fun stopWorkspace(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "销毁工作空间")
    @DELETE
    @Path("/workspace_delete")
    fun deleteWorkspace(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "销毁工作空间")
    @DELETE
    @Path("/workspace_delete_batch")
    fun batchDeleteWorkspace(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间名称列表")
        workspaceNames: Set<String>
    ): Result<Map<String, Boolean>>

    @Operation(summary = "实时获取START云桌面资源池的机器")
    @POST
    @Path("/windows/pool/list")
    fun getStartCloudResourceList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询数据")
        data: WindowsPoolListFetchData
    ): Result<Page<Map<String, Any>>>

    @Operation(summary = "获取CGS资源池的区域和机型列表")
    @GET
    @Path("/windows/pool/config")
    fun getCgsConfig(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<CgsResourceConfig>

    @Operation(summary = "初始话太湖账号信息")
    @POST
    @Path("/init_tai_user_info")
    fun initTaiUserInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        taiUsers: List<String>
    ): Result<Boolean>
}
