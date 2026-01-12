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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeFetchReq
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "SERVICE_NODE", description = "服务-节点")
@Path("/service/node")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceNodeResource {
    @Operation(summary = "获取用户有权限使用的服务器列表")
    @GET
    @Path("/projects/{projectId}/listUsableServerNodes")
    fun listUsableServerNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "根据hashId获取项目节点列表")
    @POST
    @Path("/projects/{projectId}/listByHashIds")
    fun listByHashIds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "获取项目节点详情")
    @POST
    @Path("/projects/{projectId}/node_status")
    fun getNodeStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
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
        agentHashId: String?
    ): Result<NodeWithPermission>

    @Operation(summary = "根据hashId获取项目节点列表(不校验权限)")
    @POST
    @Path("/projects/{projectId}/listRawByHashIds")
    fun listRawByHashIds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "根据环境hashId获取项目节点列表(不校验权限)")
    @POST
    @Path("/projects/{projectId}/listRawByEnvHashIds")
    fun listRawByEnvHashIds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "环境hashIds", required = true)
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>>

    @Operation(summary = "根据类型查询node")
    @GET
    @Path("/projects/{projectId}/listNodeByType/{type}")
    fun listNodeByType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("type")
        type: String
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "根据机器类型查询node")
    @GET
    @Path("/projects/{projectId}/listNodeByNodeType/{nodeType}")
    fun listNodeByNodeType(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点类型", required = true)
        @PathParam("nodeType")
        nodeType: NodeType
    ): Result<List<NodeBaseInfo>>

    @Operation(summary = "获取项目构建节点列表")
    @GET
    @Path("projects/{projectId}/extListNodes")
    fun extListNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @Operation(summary = "删除节点")
    @POST
    @Path("/projects/{projectId}/delete_nodes")
    fun deleteNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "节点列表", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "删除节点")
    @DELETE
    @Path("/projects/{projectId}/delete_third_party_node")
    fun deleteThirdPartyNode(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "agent id", required = true)
        @QueryParam("agentId")
        agentId: String
    ): Result<Boolean>

    @Operation(summary = "指定构建环境获取所有节点信息")
    @GET
    @Path("/projects/{projectId}/third_party_env2nodes")
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

    @Operation(summary = "获取项目节点列表")
    @POST
    @Path("/projects/{projectId}/fetch_nodes")
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
