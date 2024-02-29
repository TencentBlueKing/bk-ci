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
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.GitYamlString
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.enums.PreBuildPluginType
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_PREBUILD", description = "用户-PREBUILD资源")
@Path("/user/prebuild")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPreBuildResource {
    @Operation(summary = "获取用户项目信息（蓝盾项目，CHANEL=PREBUILD）")
    @GET
    @Path("/project/userProject")
    fun getUserProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<UserProject>

    @Operation(summary = "初始化Agent")
    @POST
    @Path("/agent/init/{os}/{ip}/{hostName}")
    fun getOrCreateAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "操作系统类型", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "IP", required = true)
        @PathParam("ip")
        ip: String,
        @Parameter(description = "hostName", required = true)
        @PathParam("hostName")
        hostName: String,
        @Parameter(description = "指定生成node的别名", required = false)
        @QueryParam("nodeStingId")
        nodeStingId: String?
    ): Result<ThirdPartyAgentStaticInfo>

    @Operation(summary = "获取agent状态")
    @GET
    @Path("/agent/status/{os}/{ip}/{hostName}")
    fun getAgentStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "操作系统类型", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "IP", required = true)
        @PathParam("ip")
        ip: String,
        @Parameter(description = "hostName", required = true)
        @PathParam("hostName")
        hostName: String
    ): Result<AgentStatus>

    @Operation(summary = "查询所有PreBuild项目")
    @GET
    @Path("/project/preProject/list")
    fun listPreProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<PreProject>>

    @Operation(summary = "查询prebuild项目是否存在")
    @GET
    @Path("/project/preProject/nameExist")
    fun preProjectNameExist(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "prebuild项目ID", required = true)
        @QueryParam("preProjectId")
        preProjectId: String
    ): Result<Boolean>

    @Operation(summary = "手动启动构建")
    @POST
    @Path("/project/preProject/{preProjectId}/startup")
    fun manualStartup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "yaml文件", required = true)
        startUpReq: StartUpReq
    ): Result<BuildId>

    @Operation(summary = "手动停止流水线")
    @DELETE
    @Path("/project/preProject/{preProjectId}/{buildId}/shutdown")
    fun manualShutdown(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @Operation(summary = "获取构建详情")
    @GET
    @Path("/project/preProject/{preProjectId}/build/{buildId}")
    fun getBuildDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<ModelDetail>

    @Operation(summary = "根据构建ID获取初始化所有日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs")
    fun getBuildLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否拉取DEBUG日志", required = false)
        @QueryParam("debugLog")
        debugLog: Boolean?
    ): Result<QueryLogs>

    @Operation(summary = "获取某行后的日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs/after")
    fun getAfterLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "是否拉取DEBUG日志", required = false)
        @QueryParam("debugLog")
        debugLog: Boolean?
    ): Result<QueryLogs>

    @Operation(summary = "获取build蓝盾链接")
    @GET
    @Path("/build/link/{preProjectId}/{buildId}")
    fun getBuildLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<String>

    @Operation(summary = "获取build历史")
    @GET
    @Path("/history/{preProjectId}")
    fun getHistory(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<HistoryResponse>>

    @Operation(summary = "校验yaml格式")
    @POST
    @Path("/checkYaml")
    fun checkYaml(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "yaml内容", required = true)
        yaml: GitYamlString
    ): Result<String>

    @Operation(summary = "获取插件版本信息")
    @GET
    @Path("/pluginVersion")
    fun getPluginVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户的编译器类型", required = true)
        @QueryParam("pluginType")
        pluginType: PreBuildPluginType
    ): Result<PrePluginVersion?>
}
