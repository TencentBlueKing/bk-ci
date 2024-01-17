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

package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.BKGPT
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.pojo.Watermark
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.glassfish.jersey.server.ChunkedOutput
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

@Tag(name = "USER_WORKSPACE", description = "用户-工作空间,apiType:内网传user，离岸传desktop")
@Path("/{apiType:user|desktop}/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRemoteDevResource {

    @Operation(summary = "获取远程开发环境配置")
    @GET
    @Path("/settings")
    fun getRemoteDevSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<RemoteDevSettings>

    @Operation(summary = "更新远程开发环境配置")
    @POST
    @Path("/settings")
    fun updateRemoteDevSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间描述", required = false)
        remoteDevSettings: RemoteDevSettings
    ): Result<Boolean>

    @Operation(summary = "BK-GPT")
    @POST
    @Path("/bkGPT")
    fun bkGPT(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam("X-DEVOPS-BK-TICKET")
        bkTicket: String,
        @Context
        headers: HttpHeaders,
        data: BKGPT
    ): ChunkedOutput<String>

    @Operation(summary = "watermark")
    @POST
    @Path("/watermark")
    fun getWatermark(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: Watermark
    ): Result<Any>

    @Operation(summary = "上报preci agent id")
    @POST
    @Path("/preci_agent")
    fun preCiAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间ID", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "agentId", required = true)
        @QueryParam("agentId")
        agentId: String
    ): Result<Boolean>

    @Operation(summary = "根据bi_ticket或bk_token获取用户名称")
    @GET
    @Path("/get_user")
    fun getUser(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<String>

    @Operation(summary = "获取所有的WINDOWS GPU资源配置信息")
    @GET
    @Path("/get_all_windows_resource_config")
    fun getAllWindowsResourceConfig(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "是否包含不可用机型", required = false)
        @QueryParam("withUnavailable")
        @DefaultValue("false")
        withUnavailable: Boolean? = false
    ): Result<List<WindowsResourceTypeConfig>>

    @Operation(summary = "获取所有的WINDOWS GPU资源地域信息")
    @GET
    @Path("/get_all_windows_resource_zone")
    fun getAllWindowsResourceZone(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WindowsResourceZoneConfig>>

    @Operation(summary = "获取所有的WINDOWS 配额")
    @GET
    @Path("/get_all_windows_resource_quota")
    fun allWindowsQuota(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("searchCustom")
        searchCustom: Boolean?
    ): Result<Map<String, Map<String, Int>>>

    @Operation(summary = "获取用户1Password")
    @GET
    @Path("/1Password")
    fun onePassword(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作空间ID", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<String>

    @Operation(summary = "一键认领求助问题")
    @GET
    @Path("/addExpSup")
    fun addExpSup(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工单id", required = true)
        @QueryParam("id")
        id: Long,
        @Parameter(description = "工作空间ID", required = true)
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<Boolean>

    @Operation(summary = "获取兔小巢用户登录态token")
    @GET
    @Path("/txc/token")
    fun getTxcToken(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户唯一标识", required = true)
        @QueryParam("openId")
        openId: String,
        @Parameter(description = "用户昵称", required = true)
        @QueryParam("nickName")
        nickName: String,
        @Parameter(description = "用户头像", required = true)
        @QueryParam("avatar")
        avatar: String
    ): Result<String>

    @Operation(summary = "一键查询CGS密码")
    @GET
    @Path("/queryCgsPwd")
    fun queryCgsPwd(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "cgsId", required = true)
        @QueryParam("cgsId")
        cgsId: String
    ): Result<Boolean>
}
