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
package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.common.version.VersionInfo
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.response.ImageDetail
import com.tencent.devops.store.pojo.image.response.MarketImageMain
import com.tencent.devops.store.pojo.image.response.MarketImageResp
import com.tencent.devops.store.pojo.image.response.MyImage
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_IMAGE", description = "研发商店-镜像")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserMarketImageResource {
    @Operation(summary = "镜像市场首页")
    @GET
    @Path("/image/list/main")
    fun mainPageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketImageMain>>

    @Operation(summary = "镜像市场搜索镜像")
    @GET
    @Path("/image/list")
    fun searchImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "镜像来源", required = false)
        @QueryParam("imageSourceType")
        imageSourceType: ImageType?,
        @Parameter(description = "镜像分类编码", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @Parameter(description = "镜像研发来源", required = false)
        @QueryParam("rdType")
        rdType: ImageRDTypeEnum?,
        @Parameter(description = "镜像标签代码", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "镜像评分", required = false)
        @QueryParam("score")
        score: Int?,
        @Parameter(description = "镜像排序字段", required = false)
        @QueryParam("sortType")
        sortType: MarketImageSortTypeEnum?,
        @Parameter(description = "是否推荐， TRUE：是 FALSE：不是", required = false)
        @QueryParam("recommendFlag")
        recommendFlag: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketImageResp>

    @Operation(summary = "根据ID查询镜像详情")
    @GET
    @Path("/image/imageIds/{imageId}")
    fun getImageDetailById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<ImageDetail>

    @Operation(summary = "根据code查询镜像详情")
    @GET
    @Path("/image/imageCodes/{imageCode}")
    fun getImageDetailByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<ImageDetail>

    @Operation(summary = "查询镜像版本列表")
    @GET
    @Path("/image/imageCodes/{imageCode}/version/list")
    fun getImageVersionListByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ImageDetail>>

    @Operation(summary = "我的镜像列表")
    @GET
    @Path("/desk/image/list")
    fun getMyImageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像名称", required = false)
        @QueryParam("imageName")
        imageName: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<MyImage>>

    @Operation(summary = "删除镜像")
    @DELETE
    @Path("/image/imageCodes/{imageCode}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Code", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<Boolean>

    @Operation(summary = "更新流水线镜像信息")
    @PUT
    @Path("/baseInfo/images/{imageCode}")
    fun updateImageBaseInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像代码 ", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "镜像基本信息修改请求报文体", required = true)
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "根据镜像代码获取对应的版本列表信息")
    @GET
    @Path("/projectCodes/{projectCode}/imageCodes/{imageCode}/version/list")
    fun getPipelineImageVersions(
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<List<VersionInfo>>
}
