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

package com.tencent.devops.image.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.pojo.ImageListResp
import com.tencent.devops.image.pojo.ImagePageData
import com.tencent.devops.image.pojo.UploadImageTask
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_IMAGE", description = "镜像-镜像服务")
@Path("/user/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Suppress("ALL")
interface UserImageResource {
    @Operation(summary = "上传镜像")
    @Path("/{projectId}/upload")
    @POST
    fun upload(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "是否是构建镜像", required = true)
        @QueryParam("isBuildImage")
        isBuildImage: Boolean?,
        @Parameter(description = "文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<UploadImageTask>

    @Operation(summary = "查询镜像上传状态")
    @Path("/{projectId}/queryUploadTask")
    @GET
    fun queryUploadTask(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "任务ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<UploadImageTask?>

    @Operation(summary = "获取公共镜像列表")
    @Path("/listPublicImages")
    @GET
    fun listPublicImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @Parameter(description = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @Parameter(description = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @Operation(summary = "获取项目镜像列表")
    @Path("/{projectId}/listImages")
    @GET
    fun listProjectImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @Parameter(description = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @Parameter(description = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @Operation(summary = "获取所有项目镜像列表")
    @Path("/{projectId}/listAllProjectImages")
    @GET
    fun listAllProjectImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?
    ): Result<ImageListResp>

    @Operation(summary = "获取项目构建镜像列表")
    @Path("/{projectId}/listBuildImages")
    @GET
    fun listProjectBuildImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @Parameter(description = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @Parameter(description = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @Operation(summary = "获取项目Docker构建镜像列表")
    @Path("/{projectId}/listDockerBuildImages")
    @GET
    fun listDockerBuildImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DockerTag>>

    @Operation(summary = "获取项目DevCloud构建镜像列表")
    @Path("/{projectId}/listDevCloudImages/{public}")
    @GET
    fun listDevCloudImages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "是否公共镜像", required = true)
        @PathParam("public")
        public: Boolean
    ): Result<List<DockerTag>>

    @Operation(summary = "获取镜像信息")
    @Path("/getImageInfo")
    @GET
    fun getImageInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @Parameter(description = "开始索引", required = false)
        @QueryParam("tagStart")
        tagStart: Int?,
        @Parameter(description = "页大小", required = false)
        @QueryParam("tagLimit")
        tagLimit: Int?
    ): Result<DockerRepo?>

    @Operation(summary = "获取构建镜像信息")
    @Path("/getTagInfo")
    @GET
    fun getTagInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @Parameter(description = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<DockerTag?>

    @Operation(summary = "镜像仓库支持升级为构建镜像")
    @Path("/{projectId}/setBuildImage")
    @POST
    fun setBuildImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @Parameter(description = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<Boolean>
}
