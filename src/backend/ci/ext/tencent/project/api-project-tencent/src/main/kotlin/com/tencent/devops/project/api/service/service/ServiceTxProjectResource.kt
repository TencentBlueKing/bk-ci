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

package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BG_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.project.pojo.ProjectCreateInfo
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

@Api(tags = ["SERVICE_PROJECT_TX"], description = "蓝盾项目列表接口")
@Path("/service/projects/tx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTxProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询所有项目")
    fun list(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByGroup")
    @ApiOperation("根据组织架构查询所有项目")
    fun getProjectByGroup(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("bgName", required = false)
        @QueryParam("bgName")
        bgName: String?,
        @ApiParam("deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("centerName", required = false)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<ProjectVO>>

    @GET
    @Path("/preBuild/userProject/{userId}")
    @ApiOperation("查询用户项目")
    fun getPreUserProject(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String
    ): Result<ProjectVO?>

    @GET
    @Path("/enNames/organization")
    @ApiOperation("查询用户项目")
    fun getProjectEnNamesByOrganization(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("BG_ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BG_ID)
        bgId: Long,
        @ApiParam("部门名称", required = true)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("中心名称", required = true)
        @QueryParam("centerName")
        centerName: String?
    ): Result<List<String>>

    @GET
//    @Path("/preBuild/userProject/{userId}")
    @Path("/preBuild/userProject/userId/{userId}")
    @ApiOperation("查询用户项目")
    fun getPreUserProjectV2(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String
    ): Result<ProjectVO?>

    @POST
    @Path("/newProject")
    @ApiOperation("创建项目")
    fun create(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "项目信息", required = true)
        projectCreateInfo: ProjectCreateInfo
    ): Result<String>

    @GET
    @Path("/{projectCode}/users/{userId}/verifyWithToken")
    @ApiOperation(" 校验用户是否项目成员")
    fun verifyUserProjectPermission(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/verifyProjectByOrganization")
    @ApiOperation(" 校验项目是否数据某组织架构")
    fun verifyProjectByOrganization(
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam(value = "组织类型", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织ID", required = true)
        @QueryParam(AUTH_HEADER_DEVOPS_ORGANIZATION_ID)
        organizationId: Int
    ): Result<Boolean>

    @POST
    @Path("/gitci/{gitProjectId}/{userId}")
    @ApiOperation("创建gitCI项目")
    fun createGitCIProject(
        @ApiParam("工蜂项目id", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("用户名", required = true)
        @PathParam("userId")
        userId: String
    ): Result<ProjectVO>
}