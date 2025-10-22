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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.media.MediaInfoReq
import com.tencent.devops.store.pojo.common.media.StoreMediaInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
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
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_COMMON_MEDIA", description = "研发商店_媒体信息")
@Path("/op/store/media")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpMediaResource {

    @Operation(summary = "提交媒体信息")
    @Path("/storeCodes/{storeCode}/types/{storeType}/media")
    @POST
    fun createStoreMedia(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "研发商店代码", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类别", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "评论信息", required = true)
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean>

    @Operation(summary = "获取单条媒体信息")
    @Path("/ids/{mediaId}")
    @GET
    fun getStoreMedia(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "媒体ID", required = true)
        @PathParam("mediaId")
        mediaId: String
    ): Result<StoreMediaInfo?>

    @Operation(summary = "获取扩展服务所有媒体信息")
    @Path("/storesCodes/{storeCode}/types/{labelType}")
    @GET
    fun getStoreMediaByStoreCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "研发商店编码", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum
    ): Result<List<StoreMediaInfo>?>
}
