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

package com.tencent.devops.openapi.api.apigw.v4.environment

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentPipelineRef
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

@Api(tags = ["OPEN_API_V4_ENVIRONMENT"], description = "OPENAPI-环境管理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/environment/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwEnvironmentResourceV4 {

    @ApiOperation("获取用户有权限使用的服务器列表", tags = ["v4_user_env_list_usable_nodes", "v4_app_env_list_usable_nodes"])
    @GET
    @Path("/usable_server_nodes")
    fun listUsableServerNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @ApiOperation("创建环境", tags = ["v4_app_env_create", "v4_user_env_create"])
    @POST
    @Path("/envs")
    fun createEnv(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "环境信息", required = true)
        environment: EnvCreateInfo
    ): Result<EnvironmentId>

    @ApiOperation("删除环境", tags = ["v4_app_env_delete", "v4_user_env_delete"])
    @DELETE
    @Path("/envs")
    fun deleteEnv(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String
    ): Result<Boolean>

    @ApiOperation("删除节点", tags = ["v4_app_node_delete", "v4_user_node_delete"])
    @POST
    @Path("nodes_delete")
    fun deleteNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "节点列表", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @ApiOperation("添加节点到环境", tags = ["v4_app_env_add_node", "v4_user_env_add_node"])
    @POST
    @Path("/env_add_nodes")
    fun envAddNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @ApiParam("节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @ApiOperation("从环境删除节点", tags = ["v4_app_env_delete_node", "v4_user_env_delete_node"])
    @POST
    @Path("/env_delete_nodes")
    fun envDeleteNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @ApiParam("节点 HashId", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @ApiOperation("获取用户有权限使用的环境列表", tags = ["v4_app_env_list_usable_envs", "v4_user_env_list_usable_envs"])
    @GET
    @Path("/usable_server_envs")
    fun listUsableServerEnvs(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<EnvWithPermission>>

    @ApiOperation(
        "根据环境名称获取环境信息(不校验权限)",
        tags = ["v4_user_env_list_env_by_env_names", "v4_app_env_list_env_by_env_names"]
    )
    @POST
    @Path("/envNames_to_envInfo")
    fun listEnvRawByEnvNames(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境名称(s)", required = true)
        envNames: List<String>
    ): Result<List<EnvWithPermission>>

    @ApiOperation(
        "根据hashId(多个)获取环境信息(不校验权限)",
        tags = ["v4_app_env_list_by_env_hashIds", "v4_user_env_list_by_env_hashIds"]
    )
    @POST
    @Path("/envHashIds_to_envInfo")
    fun listEnvRawByEnvHashIds(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId(s)", required = true)
        envHashIds: List<String>
    ): Result<List<EnvWithPermission>>

    @ApiOperation(
        "根据hashId获取项目节点列表(不校验权限)",
        tags = ["v4_user_env_node_list_byNodeHashIds", "v4_app_env_node_list_byNodeHashIds"]
    )
    @POST
    @Path("/nodeHashIds_to_nodes")
    fun listNodeRawByNodeHashIds(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>>

    @ApiOperation(
        "根据环境的hashId获取指定项目指定环境下节点列表(不校验权限)",
        tags = ["v4_user_env_node_list_byEnvHashIds", "v4_app_env_node_list_byEnvHashIds"]
    )
    @POST
    @Path("/envHashIds_to_nodes")
    fun listNodeRawByEnvHashIds(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点 hashIds", required = true)
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>>

    @ApiOperation("获取构建节点信息（扩展接口）", tags = ["v4_user_env_node_list_ext", "v4_app_env_node_list_ext"])
    @GET
    @Path("/ext_nodes")
    fun extListNodes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @ApiOperation(
        "获取构建节点信息（扩展接口）",
        tags = ["v4_user_env_node_list_pipeline_ref", "v4_app_env_node_list_pipeline_ref"]
    )
    @GET
    @Path("/pipeline_ref_list")
    fun listPipelineRef(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点 hashId", required = true)
        @QueryParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("排序字段, pipelineName|lastBuildTime", required = true)
        @QueryParam("sortBy")
        sortBy: String? = null,
        @ApiParam("排序方向, ASC|DESC", required = true)
        @QueryParam("sortDirection")
        sortDirection: String? = null
    ): Result<List<AgentPipelineRef>>

    @ApiOperation("设置环境共享", tags = ["v4_user_set_share_env", "v4_app_set_share_env"])
    @POST
    @Path("/share_envs")
    fun setShareEnv(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId", required = true)
        @QueryParam("envHashId")
        envHashId: String,
        @ApiParam(value = "共享的项目列表", required = true)
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean>

    @ApiOperation("指定构建环境获取所有节点信息", tags = ["v4_user_third_party_env2nodes", "v4_app_third_party_env2nodes"])
    @GET
    @Path("/third_party_env2nodes")
    fun thirdPartyEnv2Nodes(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境 hashId (envHashId和envName选填一项)", required = false)
        @QueryParam("envHashId")
        envHashId: String?,
        @ApiParam("环境名称 (envHashId和envName选填一项)", required = false)
        @QueryParam("envName")
        envName: String?
    ): Result<List<NodeWithPermission>>
}
