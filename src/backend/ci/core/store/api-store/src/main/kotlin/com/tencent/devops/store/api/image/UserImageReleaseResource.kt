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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OfflineMarketImageReq
import com.tencent.devops.store.pojo.image.response.ImageAgentTypeInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_IMAGE", description = "研发商店-镜像")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageReleaseResource {

    @Operation(summary = "关联镜像")
    @POST
    @Path("/image/imageCodes/{imageCode}/store/rel")
    fun addMarketImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像代码", required = true)
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "关联镜像请求报文体", required = true)
        @Valid
        marketImageRelRequest: MarketImageRelRequest
    ): Result<String>

    @Operation(summary = "上架/升级镜像")
    @PUT
    @Path("/desk/image/release")
    fun updateMarketImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "上架镜像请求报文体", required = true)
        marketImageUpdateRequest: MarketImageUpdateRequest
    ): Result<String?>

    @Operation(summary = "下架镜像")
    @PUT
    @Path("/desk/image/offline/imageCodes/{imageCode}/versions")
    fun offlineMarketImage(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Code", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @Parameter(description = "下架镜像请求报文体", required = true)
        offlineMarketImageReq: OfflineMarketImageReq
    ): Result<Boolean>

    @Operation(summary = "根据镜像ID获取镜像版本进度")
    @GET
    @Path("/desk/image/release/process/imageIds/{imageId}")
    fun getProcessInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<StoreProcessInfo>

    @Operation(summary = "取消发布镜像")
    @PUT
    @Path("/desk/image/release/cancel/imageIds/{imageId}")
    fun cancelRelease(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "重新验证镜像")
    @PUT
    @Path("/desk/image/release/recheck/imageIds/{imageId}")
    fun recheck(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "确认镜像通过测试")
    @PUT
    @Path("/desk/image/release/passTest/imageIds/{imageId}")
    fun passTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @Operation(summary = "获取镜像支持的机器类型列表")
    @GET
    @Path("/image/agentType/list")
    fun getImageAgentTypes(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ImageAgentTypeInfo>>
}
