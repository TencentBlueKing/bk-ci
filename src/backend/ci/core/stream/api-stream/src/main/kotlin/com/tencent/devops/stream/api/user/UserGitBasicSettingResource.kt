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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.repository.pojo.AppInstallationResult
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.StreamUpdateSetting
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STREAM_SETTING"], description = "user-setting页面")
@Path("/user/basic/setting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGitBasicSettingResource {

    @ApiOperation("开启，关闭，初始化呢Stream")
    @POST
    @Path("/enable")
    fun enableStream(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("开启或关闭", required = true)
        @QueryParam("enabled")
        enabled: Boolean,
        @ApiParam("stream 项目信息(初始化时用)", required = false)
        projectInfo: StreamGitProjectInfoWithProject
    ): Result<Boolean>

    @ApiOperation("查询Stream项目配置")
    @GET
    @Path("/{projectId}")
    fun getStreamConf(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<StreamBasicSetting?>

    @ApiOperation("保存Stream配置")
    @POST
    @Path("/{projectId}/save")
    fun saveStreamConf(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("stream 项目配置", required = true)
        streamUpdateSetting: StreamUpdateSetting
    ): Result<Boolean>

    @ApiOperation("保存Stream 权限校验配置")
    @POST
    @Path("/{projectId}/save_review_setting")
    fun saveStreamReviewSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("stream 权限校验配置", required = true)
        triggerReviewSetting: TriggerReviewSetting
    ): Result<Boolean>

    @ApiOperation("修改项目启动人")
    @POST
    @Path("/{projectId}/reset/oauth")
    fun updateEnableUser(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "目标授权人", required = true)
        @QueryParam("authUserId")
        authUserId: String
    ): Result<Boolean>

    @ApiOperation("根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @ApiParam(value = "oauth认证成功后重定向到前端的地址", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?,
        @ApiParam(value = "stream 项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "是否刷新token", required = false)
        @QueryParam("refreshToken")
        refreshToken: Boolean? = true
    ): Result<AuthorizeResult>

    @ApiOperation("获取第三方构建机任务")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/listAgentBuilds")
    fun listAgentBuilds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

    @ApiOperation("判断仓库app是否安装")
    @GET
    @Path("/isInstallApp")
    fun isInstallApp(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "stream 项目Id", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<AppInstallationResult>
}
