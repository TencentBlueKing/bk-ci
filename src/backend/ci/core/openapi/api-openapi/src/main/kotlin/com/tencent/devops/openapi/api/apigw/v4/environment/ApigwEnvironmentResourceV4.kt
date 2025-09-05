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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeFetchReq
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.openapi.BkApigwApi
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

@Tag(name = "OPEN_API_V4_ENVIRONMENT", description = "OPENAPI-环境管理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/environment/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwEnvironmentResourceV4 {

    @Operation(
        summary = "获取用户有权限使用的CMDB服务器列表",
        tags = ["v4_user_env_list_usable_nodes", "v4_app_env_list_usable_nodes"]
    )
    @GET
    @Path("/usable_server_nodes")
    fun listUsableServerCMDBNodes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "创建环境", tags = ["v4_app_env_create", "v4_user_env_create"])
    @POST
    @Path("/envs")
    fun createEnv(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境信息", required = true)
        environment: EnvCreateInfo
    ): Result<EnvironmentId>

    @Operation(summary = "删除环境", tags = ["v4_app_env_delete", "v4_user_env_delete"])
    @DELETE
    @Path("/envs")
    fun deleteEnv(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String
    ): Result<Boolean>

    @Operation(summary = "删除节点", tags = ["v4_app_node_delete", "v4_user_node_delete"])
    @POST
    @Path("nodes_delete")
    fun deleteNodes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点列表", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "添加节点到环境", tags = ["v4_app_env_add_node", "v4_user_env_add_node"])
    @POST
    @Path("/env_add_nodes")
    fun envAddNodes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "从环境删除节点", tags = ["v4_app_env_delete_node", "v4_user_env_delete_node"])
    @POST
    @Path("/env_delete_nodes")
    fun envDeleteNodes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @Parameter(description = "节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(
        summary = "获取用户有权限使用的CMDB环境列表",
        tags = ["v4_app_env_list_usable_envs", "v4_user_env_list_usable_envs"]
    )
    @GET
    @Path("/usable_server_envs")
    fun listUsableServerEnvs(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<EnvWithPermission>>

    @Operation(
        summary = "根据环境名称获取环境CMDB信息(不校验权限)",
        tags = ["v4_user_env_list_env_by_env_names", "v4_app_env_list_env_by_env_names"]
    )
    @POST
    @Path("/envNames_to_envInfo")
    fun listEnvRawByEnvNames(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境名称(s)", required = true)
        envNames: List<String>
    ): Result<List<EnvWithPermission>>

    @Operation(
        summary = "根据hashId(多个)获取CMDB环境信息(不校验权限)",
        tags = ["v4_app_env_list_by_env_hashIds", "v4_user_env_list_by_env_hashIds"]
    )
    @POST
    @Path("/envHashIds_to_envInfo")
    fun listEnvRawByEnvHashIds(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId(s)", required = true)
        envHashIds: List<String>
    ): Result<List<EnvWithPermission>>

    @Operation(
        summary = "根据hashId获取项目CMDB节点列表(不校验权限)",
        tags = ["v4_user_env_node_list_byNodeHashIds", "v4_app_env_node_list_byNodeHashIds"]
    )
    @POST
    @Path("/nodeHashIds_to_nodes")
    fun listNodeRawByNodeHashIds(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>>

    @Operation(
        summary = "根据环境的hashId获取指定项目指定CMDB环境下节点列表(不校验权限)",
        tags = ["v4_user_env_node_list_byEnvHashIds", "v4_app_env_node_list_byEnvHashIds"]
    )
    @POST
    @Path("/envHashIds_to_nodes")
    fun listNodeRawByEnvHashIds(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashIds", required = true)
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>>

    @Operation(
        summary = "获取第三方构建节点信息（扩展接口）",
        tags = ["v4_user_env_node_list_ext", "v4_app_env_node_list_ext"]
    )
    @GET
    @Path("/ext_nodes")
    fun extListNodes(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(
        summary = "获取第三方构建节点被流水线引用数据",
        tags = ["v4_user_env_node_list_pipeline_ref", "v4_app_env_node_list_pipeline_ref"]
    )
    @GET
    @Path("/pipeline_ref_list")
    fun listPipelineRef(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashId", required = true)
        @QueryParam("nodeHashId")
        nodeHashId: String,
        @Parameter(description = "排序字段, pipelineName|lastBuildTime", required = true)
        @QueryParam("sortBy")
        sortBy: String? = null,
        @Parameter(description = "排序方向, ASC|DESC", required = true)
        @QueryParam("sortDirection")
        sortDirection: String? = null
    ): Result<List<AgentPipelineRef>>

    @Operation(summary = "设置环境共享", tags = ["v4_user_set_share_env", "v4_app_set_share_env"])
    @POST
    @Path("/share_envs")
    fun setShareEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @Parameter(description = "共享的项目列表", required = true)
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean>

    @Operation(
        summary = "指定第三方构建环境获取所有第三方构建机节点信息",
        tags = ["v4_user_third_party_env2nodes", "v4_app_third_party_env2nodes"]
    )
    @GET
    @Path("/third_party_env2nodes")
    fun thirdPartyEnv2Nodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境 hashId (envHashId和envName选填一项)", required = false)
        @QueryParam("envHashId")
        envHashId: String?,
        @Parameter(description = "环境名称 (envHashId和envName选填一项)", required = false)
        @QueryParam("envName")
        envName: String?
    ): Result<List<NodeWithPermission>>

    @Operation(
        summary = "获取项目节点列表",
        tags = ["v4_user_env_nodes", "v4_app_env_nodes"]
    )
    @POST
    @Path("/fetch_nodes")
    fun fetchNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int? = 1,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int? = 20,
        @Parameter(description = "IP", required = false)
        @QueryParam("nodeIp")
        nodeIp: String?,
        @Parameter(description = "别名", required = false)
        @QueryParam("displayName")
        displayName: String?,
        @Parameter(description = "创建人", required = false)
        @QueryParam("createdUser")
        createdUser: String?,
        @Parameter(description = "最后修改人", required = false)
        @QueryParam("lastModifiedUser")
        lastModifiedUser: String?,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keywords")
        keywords: String?,
        @Parameter(description = "节点类型|用途 (构建: THIRDPARTY;部署: CMDB)", required = false)
        @QueryParam("nodeType")
        nodeType: NodeType?,
        @Parameter(description = "Agent 状态", required = false)
        @QueryParam("nodeStatus")
        nodeStatus: NodeStatus?,
        @Parameter(description = "Agent 版本", required = false)
        @QueryParam("agentVersion")
        agentVersion: String?,
        @Parameter(description = "操作系统", required = false)
        @QueryParam("osName")
        osName: String?,
        @Parameter(description = "最近执行流水线", required = false)
        @QueryParam("latestBuildPipelineId")
        latestBuildPipelineId: String?,
        @Parameter(description = "最近构建执行时间 (开始)", required = false)
        @QueryParam("latestBuildTimeStart")
        latestBuildTimeStart: Long?,
        @Parameter(description = "最近构建执行时间 (结束)", required = false)
        @QueryParam("latestBuildTimeEnd")
        latestBuildTimeEnd: Long?,
        @Parameter(description = "排序字段", required = false)
        @QueryParam("sortType")
        sortType: String?,
        @Parameter(description = "正序ASC/倒序DESC (默认倒序)", required = false)
        @QueryParam("collation")
        collation: String?,
        data: NodeFetchReq?
    ): Result<Page<NodeWithPermission>>
}
