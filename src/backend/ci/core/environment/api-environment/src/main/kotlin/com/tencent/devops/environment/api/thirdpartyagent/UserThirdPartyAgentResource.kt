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

package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchUpdateParallelTaskCountData
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentAction
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStatusWithInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/user/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions")
interface UserThirdPartyAgentResource {

    @Operation(summary = "是否启动第三方构建机接入")
    @GET
    @Path("/projects/{projectId}/enable")
    fun isProjectEnable(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "生成链接")
    @GET
    @Path("/projects/{projectId}/os/{os}/generateLink")
    fun generateLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "网关地域", required = false)
        @QueryParam("zoneName")
        zoneName: String?
    ): Result<ThirdPartyAgentLink>

    @Operation(summary = "生成批量安装链接")
    @GET
    @Path("/projects/{projectId}/os/{os}/generateBatchInstallLink")
    fun generateBatchInstallLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "网关地域", required = false)
        @QueryParam("zoneName")
        zoneName: String?,
        @Parameter(description = "登录账户名", required = false)
        @QueryParam("loginName")
        loginName: String?,
        @Parameter(description = "登录账户密码", required = false)
        @QueryParam("loginPassword")
        loginPassword: String?,
        @Parameter(description = "Agent安装模式", required = false)
        @QueryParam("installType")
        installType: TPAInstallType?,
        @Parameter(description = "重装使用的AgentHashId", required = false)
        @QueryParam("reInstallId")
        reInstallId: String?
    ): Result<String>

    @Operation(summary = "获取网关列表")
    @GET
    @Path("/projects/{projectId}/os/{os}/gateway")
    fun getGateway(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "可见性", required = false)
        @QueryParam("visibility")
        visibility: Boolean?
    ): Result<List<SlaveGateway>>

    @Operation(summary = "查看Agent安装链接")
    @GET
    @Path("/projects/{projectId}/node/{nodeId}/link")
    fun getLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeId")
        nodeId: String
    ): Result<ThirdPartyAgentLink>

    @Operation(summary = "查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/os/{os}/list")
    fun listAgents(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ThirdPartyAgentInfo>>

    @Operation(summary = "查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/list")
    fun listAgents(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ThirdPartyAgentInfo>>

    @Operation(summary = "查询Agent状态")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/status")
    fun getAgentStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<ThirdPartyAgentStatusWithInfo>

    @Operation(summary = "导入该第三方构建机")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/import")
    fun importAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 64)
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        @BkField(minLength = 1, maxLength = 32)
        agentId: String
    ): Result<Boolean>

    @Operation(summary = "删除该第三方构建机")
    @DELETE
    @Path("/projects/{projectId}/nodes/{nodeHashId}/delete")
    fun deleteAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 64)
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        @BkField(minLength = 1, maxLength = 32)
        nodeHashId: String
    ): Result<Boolean>

    @Operation(summary = "批量删除第三方构建机")
    @DELETE
    @Path("/projects/{projectId}/nodes/batch_delete")
    fun batchDeleteAgent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 64)
        projectId: String,
        @Parameter(description = "Node Hash IDs", required = true)
        nodeHashIds: Set<String>
    ): Result<Boolean>

    @Operation(summary = "保存agent环境变量")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/envs")
    fun saveAgentEnvs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "Envs", required = true)
        envs: List<EnvVar>
    ): Result<Boolean>

    @Operation(summary = "获取agent环境变量")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/envs")
    fun getAgentEnvs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<List<EnvVar>>

    @Operation(summary = "设置agent构建并发数")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/parallelTaskCount")
    fun setAgentParallelTaskCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "parallelTaskCount", required = true)
        @QueryParam("parallelTaskCount")
        parallelTaskCount: Int
    ): Result<Boolean>

    @Operation(summary = "设置agent Docker构建并发数")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/dockerParallelTaskCount")
    fun setAgentDockerParallelTaskCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "count", required = true)
        @QueryParam("count")
        count: Int
    ): Result<Boolean>

    @Operation(summary = "批量修改agent并发数")
    @POST
    @Path("/projects/{projectId}/nodes/batchUpdateParallelTaskCount")
    fun batchUpdateParallelTaskCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        data: BatchUpdateParallelTaskCountData
    ): Result<Boolean>

    @Operation(summary = "获取构建机详情")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/thirdPartyAgentDetail")
    fun getThirdPartyAgentDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<ThirdPartyAgentDetail?>

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

    @Operation(summary = "获取构建机最近执行记录")
    @GET
    @Path("/projects/{projectId}/listLatestBuildPipelines")
    fun listLatestBuildPipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

    @Operation(summary = "获取第三方构建机活动")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/listAgentActions")
    fun listAgentActions(
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
    ): Result<Page<ThirdPartyAgentAction>>

    @Operation(summary = "获取 CPU 使用率图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryCpuUsageMetrix")
    fun queryCpuUsageMetrix(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @Operation(summary = "获取内存使用率图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryMemoryUsageMetrix")
    fun queryMemoryUsageMetrix(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @Operation(summary = "获取磁盘IO图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryDiskioMetrix")
    fun queryDiskioMetrix(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @Operation(summary = "获取网络图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryNetMetrix")
    fun queryNetMetrix(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>
}
