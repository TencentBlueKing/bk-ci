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

package com.tencent.devops.openapi.api.apigw.v4.environment

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchFetchAgentData
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchUpdateAgentEnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.UpdateAgentInfo
import com.tencent.devops.openapi.BkApigwApi
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

@Tag(name = "OPEN_API_V4_ENVIRONMENT", description = "OPENAPI-环境管理-构建机管理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/environment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwEnvironmentAgentResourceV4 {

    @Operation(summary = "获取项目下第三方构建机列表", tags = ["v4_app_node_list", "v4_user_node_list"])
    @GET
    @Path("/third_part_agent_nodes")
    fun thirdPartAgentList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "获取指定第三方构建机状态", tags = ["v4_user_node_status", "v4_app_node_status"])
    @Path("/third_part_agent_node_status")
    @GET
    fun getNodeStatus(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(description = "节点 别名 (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeName")
        nodeName: String?,
        @Parameter(description = "节点 agentId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("agentHashId")
        agentHashId: String?
    ): Result<NodeWithPermission?>

    @Operation(
        summary = "获取指定第三方构建机详情信息",
        tags = ["v4_user_node_third_part_detail", "v4_app_node_third_part_detail"]
    )
    @Path("/third_part_agent_node_detail")
    @GET
    fun getNodeDetail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(description = "节点 别名 (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeName")
        nodeName: String?,
        @Parameter(description = "节点 agentId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("agentHashId")
        agentHashId: String?
    ): Result<ThirdPartyAgentDetail?>

    @Operation(
        summary = "获取第三方构建机任务",
        tags = ["v4_user_node_third_part_builds", "v4_app_node_third_part_builds"]
    )
    @GET
    @Path("/third_part_agent_builds")
    fun listAgentBuilds(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(description = "节点 别名 (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("nodeName")
        nodeName: String?,
        @Parameter(description = "节点 agentId (nodeHashId、nodeName、agentHashId 三个参数任选其一填入即可)", required = false)
        @QueryParam("agentHashId")
        agentHashId: String?,
        @Parameter(description = "筛选此状态，支持4种输入(QUEUE,RUNNING,DONE,FAILURE)", required = false)
        @QueryParam("status")
        status: String?,
        @Parameter(description = "筛选此pipelineId", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

    @Operation(
        summary = "批量查询Agent环境变量",
        tags = ["v4_user_node_third_part_agent_envvar", "v4_app_node_third_part_agent_envvar"]
    )
    @POST
    @Path("/fetch_agent_env")
    fun fetchAgentEnv(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询数据", required = true)
        data: BatchFetchAgentData
    ): Result<Map<String, List<EnvVar>>>

    @Operation(
        summary = "批量修改Agent环境变量",
        tags = ["v4_user_node_third_part_agent_update_envvar", "v4_app_node_third_part_agent_update_envvar"]
    )
    @POST
    @Path("/batch_update_agent_env")
    fun batchUpdateEnv(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "修改数据", required = true)
        data: BatchUpdateAgentEnvVar
    ): Result<Boolean>

    @Operation(
        summary = "修改Agent信息",
        tags = ["v4_user_node_third_part_agent_update_agent_info", "v4_app_node_third_part_agent_update_agent_info"]
    )
    @POST
    @Path("/update_agent_info")
    fun updateAgentInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "修改数据", required = true)
        data: UpdateAgentInfo
    ): Result<Boolean>

    @Operation(summary = "查询项目标签和对应节点数",
        tags = ["v4_user_node_third_part_agent_tags", "v4_app_node_third_part_agent_tags"])
    @GET
    @Path("/fetch_agent_tag")
    fun fetchTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<NodeTag>>
}
