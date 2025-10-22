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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.platform.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.platform.StoreDockingPlatformRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
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

@Tag(name = "OP_STORE_DOCKING_PLATFORM", description = "OP-STORE-对接平台")
@Path("/op/store/docking/platforms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreDockingPlatformResource {

    @Operation(summary = "添加对接平台")
    @POST
    @Path("/add")
    fun add(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "对接平台请求报文体", required = true)
        @Valid
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Result<Boolean>

    @Operation(summary = "更新对接平台")
    @PUT
    @Path("/ids/{id}/update")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "平台ID", required = true)
        @PathParam("id")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE)
        id: String,
        @Parameter(description = "对接平台请求报文体", required = true)
        @Valid
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ): Result<Boolean>

    @Operation(summary = "获取对接平台信息列表")
    @GET
    @Path("/list")
    fun listPlatforms(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "平台名称", required = false)
        @QueryParam("platformName")
        @BkField(patternStyle = BkStyleEnum.NAME_STYLE, required = false)
        platformName: String? = null,
        @Parameter(description = "平台ID", required = false)
        @QueryParam("id")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE, required = false)
        id: String? = null,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = true)
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE, required = true)
        pageSize: Int = 10
    ): Result<Page<StoreDockingPlatformInfo>?>

    @Operation(summary = "根据ID删除标签信息")
    @DELETE
    @Path("/ids/{id}/delete")
    fun deletePlatformById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "平台ID", required = true)
        @PathParam("id")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE)
        id: String
    ): Result<Boolean>
}
