/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.DevCloudModel
import com.tencent.devops.environment.pojo.DevCloudImageParam
import com.tencent.devops.environment.pojo.DevCloudVmParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_DEVCLOUD"], description = "用户-DEVCLOUD信息")
@Path("/user/devcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDevCloudResource {

    @ApiOperation("获取DevCloud机型列表")
    @GET
    @Path("/{projectId}/getModelList")
    fun getDevCloudModelList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DevCloudModel>>

    @ApiOperation("添加DevCloud虚拟机")
    @POST
    @Path("/{projectId}/addDevCloudVm")
    fun addDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点配置", required = true)
        devCloudVmParam: DevCloudVmParam
    ): Result<Boolean>

    @ApiOperation("开机DevCloud虚拟机")
    @POST
    @Path("/{projectId}/startDevCloudVm/{nodeHashId}")
    fun startDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("关机DevCloud虚拟机")
    @POST
    @Path("/{projectId}/stopDevCloudVm/{nodeHashId}")
    fun stopDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("销毁DevCloud虚拟机")
    @POST
    @Path("/{projectId}/deleteDevCloudVm/{nodeHashId}")
    fun deleteDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("制作镜像")
    @POST
    @Path("/{projectId}/createImage/{nodeHashId}")
    fun createImage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String,
        @ApiParam("镜像参数", required = true)
        devCloudImage: DevCloudImageParam
    ): Result<Boolean>

    @ApiOperation("制作镜像结果确认")
    @POST
    @Path("/{projectId}/confirm/{nodeHashId}")
    fun createImageResultConfirm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Boolean>

    @ApiOperation("查询DevCloud虚拟机状态")
    @GET
    @Path("/{projectId}/getDevCloudVm/{nodeHashId}")
    fun getDevCloudVm(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("节点ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<Map<String, Any>>
}