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
import com.tencent.devops.store.pojo.common.logo.Logo
import com.tencent.devops.store.pojo.common.logo.StoreLogoInfo
import com.tencent.devops.store.pojo.common.logo.StoreLogoReq
import com.tencent.devops.store.pojo.common.enums.LogoTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_LOGO", description = "OP-STORE-LOGO")
@Path("/op/store/logo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreLogoResource {

    @Operation(summary = "上传logo")
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadStoreLogo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "contentLength", required = true)
        @HeaderParam("content-length")
        contentLength: Long,
        @Parameter(description = "是否限制图片尺寸范围", required = false)
        @QueryParam("sizeLimitFlag")
        sizeLimitFlag: Boolean? = null,
        @Parameter(description = "logo", required = true)
        @FormDataParam("logo")
        inputStream: InputStream,
        @FormDataParam("logo")
        disposition: FormDataContentDisposition
    ): Result<StoreLogoInfo?>

    @Operation(summary = "新增一条logo记录")
    @POST
    @Path("/type/{logoType}")
    fun add(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "logoType", required = true)
        @PathParam("logoType")
        logoType: LogoTypeEnum,
        @Parameter(description = "storeLogoReq", required = true)
        storeLogoReq: StoreLogoReq
    ): Result<Boolean>

    @Operation(summary = "更新一条logo记录")
    @PUT
    @Path("/{id}")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "storeLogoReq", required = true)
        storeLogoReq: StoreLogoReq
    ): Result<Boolean>

    @Operation(summary = "获取一条logo记录")
    @GET
    @Path("/{id}")
    fun get(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String
    ): Result<Logo?>

    @Operation(summary = "list logo记录")
    @GET
    @Path("/type/{logoType}")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "logoType", required = true)
        @PathParam("logoType")
        logoType: LogoTypeEnum
    ): Result<List<Logo>?>

    @Operation(summary = "删除一条logo记录")
    @DELETE
    @Path("/{id}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
