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

package com.tencent.devops.experience.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceInfoForBuild
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceUpdate
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_EXPERIENCE"], description = "版本体验-发布体验")
@Path("/service/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceExperienceResource {
    @ApiOperation("创建体验")
    @Path("/{projectId}/")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("发布详情", required = true)
        experience: ExperienceServiceCreate
    ): Result<String>

    @ApiOperation("获取体验列表数量")
    @Path("/list/count")
    @POST
    fun count(
        @ApiParam("项目ID集合", required = false)
        projectIds: Set<String>? = setOf(),
        @ApiParam("", required = false)
        @QueryParam("expired")
        expired: Boolean? = false
    ): Result<Map<String, Int>>

    @ApiOperation("获取体验详情")
    @Path("/projects/{projectId}/experienceIds/{experienceHashId}")
    @GET
    fun get(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Experience>

    @ApiOperation("是否有体验权限")
    @Path("/experienceIds/{experienceHashId}/check")
    @GET
    fun check(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @ApiParam("组织", required = false)
        @QueryParam("organization")
        organization: String?
    ): Result<Boolean>

    @ApiOperation("通过bundleId获取公开体验跳转信息")
    @Path("/jumpInfo")
    @GET
    fun jumpInfo(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("bundleId", required = true)
        @QueryParam("bundleIdentifier")
        bundleIdentifier: String,
        @ApiParam("平台", required = true)
        @QueryParam("platform")
        platform: String
    ): Result<ExperienceJumpInfo>

    @ApiOperation("获取构建中的体验的信息")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}")
    @GET
    fun listForBuild(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<ExperienceInfoForBuild>>

    @ApiOperation("编辑体验")
    @Path("/{projectId}/{experienceHashId}")
    @PUT
    fun edit(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @ApiParam("发布详情", required = true)
        experience: ExperienceUpdate
    ): Result<Boolean>

    @ApiOperation("下架体验")
    @Path("/{projectId}/{experienceHashId}/offline")
    @PUT
    fun offline(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Boolean>

    @ApiOperation("上架体验")
    @Path("/{projectId}/{experienceHashId}/online")
    @PUT
    fun online(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Boolean>
}
