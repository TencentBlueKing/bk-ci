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
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.GitYamlString
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.prebuild.pojo.enums.PreBuildPluginType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.DELETE
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PREBUILD"], description = "用户-PREBUILD资源")
@Path("/user/prebuild")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPreBuildResource {
    @ApiOperation("获取用户项目信息（蓝盾项目，CHANEL=PREBUILD）")
    @GET
    @Path("/project/userProject")
    fun getUserProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<UserProject>

    @ApiOperation("初始化Agent")
    @POST
    @Path("/agent/init/{os}/{ip}/{hostName}")
    fun getOrCreateAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("操作系统类型", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("IP", required = true)
        @PathParam("ip")
        ip: String,
        @ApiParam("hostName", required = true)
        @PathParam("hostName")
        hostName: String,
        @ApiParam("指定生成node的别名", required = false)
        @QueryParam("nodeStingId")
        nodeStingId: String?
    ): Result<ThirdPartyAgentStaticInfo>

    @ApiOperation("获取agent状态")
    @GET
    @Path("/agent/status/{os}/{ip}/{hostName}")
    fun getAgentStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("操作系统类型", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("IP", required = true)
        @PathParam("ip")
        ip: String,
        @ApiParam("hostName", required = true)
        @PathParam("hostName")
        hostName: String
    ): Result<AgentStatus>

    @ApiOperation("查询所有PreBuild项目")
    @GET
    @Path("/project/preProject/list")
    fun listPreProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<PreProject>>

    @ApiOperation("查询prebuild项目是否存在")
    @GET
    @Path("/project/preProject/nameExist")
    fun preProjectNameExist(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("prebuild项目ID", required = true)
        @QueryParam("preProjectId")
        preProjectId: String
    ): Result<Boolean>

    @ApiOperation("手动启动构建")
    @POST
    @Path("/project/preProject/{preProjectId}/startup")
    fun manualStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("yaml文件", required = true)
        startUpReq: StartUpReq
    ): Result<BuildId>

    @ApiOperation("手动停止流水线")
    @DELETE
    @Path("/project/preProject/{preProjectId}/{buildId}/shutdown")
    fun manualShutdown(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @ApiOperation("获取构建详情")
    @GET
    @Path("/project/preProject/{preProjectId}/build/{buildId}")
    fun getBuildDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<ModelDetail>

    @ApiOperation("根据构建ID获取初始化所有日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs")
    fun getBuildLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否拉取DEBUG日志", required = false)
        @QueryParam("debugLog")
        debugLog: Boolean?
    ): Result<QueryLogs>

    @ApiOperation("获取某行后的日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs/after")
    fun getAfterLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @ApiParam("是否拉取DEBUG日志", required = false)
        @QueryParam("debugLog")
        debugLog: Boolean?
    ): Result<QueryLogs>

    @ApiOperation("获取报告")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/report")
    fun getReport(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<CodeccCallback?>

    @ApiOperation("获取build蓝盾链接")
    @GET
    @Path("/build/link/{preProjectId}/{buildId}")
    fun getBuildLink(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<String>

    @ApiOperation("获取build历史")
    @GET
    @Path("/history/{preProjectId}")
    fun getHistory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<HistoryResponse>>

    @ApiOperation("校验yaml格式")
    @POST
    @Path("/checkYaml")
    fun checkYaml(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("yaml内容", required = true)
        yaml: GitYamlString
    ): Result<String>

    @ApiOperation("获取插件版本信息")
    @GET
    @Path("/pluginVersion")
    fun getPluginVersion(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("用户的编译器类型", required = true)
        @QueryParam("pluginType")
        pluginType: PreBuildPluginType
    ): Result<PrePluginVersion?>
}
