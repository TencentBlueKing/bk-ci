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
package com.tencent.devops.openapi.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.pojo.AppCodeProjectResponse
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

@Api(tags = ["OP_APP_CODE_PROJECT"], description = "OP-AppCode项目资源")
@Path("/op/appCodeProject/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpAppCodeProjectResource {

    @ApiOperation("新增appCode的project")
    @POST
    @Path("{appCode}")
    fun addProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @ApiParam("appCode", required = true)
        @PathParam("appCode")
        appCode: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("获取appCode的project列表")
    @GET
    @Path("")
    fun listProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
    ): Result<List<AppCodeProjectResponse>>

    @ApiOperation("根据AppCode获取appCode的project列表")
    @GET
    @Path("{appCode}")
    fun listProjectByAppCode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @ApiParam("appCode", required = true)
        @PathParam("appCode")
        appCode: String
    ): Result<List<AppCodeProjectResponse>>

    @ApiOperation("获取单个appCode的project")
    @GET
    @Path("{appCode}/{projectId}")
    fun getProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @ApiParam("appCode", required = true)
        @PathParam("appCode")
        appCode: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<AppCodeProjectResponse?>

    @ApiOperation("删除appCode的project")
    @DELETE
    @Path("{appCode}/{projectId}")
    fun deleteProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @ApiParam("appCode", required = true)
        @PathParam("appCode")
        appCode: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
