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

package com.tencent.devops.prebuild.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.prebuild.pojo.ide.IdeDirInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.IDEAgentReq
import com.tencent.devops.prebuild.pojo.IDEInfo
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.pojo.ProjectVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Tag(name = "WEBIDE", description = "WebIDE资源")
@Path("/user/webide")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface WebIDEResource {
    @Operation(summary = "获取用户IDE实例列表，包含IDE服务状态、Agent状态")
    @GET
    @Path("/ideList/{projectId}")
    fun getUserIDEList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目id", required = true)
        projectId: String
    ): Result<List<IDEInfo>>

    @Operation(summary = "初始化agent")
    @POST
    @Path("/setupAgent")
    fun setupAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "IDE Agent请求包", required = true)
        req: IDEAgentReq
    ): Result<BuildId>

    @Operation(summary = "获取agent安装连接")
    @GET
    @Path("/getAgentInstallLink/{projectId}/{zoneName}/{operationSystem}/{initIp}")
    fun getAgentInstallLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目id", required = true, example = "start")
        projectId: String,
        @PathParam("zoneName")
        @Parameter(description = "服务器所在地", required = true, example = "0")
        zoneName: String,
        @PathParam("operationSystem")
        @Parameter(description = "操作系统类型(可选：LINUX/WINDOWS/MACOS)", required = true, example = "LINUX")
        operationSystem: String,
        @PathParam("initIp")
        @Parameter(description = "构建机IP", required = false)
        initIp: String
    ): Result<ThirdPartyAgentStaticInfo>

    @GET
    @Path("/userProject")
    @Operation(summary = "查询用户项目")
    fun getUserProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<ProjectVO?>

    @GET
    @Path("/heartBeat/{ip}")
    @Operation(summary = "ide心跳上报接口")
    fun heartBeat(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @Parameter(description = "IDE实例的ip地址", required = true)
        ip: String
    ): Result<Boolean>

    @GET
    @Path("/lastOpenDir/{ip}")
    @Operation(summary = "获取上次打开的项目路径")
    fun lastOpenDir(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @Parameter(description = "实例id", required = true)
        ip: String
    ): Result<IdeDirInfo>

    @POST
    @Path("/lastOpenDir/{ip}")
    @Operation(summary = "更新上次打开的项目路径")
    fun updateLastOpenDir(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @Parameter(description = "实例id", required = true)
        ip: String,
        @Parameter(description = "路径", required = true)
        path: String
    ): Result<Boolean>
}
