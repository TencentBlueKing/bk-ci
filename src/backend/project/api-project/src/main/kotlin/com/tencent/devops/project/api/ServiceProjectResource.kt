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

package com.tencent.devops.project.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT"], description = "项目列表接口")
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询所有项目")
    fun list(
        @ApiParam("用户ID", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @POST
    @Path("/")
    @ApiOperation("查询指定项目")
    fun listByProjectCode(
        @ApiParam(value = "项目id", required = true)
        projectCodes: Set<String>
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByUser")
    @ApiOperation("查询所有项目")
    fun getProjectByUser(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userName: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/{projectCode}/users/{userId}/verify")
    @ApiOperation(" 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/getNameByCode")
    @ApiOperation("根据项目Code获取对应的名称")
    fun getNameByCode(
        @ApiParam("projectCodes，多个以英文逗号分隔", required = true)
        @QueryParam("projectCodes")
        projectCodes: String
    ): Result<HashMap<String, String>>

    @GET
    @Path("/{projectId}")
    @ApiOperation("查询所有项目")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        englishName: String
    ): Result<ProjectVO?>
}