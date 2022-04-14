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

package com.tencent.devops.auth.api.op

import com.tencent.devops.auth.pojo.enum.SystemType
import com.tencent.devops.auth.pojo.resource.CreateResourceDTO
import com.tencent.devops.auth.pojo.resource.ResourceInfo
import com.tencent.devops.auth.pojo.resource.UpdateResourceDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_RESOURCE"], description = "权限-系统资源")
@Path("/op/resources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpResourceResource {

    @POST
    @ApiOperation("添加系统资源")
    @Path("/")
    fun createSystemResource(
        @ApiParam("操作人")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("系统资源参数")
        resourceInfo: CreateResourceDTO
    ): Result<Boolean>

    @PUT
    @ApiOperation("修改系统资源")
    @Path("/{resourceId}")
    fun updateSystemResource(
        @ApiParam("操作人")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("资源编码")
        @PathParam("resourceId")
        resourceId: String,
        @ApiParam("系统资源参数")
        resourceInfo: UpdateResourceDTO
    ): Result<Boolean>

    @GET
    @ApiOperation("获取资源信息")
    @Path("/{resourceId}")
    fun getSystemResource(
        @ApiParam("资源编码")
        @PathParam("resourceId")
        resourceId: String
    ): Result<ResourceInfo?>

    @GET
    @ApiOperation("按系统获取资源信息")
    @Path("/systems/{systemType}")
    fun getSystemResourceBySystem(
        @ApiParam("系统")
        @PathParam("systemType")
        systemId: SystemType
    ): Result<List<ResourceInfo>?>

    @GET
    @ApiOperation("获取所有资源列表")
    @Path("/")
    fun listSystemResource(): Result<List<ResourceInfo>?>
}
