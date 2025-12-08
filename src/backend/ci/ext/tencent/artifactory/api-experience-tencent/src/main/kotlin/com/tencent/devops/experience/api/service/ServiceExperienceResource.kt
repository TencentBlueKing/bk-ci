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

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceInfoForBuild
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_EXPERIENCE", description = "版本体验-发布体验")
@Path("/service/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("artifactory")
interface ServiceExperienceResource {
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
        experience: ExperienceServiceCreate
    ): Result<String>

    @Operation(summary = "获取体验列表数量")
    @Path("/list/count")
    @POST
    fun count(
        @Parameter(description = "项目ID集合", required = false)
        projectIds: Set<String>? = setOf(),
        @Parameter(description = "", required = false)
        @QueryParam("expired")
        expired: Boolean? = false
    ): Result<Map<String, Int>>

    @Operation(summary = "获取体验详情")
    @Path("/projects/{projectId}/experienceIds/{experienceHashId}")
    @GET
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Experience>

    @Operation(summary = "是否有体验权限")
    @Path("/experienceIds/{experienceHashId}/check")
    @GET
    fun check(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "体验HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String?
    ): Result<Boolean>

    @Operation(summary = "通过bundleId获取公开体验跳转信息")
    @Path("/jumpInfo")
    @GET
    fun jumpInfo(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "bundleId", required = true)
        @QueryParam("bundleIdentifier")
        bundleIdentifier: String,
        @Parameter(description = "平台", required = true)
        @QueryParam("platform")
        platform: String
    ): Result<ExperienceJumpInfo>

    @Operation(summary = "获取构建中的体验的信息")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}")
    @GET
    fun listForBuild(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<ExperienceInfoForBuild>>

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
        userId: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Boolean>

    @Operation(summary = "上架体验")
    @Path("/{projectId}/{experienceHashId}/online")
    @PUT
    fun online(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "发布HashId", required = false)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取体验列表--v3")
    @Path("/v3/list")
    @GET
    fun listV3(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "平台", required = true)
        @QueryParam("platform")
        platform: Int,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null
    ): Result<ExperienceList>

    @Operation(summary = "获取体验详情")
    @Path("/experienceIds/{experienceHashId}/detail")
    @GET
    fun detail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "平台", required = true)
        @QueryParam("platform")
        platform: Int,
        @Parameter(description = "版本号", required = false)
        @QueryParam("appVersion")
        appVersion: String?,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "强制最新版本", required = false)
        @QueryParam("forceNew")
        forceNew: Boolean = true
    ): Result<AppExperienceDetail>

    @Operation(summary = "历史版本")
    @Path("/experienceIds/{experienceHashId}/changeLog")
    @GET
    fun changeLog(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String,
        @Parameter(description = "页目", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数目", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "是否展示所有版本", required = false)
        @QueryParam("showAll")
        showAll: Boolean?,
        @Parameter(description = "文件名", required = false)
        @QueryParam("name")
        name: String? = null,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String? = null,
        @Parameter(description = "版本描述", required = false)
        @QueryParam("remark")
        remark: String? = null,
        @Parameter(description = "体验发起时间--起始时间(秒级)", required = false)
        @QueryParam("createDateBegin")
        createDateBegin: Long? = null,
        @Parameter(description = "体验发起时间--终止时间(秒级)", required = false)
        @QueryParam("createDateEnd")
        createDateEnd: Long? = null,
        @Parameter(description = "体验结束时间--起始时间(秒级)", required = false)
        @QueryParam("endDateBegin")
        endDateBegin: Long? = null,
        @Parameter(description = "体验结束时间--终止时间(秒级)", required = false)
        @QueryParam("endDateEnd")
        endDateEnd: Long? = null,
        @Parameter(description = "发起人", required = false)
        @QueryParam("creator")
        creator: String? = null
    ): Result<Pagination<ExperienceChangeLog>>

    @Operation(summary = "创建外部直接下载链接")
    @Path("/experienceIds/{experienceHashId}/downloadUrl")
    @POST
    fun downloadUrl(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<DownloadUrl>

    @Operation(summary = "获取项目体验列表")
    @Path("/projects/{projectId}/history")
    @GET
    fun history(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("appVersion")
        appVersion: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<AppExperienceSummary>>

    @Operation(summary = "获取项目用户组信息和组所有人员")
    @Path("/projects/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @Operation(summary = "获取上一次体验的参数")
    @Path("/lastParams")
    @GET
    fun lastParams(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "构件名称", required = true)
        @QueryParam("name")
        name: String,
        @Parameter(description = "项目Id", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "bundleIdentifier", required = true)
        @QueryParam("bundleIdentifier")
        bundleIdentifier: String
    ): Result<ExperienceLastParams>

    @Operation(summary = "列出外部用户列表")
    @Path("/outer/list")
    @GET
    fun outerList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<OuterSelectorVO>>

    @Operation(summary = "体验对应的安装包列表")
    @Path("/experienceIds/{experienceHashId}/installPackages")
    @GET
    fun installPackages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "平台", required = true)
        @QueryParam("platform")
        platform: Int,
        @Parameter(description = "版本号", required = false)
        @QueryParam("appVersion")
        appVersion: String?,
        @Parameter(description = "组织", required = false)
        @QueryParam("organization")
        organization: String? = null,
        @Parameter(description = "体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<Pagination<AppExperienceInstallPackage>>
}
