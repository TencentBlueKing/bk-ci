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

import com.tencent.devops.auth.pojo.action.ActionInfo
import com.tencent.devops.auth.pojo.action.CreateActionDTO
import com.tencent.devops.auth.pojo.action.UpdateActionDTO
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

@Api(tags = ["OP_ACTION"], description = "权限-操作Action")
@Path("/op/actions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpActionResource {
    @POST
    @ApiOperation("添加系统操作action")
    @Path("/")
    fun createSystemAction(
        @ApiParam("操作人")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("actionInfo")
        actionInfo: CreateActionDTO
    ): Result<Boolean>

    @PUT
    @ApiOperation("修改系统操作action")
    @Path("/{actionId}")
    fun updateSystemAction(
        @ApiParam("操作人")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("资源编码")
        @PathParam("actionId")
        actionId: String,
        @ApiParam("系统action参数")
        actionInfo: UpdateActionDTO
    ): Result<Boolean>

    @GET
    @ApiOperation("按ID获取资源信息")
    @Path("/{actionId}")
    fun getAction(
        @ApiParam("操作编码")
        @PathParam("actionId")
        actionId: String
    ): Result<ActionInfo?>

    @GET
    @ApiOperation("获取操作列表")
    @Path("/")
    fun listAllAction(): Result<List<ActionInfo>?>

    @GET
    @ApiOperation("获取系统操作列表")
    @Path("/resource")
    fun listActionResource(): Result<Map<String, List<ActionInfo>>?>

    @GET
    @ApiOperation("按资源获取操作列表")
    @Path("/resources/{resourceId}")
    fun listActionByResource(
        @ApiParam("资源编码")
        @PathParam("resourceId")
        resourceId: String
    ): Result<List<ActionInfo>?>
}
