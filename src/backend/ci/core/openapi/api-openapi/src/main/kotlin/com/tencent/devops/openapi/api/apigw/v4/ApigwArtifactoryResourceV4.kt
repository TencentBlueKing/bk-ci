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

package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_ARTIFACTORY_V4", description = "OPENAPI-构建产物资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/artifactories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwArtifactoryResourceV4 {

    @Operation(
        summary = "获取用户下载链接",
        tags = ["v4_app_artifactory_userDownloadUrl", "v4_user_artifactory_userDownloadUrl"]
    )
    @Path("/user_download_url")
    @GET
    fun getUserDownloadUrl(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @Operation(
        summary = "获取APP跳转链接",
        tags = ["v4_app_artifactory_appDownloadUrl", "v4_user_artifactory_appDownloadUrl"]
    )
    @Path("/app_download_url")
    @GET
    fun getAppDownloadUrl(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Url>

    @Operation(
        summary = "根据元数据获取文件(注意: 如果需要构建产物的下载url，请单独调用下载接口，如 v4_app_artifactory_userDownloadUrl)",
        tags = ["v4_app_artifactory_list", "v4_user_artifactory_list"]
    )
    @Path("/file_info")
    @GET
    fun search(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条(不传默认全部返回)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<FileInfo>>

    @Operation(
        summary = "下载熔断归档的全量日志（开源版暂未实现）",
        tags = ["v4_app_artifactory_log_download", "v4_user_artifactory_log_download"]
    )
    @GET
    @Path("/log")
    fun getPluginLogUrl(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线 ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建 ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "插件 elementId", required = true)
        @QueryParam("elementId")
        elementId: String,
        @Parameter(description = "执行序号", required = true)
        @QueryParam("executeCount")
        executeCount: String
    ): Result<Url>

    @Operation(summary = "查询自定义仓库文件信息", tags = ["v4_app_artifactory_custom", "v4_user_artifactory_custom"])
    @Path("/custom")
    @GET
    fun listCustomFiles(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
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
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "是否按modifiedTime倒序排列", required = false, example = "false")
        @QueryParam("modifiedTimeDesc")
        modifiedTimeDesc: Boolean?
    ): Result<Page<FileInfo>>
}
