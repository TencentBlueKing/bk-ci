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

package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OpImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.OpImageResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_MARKET_IMAGE", description = "OP-流水线-镜像")
@Path("/op/market/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpImageResource {

    @Operation(summary = "新增镜像")
    @POST
    @Path("/")
    fun addImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "新增镜像请求报文体", required = true)
        imageCreateRequest: ImageCreateRequest
    ): Result<String>

    @Operation(summary = "确认镜像通过测试")
    @PUT
    @Path("/release/passTest/imageIds/{imageId}")
    fun passTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "重新验证镜像")
    @PUT
    @Path("/release/recheck/imageIds/{imageId}")
    fun recheck(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "更新镜像信息")
    @PUT
    @Path("/{imageId}")
    fun updateImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像ID", required = true)
        @PathParam("imageId")
        imageId: String,
        @Parameter(description = "更新镜像请求报文体", required = true)
        imageUpdateRequest: ImageUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "根据ID删除镜像信息")
    @DELETE
    @Path("/imageIds/{imageId}")
    fun deleteImageById(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像ID", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "根据ID获取镜像信息")
    @GET
    @Path("/imageIds/{imageId}")
    fun getImageById(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像ID", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<ImageDetail>

    @Operation(summary = "根据镜像代码获取镜像信息")
    @GET
    @Path("/imageCodes/{imageCode}")
    fun getImagesByCodeAndVersion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String?
    ): Result<ImageDetail>

    @Operation(summary = "根据镜像代码获取镜像版本列表")
    @GET
    @Path("/imageCodes/{imageCode}/versions/list")
    fun getImageVersionsByCode(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ImageDetail>>

    @Operation(summary = "下架镜像")
    @PUT
    @Path("/offline/imageCodes/{imageCode}/versions")
    fun offlineImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "原因", required = false)
        @QueryParam("reason")
        reason: String?
    ): Result<Boolean>

    @Operation(summary = "获取市场镜像")
    @GET
    @Path("/")
    fun listImages(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像名称", required = false)
        @QueryParam("imageName")
        imageName: String?,
        @Parameter(description = "镜像来源类型", required = false)
        @QueryParam("imageSourceType")
        imageSourceType: ImageType?,
        @Parameter(description = "是否处于流程中", required = false)
        @QueryParam("processFlag")
        processFlag: Boolean?,
        @Parameter(description = "镜像分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴", required = false)
        @QueryParam("categoryCodes")
        categoryCodes: String?,
        @Parameter(description = "功能标签", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: OpImageSortTypeEnum?,
        @Parameter(description = "是否降序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<OpImageResp>

    @Operation(summary = "审核镜像")
    @Path("/{imageId}/approve")
    @PUT
    fun approveImage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "ID", required = true)
        @PathParam("imageId")
        imageId: String,
        @Parameter(description = "审核镜像请求报文")
        approveImageReq: ApproveImageReq
    ): Result<Boolean>

    @Operation(summary = "查看可见范围")
    @GET
    @Path("/{imageCode}/visible")
    fun getVisibleDept(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<StoreVisibleDeptResp?>

    @Operation(summary = "查找蓝盾仓库关联镜像信息")
    @GET
    @Path("/repo/bk/names/{imageRepoName}")
    fun getBkRelImageInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像在仓库中的名称", required = true)
        @PathParam("imageRepoName")
        imageRepoName: String,
        @Parameter(description = "需要回显镜像tag的镜像ID", required = false)
        @QueryParam("imageId")
        imageId: String?
    ): Result<DockerRepo?>
}
