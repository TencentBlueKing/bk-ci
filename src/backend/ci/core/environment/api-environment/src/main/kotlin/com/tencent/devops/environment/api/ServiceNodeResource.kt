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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
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

@Api(tags = ["SERVICE_NODE"], description = "服务-节点")
@Path("/service/node")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceNodeResource {
    @ApiOperation("获取用户有权限使用的服务器列表")
    @GET
    @Path("/projects/{projectId}/listUsableServerNodes")
    fun listUsableServerNodes(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @ApiOperation("根据hashId获取项目节点列表")
    @POST
    @Path("/projects/{projectId}/listByHashIds")
    fun listByHashIds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeWithPermission>>

    @ApiOperation("根据hashId获取项目节点列表(不校验权限)")
    @POST
    @Path("/projects/{projectId}/listRawByHashIds")
    fun listRawByHashIds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点 hashIds", required = true)
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>>

    @ApiOperation("根据环境hashId获取项目节点列表(不校验权限)")
    @POST
    @Path("/projects/{projectId}/listRawByEnvHashIds")
    fun listRawByEnvHashIds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("环境hashIds", required = true)
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>>

    @ApiOperation("根据类型查询node")
    @GET
    @Path("/projects/{projectId}/listNodeByType/{type}")
    fun listNodeByType(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("type")
        type: String
    ): Result<List<NodeBaseInfo>>

    @ApiOperation("根据机器类型查询node")
    @GET
    @Path("/projects/{projectId}/listNodeByNodeType/{nodeType}")
    fun listNodeByNodeType(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点类型", required = true)
        @PathParam("nodeType")
        nodeType: NodeType
    ): Result<List<NodeBaseInfo>>

    @ApiOperation("获取项目构建节点列表")
    @GET
    @Path("projects/{projectId}/extListNodes")
    fun extListNodes(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<NodeWithPermission>>

    @ApiOperation("删除节点")
    @POST
    @Path("/projects/{projectId}/delete_nodes")
    fun deleteNodes(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "节点列表", required = true)
        nodeHashIds: List<String>
    ): Result<Boolean>

    @ApiOperation("删除节点")
    @DELETE
    @Path("/projects/{projectId}/delete_third_party_node")
    fun deleteThirdPartyNode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "agent id", required = true)
        @QueryParam("agentId")
        agentId: String
    ): Result<Boolean>

    @ApiOperation("指定构建环境获取所有节点信息")
    @GET
    @Path("/projects/{projectId}/third_party_env2nodes")
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
