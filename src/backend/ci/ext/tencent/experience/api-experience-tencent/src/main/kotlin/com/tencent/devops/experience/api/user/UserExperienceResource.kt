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

package com.tencent.devops.experience.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperienceSummaryWithPermission
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.Url
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.outer.OuterCanAddParam
import com.tencent.devops.experience.pojo.outer.OuterCanAddVO
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "USER_EXPERIENCE", description = "版本体验-发布体验")
@Path("/user/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExperienceResource {
    @Operation(summary = "获取是否有文件转体验权限")
    @Path("/{projectId}/hasPermission")
    @GET
    fun hasArtifactoryPermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验路径", required = false)
        @QueryParam("path")
        path: String,
        @Parameter(description = "仓库类型", required = false)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType
    ): Result<Boolean>

    @Operation(summary = "获取体验列表")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "", required = false)
        @QueryParam("expired")
        expired: Boolean?
    ): Result<List<ExperienceSummaryWithPermission>>

    @Operation(summary = "获取体验详情")
    @Path("/{projectId}/{experienceHashId}")
    @GET
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Experience>

    @Operation(summary = "创建体验")
    @Path("/{projectId}/")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布详情", required = true)
        experience: ExperienceCreate
    ): Result<Boolean>

    @Operation(summary = "编辑体验")
    @Path("/{projectId}/{experienceHashId}")
    @PUT
    fun edit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "发布详情", required = true)
        experience: ExperienceUpdate
    ): Result<Boolean>

    @Operation(summary = "下架体验")
    @Path("/{projectId}/{experienceHashId}/offline")
    @PUT
    fun offline(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取体验统计")
    @Path("/{projectId}/{experienceHashId}/downloadCount")
    @GET
    fun downloadCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<ExperienceCount>

    @Operation(summary = "获取体验用户统计")
    @Path("/{projectId}/{experienceHashId}/downloadUserCount")
    @GET
    fun downloadUserCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "页目", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ExperienceUserCount>>

    @Operation(summary = "获取外部下载链接")
    @Path("/{projectId}/{experienceHashId}/externalUrl")
    @GET
    fun externalUrl(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Url>

    @Operation(summary = "获取内部下载链接")
    @Path("/{projectId}/{experienceHashId}/downloadUrl")
    @GET
    fun downloadUrl(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Url>

    @Operation(summary = "列出外部用户列表")
    @Path("/outer/list")
    @GET
    fun outerList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<OuterSelectorVO>>

    @Operation(summary = "检查是否可以添加外部用户")
    @Path("/outer/canAdd")
    @POST
    fun outerCanAdd(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "入参")
        param: OuterCanAddParam
    ): Result<OuterCanAddVO>
}
