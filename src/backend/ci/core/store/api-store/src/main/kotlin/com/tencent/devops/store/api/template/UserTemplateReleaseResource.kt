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

package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "USER_MARKET_TEMPLATE", description = "模板")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTemplateReleaseResource {

    @Operation(summary = "关联模板")
    @POST
    @Path("/templates/{templateCode}/store/rel")
    fun addMarketTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "关联模板请求报文体", required = true)
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean>

    @Operation(summary = "上架模板")
    @PUT
    @Path("/desk/template/release")
    fun updateMarketTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "上架模板请求报文体", required = true)
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?>

    @Operation(summary = "根据模板版本ID获取模板版本进度")
    @GET
    @Path("/desk/template/release/process/{templateId}")
    fun getProcessInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "templateId", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<StoreProcessInfo>

    @Operation(summary = "取消发布")
    @PUT
    @Path("/desk/template/release/cancel/templateIds/{templateId}")
    fun cancelRelease(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "templateId", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>

    @Operation(summary = "下架模板")
    @PUT
    @Path("/desk/template/offline/templateCodes/{templateCode}/versions")
    fun offlineTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "原因", required = false)
        @QueryParam("reason")
        reason: String?
    ): Result<Boolean>
}
