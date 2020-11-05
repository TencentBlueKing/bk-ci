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

package com.tencent.devops.dispatch.docker.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.DockerIpListPage
import com.tencent.devops.dispatch.docker.pojo.DockerIpUpdateVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_DISPATCH_IDC"], description = "OP-IDC构建机管理接口")
@Path("/op/dispatchDocker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPDispatchDockerResource {

    @GET
    @Path("/getDockerIpList")
    @ApiOperation("获取Docker构建机列表")
    fun listDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<DockerIpListPage<DockerIpInfoVO>>

    @POST
    @Path("/add")
    @ApiOperation("批量新增Docker构建机")
    fun createDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("IDC构建机信息", required = true)
        dockerIpInfoVOs: List<DockerIpInfoVO>
    ): Result<Boolean>

    @PUT
    @Path("/update/{dockerIp}")
    @ApiOperation("更新Docker构建机状态")
    fun updateDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("IDC构建机ID", required = true)
        @PathParam("dockerIp")
        dockerIp: String,
        @ApiParam("IDC构建机信息", required = true)
        dockerIpUpdateVO: DockerIpUpdateVO
    ): Result<Boolean>

    @PUT
    @Path("/update/all/enable")
    @ApiOperation("重置所有Docker构建机状态可用")
    fun updateAllDispatchDockerEnable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Boolean>

    @DELETE
    @Path("/delete/{dockerIp}")
    @ApiOperation("删除Docker构建机")
    fun deleteDispatchDocker(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("dockerIp")
        dockerIp: String
    ): Result<Boolean>

    @DELETE
    @Path("/dockerBuildBinding/delete/{pipelineId}/{vmSeqId}")
    @ApiOperation("删除Docker构建绑定关系")
    fun removeDockerBuildBinding(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建序列号", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<Boolean>

    @POST
    @Path("/load-config/add")
    @ApiOperation("新增Docker构建机负载配置")
    fun createDockerHostLoadConfig(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("创建IDC构建机所需信息", required = true)
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Result<Boolean>

    @POST
    @Path("/docker/threshold/update")
    @ApiOperation("更新docker漂移负载阈值")
    fun updateDockerDriftThreshold(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("阈值", required = true)
        thresholdMap: Map<String, String>
    ): Result<Boolean>
}