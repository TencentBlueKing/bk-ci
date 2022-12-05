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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
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

@Api(tags = ["USER_MARKET_ATOM"], description = "插件市场-插件")
@Path("/user/market/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAtomReleaseResource {

    @ApiOperation("插件工作台-新增插件")
    @POST
    @Path("/desk/atom/")
    fun addMarketAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件市场工作台-新增插件请求报文体", required = true)
        @Valid
        marketAtomCreateRequest: MarketAtomCreateRequest
    ): Result<String>

    @ApiOperation("插件工作台-升级插件")
    @PUT
    @Path("/desk/atom/")
    fun updateMarketAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("插件市场工作台-新增插件请求报文体", required = true)
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?>

    @ApiOperation("根据插件版本ID获取插件版本进度")
    @GET
    @Path("/desk/atom/release/process/{atomId}")
    fun getProcessInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<StoreProcessInfo>

    @ApiOperation("取消发布")
    @PathParam("atomId")
    @PUT
    @Path("/desk/atom/release/cancel/{atomId}")
    fun cancelRelease(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<Boolean>

    @ApiOperation("确认通过测试")
    @PathParam("atomId")
    @PUT
    @Path("/desk/atom/release/passTest/{atomId}")
    fun passTest(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomId", required = true)
        @PathParam("atomId")
        atomId: String
    ): Result<Boolean>

    @ApiOperation("下架插件")
    @PUT
    @Path("/desk/atom/offline/{atomCode}")
    fun offlineAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("下架请求报文")
        atomOfflineReq: AtomOfflineReq
    ): Result<Boolean>
}
