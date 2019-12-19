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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OfflineMarketImageReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE"], description = "研发商店-镜像")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageReleaseResource {

    @ApiOperation("关联镜像")
    @POST
    @Path("/image/imageCodes/{imageCode}/store/rel")
    fun addMarketImage(
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("关联镜像请求报文体", required = true)
        marketImageRelRequest: MarketImageRelRequest
    ): Result<String>

    @ApiOperation("上架/升级镜像")
    @PUT
    @Path("/desk/image/release")
    fun updateMarketImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("上架镜像请求报文体", required = true)
        marketImageUpdateRequest: MarketImageUpdateRequest
    ): Result<String?>

    @ApiOperation("下架镜像")
    @PUT
    @Path("/desk/image/offline/imageCodes/{imageCode}/versions")
    fun offlineMarketImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Code", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("下架镜像请求报文体", required = true)
        offlineMarketImageReq: OfflineMarketImageReq
    ): Result<Boolean>

    @ApiOperation("根据镜像ID获取镜像版本进度")
    @GET
    @Path("/desk/image/release/process/imageIds/{imageId}")
    fun getProcessInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<StoreProcessInfo>

    @ApiOperation("取消发布镜像")
    @PUT
    @Path("/desk/image/release/cancel/imageIds/{imageId}")
    fun cancelRelease(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @ApiOperation("重新验证镜像")
    @PUT
    @Path("/desk/image/release/recheck/imageIds/{imageId}")
    fun recheck(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>

    @ApiOperation("确认镜像通过测试")
    @PUT
    @Path("/desk/image/release/passTest/imageIds/{imageId}")
    fun passTest(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像Id", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<Boolean>
}