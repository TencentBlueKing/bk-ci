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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamUpdateSetting
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STREAM_SETTING", description = "user-setting页面")
@Path("/user/basic/setting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGitBasicSettingResource {

    @Operation(summary = "开启，关闭，初始化呢Stream")
    @POST
    @Path("/enable")
    fun enableStream(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "开启或关闭", required = true)
        @QueryParam("enabled")
        enabled: Boolean,
        @Parameter(description = "stream 项目信息(初始化时用)", required = false)
        projectInfo: StreamGitProjectInfoWithProject
    ): Result<Boolean>

    @Operation(summary = "查询Stream项目配置")
    @GET
    @Path("/{projectId}")
    fun getStreamConf(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<StreamBasicSetting?>

    @Operation(summary = "保存Stream配置")
    @POST
    @Path("/{projectId}/save")
    fun saveStreamConf(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "stream 项目配置", required = true)
        streamUpdateSetting: StreamUpdateSetting
    ): Result<Boolean>

    @Operation(summary = "保存Stream 权限校验配置")
    @POST
    @Path("/{projectId}/save_review_setting")
    fun saveStreamReviewSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "stream 权限校验配置", required = true)
        triggerReviewSetting: TriggerReviewSetting
    ): Result<Boolean>

    @Operation(summary = "修改项目启动人")
    @POST
    @Path("/{projectId}/reset/oauth")
    fun updateEnableUser(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标授权人", required = true)
        @QueryParam("authUserId")
        authUserId: String
    ): Result<Boolean>

    @Operation(summary = "根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @Parameter(description = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @Parameter(description = "stream 项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @Parameter(description = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = true
    ): Result<AuthorizeResult>

    @Operation(summary = "获取第三方构建机任务")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/listAgentBuilds")
    fun listAgentBuilds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

    @Operation(summary = "判断仓库app是否安装")
    @GET
    @Path("/isInstallApp")
    fun isInstallApp(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "stream 项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<AppInstallationResult>
}
