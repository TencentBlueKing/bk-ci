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

package com.tencent.devops.store.api.extservice

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.MediaInfoReq
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_EXTENSION_MEDIA", description = "服务扩展_媒体信息")
@Path("/user/market/common/media")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtMediaResource {
    @Operation(summary = "添加媒体信息")
    @Path("/serviceCodes/{serviceCode}/media")
    @POST
    fun createServiceMedia(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务代码", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "评论信息", required = true)
        mediaInfoList: List<MediaInfoReq>
    ): Result<Boolean>

    @Operation(summary = "修改媒体信息")
    @Path("/ids/{mediaId}/")
    @PUT
    fun updateSericeMedia(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "媒体ID", required = true)
        @QueryParam("mediaId")
        mediaId: String,
        @Parameter(description = "扩展服务代码", required = true)
        @QueryParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "媒体信息", required = true)
        mediaInfoReq: MediaInfoReq
    ): Result<Boolean>

    @Operation(summary = "获取单条媒体信息")
    @Path("/ids/{mediaId}")
    @GET
    fun getServiceMedia(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "媒体ID", required = true)
        @PathParam("mediaId")
        mediaId: String
    ): Result<StoreMediaInfo?>

    @Operation(summary = "获取扩展服务所有媒体信息")
    @Path("/services/{serviceCode}")
    @GET
    fun getServiceMediaByServiceCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务编码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<List<StoreMediaInfo>?>
}
