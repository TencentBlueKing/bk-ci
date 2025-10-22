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

package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.artifactory.pojo.GetFileDownloadUrlsResponse
import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_ARTIFACTORY", description = "仓库-文件管理")
@Path("/build/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)@Suppress("ALL")
interface BuildFileResource {

    @Operation(summary = "下载文件")
    @GET
    @Path("/file/download")
    fun downloadFile(
        @Parameter(description = "projectCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectCode: String,
        @Parameter(description = "pipelineId", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "归档文件")
    @POST
    @Path("/file/archive")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun archiveFile(
        @Parameter(description = "projectCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectCode: String,
        @Parameter(description = "pipelineId", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "buildId", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "文件类型", required = true)
        @QueryParam("fileType")
        fileType: FileTypeEnum,
        @Parameter(description = "自定义文件路径", required = false)
        @QueryParam("customFilePath")
        customFilePath: String?,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<String?>

    @Operation(summary = "下载归档文件")
    @GET
    @Path("/file/archive/download")
    fun downloadArchiveFile(
        @Parameter(description = "projectCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectCode: String,
        @Parameter(description = "pipelineId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @Parameter(description = "buildId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "文件类型", required = true)
        @QueryParam("fileType")
        fileType: FileTypeEnum,
        @Parameter(description = "自定义文件路径", required = true)
        @QueryParam("customFilePath")
        customFilePath: String,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "获取文件下载url列表")
    @GET
    @Path("/pipeline/{pipelineId}/build/{buildId}/file/download/urls/get")
    fun getFileDownloadUrls(
        @Parameter(description = "projectCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectCode: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "文件类型", required = true)
        @QueryParam("fileType")
        fileType: FileTypeEnum,
        @Parameter(description = "自定义文件路径", required = false)
        @QueryParam("customFilePath")
        customFilePath: String?
    ): Result<GetFileDownloadUrlsResponse?>
}
