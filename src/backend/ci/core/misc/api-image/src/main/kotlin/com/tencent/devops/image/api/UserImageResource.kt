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

package com.tencent.devops.image.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.pojo.ImageListResp
import com.tencent.devops.image.pojo.ImagePageData
import com.tencent.devops.image.pojo.UploadImageTask
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_IMAGE"], description = "镜像-镜像服务")
@Path("/user/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Suppress("ALL")
interface UserImageResource {
    @ApiOperation("上传镜像")
    @Path("/{projectId}/upload")
    @POST
    fun upload(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否是构建镜像", required = true)
        @QueryParam("isBuildImage")
        isBuildImage: Boolean?,
        @ApiParam("文件", required = true)
        @FormDataParam("file")
        inputStream: InputStream,
        @FormDataParam("file")
        disposition: FormDataContentDisposition
    ): Result<UploadImageTask>

    @ApiOperation("查询镜像上传状态")
    @Path("/{projectId}/queryUploadTask")
    @GET
    fun queryUploadTask(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("任务ID", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<UploadImageTask?>

    @ApiOperation("获取公共镜像列表")
    @Path("/listPublicImages")
    @GET
    fun listPublicImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam(value = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @ApiParam(value = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @ApiOperation("获取项目镜像列表")
    @Path("/{projectId}/listImages")
    @GET
    fun listProjectImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam(value = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @ApiParam(value = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @ApiOperation("获取所有项目镜像列表")
    @Path("/{projectId}/listAllProjectImages")
    @GET
    fun listAllProjectImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?
    ): Result<ImageListResp>

    @ApiOperation("获取项目构建镜像列表")
    @Path("/{projectId}/listBuildImages")
    @GET
    fun listProjectBuildImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "查询关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @ApiParam(value = "分页start", required = false)
        @QueryParam("start")
        start: Int?,
        @ApiParam(value = "分页大小", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<ImagePageData>

    @ApiOperation("获取项目Docker构建镜像列表")
    @Path("/{projectId}/listDockerBuildImages")
    @GET
    fun listDockerBuildImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DockerTag>>

    @ApiOperation("获取项目DevCloud构建镜像列表")
    @Path("/{projectId}/listDevCloudImages/{public}")
    @GET
    fun listDevCloudImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否公共镜像", required = true)
        @PathParam("public")
        public: Boolean
    ): Result<List<DockerTag>>

    @ApiOperation("获取镜像信息")
    @Path("/getImageInfo")
    @GET
    fun getImageInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "开始索引", required = false)
        @QueryParam("tagStart")
        tagStart: Int?,
        @ApiParam(value = "页大小", required = false)
        @QueryParam("tagLimit")
        tagLimit: Int?
    ): Result<DockerRepo?>

    @ApiOperation("获取构建镜像信息")
    @Path("/getTagInfo")
    @GET
    fun getTagInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<DockerTag?>

    @ApiOperation("镜像仓库支持升级为构建镜像")
    @Path("/{projectId}/setBuildImage")
    @POST
    fun setBuildImage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<Boolean>
}
