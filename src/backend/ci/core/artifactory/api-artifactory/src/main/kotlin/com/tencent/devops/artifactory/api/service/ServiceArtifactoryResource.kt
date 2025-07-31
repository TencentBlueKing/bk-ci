/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.Count
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.artifactory.pojo.FileDetail
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ARTIFACTORY_BASIC", description = "仓库-文件管理")
@Path("/service/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceArtifactoryResource {

    @Operation(summary = "OpenAPI调用创建用户下载链接（持久）")
    @Path("/projects/{projectId}/{artifactoryType}/downloadUrl")
    @POST
    fun downloadUrlForOpenApi(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @Operation(summary = "检测文件是否存在")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/check")
    @Path("/{projectId}/{artifactoryType}/check")
    @GET
    fun check(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @Operation(summary = "夸项目拷贝文件")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/acrossProjectCopy")
    @Path("/{projectId}/{artifactoryType}/acrossProjectCopy")
    @POST
    fun acrossProjectCopy(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String,
        @Parameter(description = "目标项目", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "目标路径", required = true)
        @QueryParam("targetPath")
        targetPath: String
    ): Result<Count>

    @Operation(summary = "检测文件是否存在")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/properties")
    @Path("/{projectId}/{artifactoryType}/properties")
    @GET
    fun properties(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<List<Property>>

    @Operation(summary = "外部下载链接")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/externalUrl")
    @Path("/{projectId}/{artifactoryType}/externalUrl")
    @GET
    fun externalUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "创建用户", required = false)
        @QueryParam("creatorId")
        creatorId: String?,
        @Parameter(description = "下载用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String,
        @Parameter(description = "有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int,
        @Parameter(description = "是否直接对应下载链接(false情况下ipa会换成plist下载链接)", required = false)
        @QueryParam("directed")
        directed: Boolean?
    ): Result<Url>

    @Operation(summary = "APP构件跳转链接")
    @Path("/{projectId}/{artifactoryType}/appDownloadUrl")
    @GET
    fun appDownloadUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "下载用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @Operation(summary = "创建内部链接")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/downloadUrl")
    @Path("/{projectId}/{artifactoryType}/downloadUrl")
    @GET
    fun downloadUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "下载用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String,
        @Parameter(description = "有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int,
        @Parameter(description = "是否直接对应下载链接(false情况下ipa会换成plist下载链接)", required = false)
        @QueryParam("directed")
        directed: Boolean?
    ): Result<Url>

    @Operation(summary = "获取文件信息")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/show")
    @Path("/{projectId}/{artifactoryType}/show")
    @GET
    fun show(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<FileDetail>

    @Operation(summary = "根据元数据获取文件(有排序),searchProps条件为and")
    // @Path("/projects/{projectId}/search")
    @Path("/{projectId}/search")
    @POST
    fun search(
        @Parameter(description = "用户ID", required = false)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条(不传默认全部返回)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "元数据", required = true)
        searchProps: List<Property>
    ): Result<FileInfoPage<FileInfo>>

    @Operation(summary = "获取匹配到的自定义仓库文件")
    // @Path("/projects/{projectId}/searchCustomFiles")
    @Path("/{projectId}/searchCustomFiles")
    @POST
    fun searchCustomFiles(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询条件", required = true)
        condition: CustomFileSearchCondition
    ): Result<List<String>>

    @Operation(summary = "获取自定义报告根目录Url")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/report/root")
    @GET
    fun getReportRootUrl(
        @Parameter(description = "项目的英文名", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<String>

    @Operation(summary = "根据元数据获取文件")
    @Path("/projects/{projectId}/search")
    @POST
    fun searchFile(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条(不传默认全部返回)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "元数据", required = true)
        searchProps: SearchProps
    ): Result<Page<FileInfo>>

    @Operation(summary = "获取匹配到的自定义仓库文件")
    @Path("/{projectId}/listCustomFiles")
    @POST
    fun listCustomFiles(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("fullPath")
        fullPath: String,
        @Parameter(description = "是否包含文件夹", required = false, example = "true")
        @QueryParam("includeFolder")
        includeFolder: Boolean?,
        @Parameter(description = "是否深度查询文件", required = false, example = "false")
        @QueryParam("deep")
        deep: Boolean?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条(不传默认全部返回)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "是否按modifiedTime倒序排列", required = false, example = "false")
        @QueryParam("modifiedTimeDesc")
        modifiedTimeDesc: Boolean?
    ): Result<Page<FileInfo>>

    @Operation(summary = "获取文件内容")
    @GET
    @Path("/file/content")
    fun getFileContent(
        @Parameter(description = "仓库项目", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "仓库名称", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String
    ): Result<String>

    @Operation(summary = "获取路径下的文件名称列表")
    @GET
    @Path("/fileNames/list")
    fun listFileNamesByPath(
        @Parameter(description = "仓库项目", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "仓库名称", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String
    ): Result<List<String>>
}
