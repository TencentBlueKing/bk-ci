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

package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPEN_API_EXPERIENCE_V4", description = "OPEN-API-版本体验V4")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/experience/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList", "TooManyFunctions")
interface ApigwExperienceResourceV4 {
    
    @Operation(summary = "获取体验列表--v3", tags = ["v4_app_experience_listV3"])
    @Path("/list")
    @GET
    fun listV3(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "获取体验详情", tags = ["v4_app_experience_detail"])
    @Path("/{experienceHashId}/detail")
    @GET
    fun detail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "历史版本", tags = ["v4_app_experience_changeLog"])
    @Path("/{experienceHashId}/changeLog")
    @GET
    fun changeLog(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "创建外部直接下载链接", tags = ["v4_app_experience_downloadUrl"])
    @Path("/{experienceHashId}/downloadUrl")
    @POST
    fun downloadUrl(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "获取项目体验列表", tags = ["v4_app_experience_history"])
    @Path("/projects/{projectId}/history")
    @GET
    fun history(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "获取项目用户组信息和组所有人员", tags = ["v4_app_experience_projectGroupAndUsers"])
    @Path("/projects/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @Operation(summary = "获取上一次体验的参数", tags = ["v4_app_experience_lastParams"])
    @Path("/lastParams")
    @GET
    fun lastParams(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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

    @Operation(summary = "列出外部用户列表", tags = ["v4_app_experience_outerList"])
    @Path("/outer/list")
    @GET
    fun outerList(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<OuterSelectorVO>>

    @Operation(summary = "体验对应的安装包列表", tags = ["v4_app_experience_installPackages"])
    @Path("/{experienceHashId}/installPackages")
    @GET
    fun installPackages(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
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
