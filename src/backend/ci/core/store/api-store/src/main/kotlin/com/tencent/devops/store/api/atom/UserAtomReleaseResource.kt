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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
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
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_ATOM", description = "插件市场-插件")
@Path("/user/market/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAtomReleaseResource {

    @Operation(summary = "插件工作台-新增插件")
    @POST
    @Path("/desk/atom/")
    fun addMarketAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件市场工作台-新增插件请求报文体", required = true)
        @Valid
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<String>

    @Operation(summary = "插件工作台-升级插件")
    @PUT
    @Path("/desk/atom/")
    fun updateMarketAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件市场工作台-新增插件请求报文体", required = true)
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?>

    @Operation(summary = "根据插件版本ID获取插件版本进度")
    @GET
    @Path("/desk/atom/release/process/{atomId}")
    fun getProcessInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<StoreProcessInfo>

    @Operation(summary = "取消发布")
    @PathParam("atomId")
    @PUT
    @Path("/desk/atom/release/cancel/{atomId}")
    fun cancelRelease(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<Boolean>

    @Operation(summary = "确认通过测试")
    @PathParam("atomId")
    @PUT
    @Path("/desk/atom/release/passTest/{atomId}")
    fun passTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<Boolean>

    @Operation(summary = "下架插件")
    @PUT
    @Path("/desk/atom/offline/{atomCode}")
    fun offlineAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "下架请求报文")
        atomOfflineReq: AtomOfflineReq
    ): Result<Boolean>
}
