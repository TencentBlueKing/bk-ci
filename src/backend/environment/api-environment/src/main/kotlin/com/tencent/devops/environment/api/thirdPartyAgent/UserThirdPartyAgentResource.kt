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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.api.thirdPartyAgent

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentAction
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStatusWithInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

@Api(tags = ["USER_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/user/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserThirdPartyAgentResource {
    @ApiOperation("是否启动第三方构建机接入")
    @GET
    @Path("/projects/{projectId}/enable")
    fun isProjectEnable(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("生成链接")
    @GET
    @Path("/projects/{projectId}/os/{os}/generateLink")
    fun generateLink(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("网关地域", required = false)
        @QueryParam("zoneName")
        zoneName: String?
    ): Result<ThirdPartyAgentLink>

    @ApiOperation("获取网关列表")
    @GET
    @Path("/projects/{projectId}/os/{os}/gateway")
    fun getGateway(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<SlaveGateway>>

    @ApiOperation("查看Agent安装链接")
    @GET
    @Path("/projects/{projectId}/node/{nodeId}/link")
    fun getLink(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeId")
        nodeId: String
    ): Result<ThirdPartyAgentLink>

    @ApiOperation("查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/os/{os}/list")
    fun listAgents(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ThirdPartyAgentInfo>>

    @ApiOperation("查询Agent状态")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/status")
    fun getAgentStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<ThirdPartyAgentStatusWithInfo>

    @ApiOperation("导入该第三方构建机")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/import")
    fun importAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<Boolean>

    @ApiOperation("删除该第三方构建机")
    @DELETE
    @Path("/projects/{projectId}/nodes/{nodeId}/delete")
    fun deleteAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeId")
        nodeId: String
    ): Result<Boolean>

    @ApiOperation("保存agent环境变量")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/envs")
    fun saveAgentEnvs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("Envs", required = true)
        envs: List<EnvVar>
    ): Result<Boolean>

    @ApiOperation("获取agent环境变量")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/envs")
    fun getAgentEnvs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<List<EnvVar>>

    @ApiOperation("设置agent构建并发数")
    @POST
    @Path("/projects/{projectId}/nodes/{nodeHashId}/parallelTaskCount")
    fun setAgentParallelTaskCount(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("parallelTaskCount", required = true)
        @QueryParam("parallelTaskCount")
        parallelTaskCount: Int
    ): Result<Boolean>

    @ApiOperation("获取构建机详情")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/thirdPartyAgentDetail")
    fun getThirdPartyAgentDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<ThirdPartyAgentDetail?>

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

    @ApiOperation("获取第三方构建机活动")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/listAgentActions")
    fun listAgentActions(
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
    ): Result<Page<ThirdPartyAgentAction>>

    @ApiOperation("获取 CPU 使用率图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryCpuUsageMetrix")
    fun queryCpuUsageMetrix(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @ApiOperation("获取内存使用率图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryMemoryUsageMetrix")
    fun queryMemoryUsageMetrix(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @ApiOperation("获取磁盘IO图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryDiskioMetrix")
    fun queryDiskioMetrix(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>

    @ApiOperation("获取网络图表数据")
    @GET
    @Path("/projects/{projectId}/nodes/{nodeHashId}/queryNetMetrix")
    fun queryNetMetrix(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("时间段，HOUR | DAY | WEEK", required = true)
        @QueryParam("timeRange")
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>>
}