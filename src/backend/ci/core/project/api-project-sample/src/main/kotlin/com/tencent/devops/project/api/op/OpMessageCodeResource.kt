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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.code.AddMessageCodeRequest
import com.tencent.devops.project.pojo.code.MessageCodeResp
import com.tencent.devops.project.pojo.code.UpdateMessageCodeRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_MESSAGE_CODE", description = "OP-返回码")
@Path("/op/message/codes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpMessageCodeResource {

    @Operation(summary = "获取返回码信息")
    @GET
    @Path("/")
    fun getMessageCodeDetails(
        @Parameter(description = "返回码", required = false)
        @QueryParam("messageCode")
        messageCode: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MessageCodeResp>

    @Operation(summary = "获取返回码信息")
    @GET
    @Path("/{messageCode}")
    fun getMessageCodeDetail(
        @Parameter(description = "返回码", required = true)
        @PathParam("messageCode")
        messageCode: String
    ): Result<MessageCodeDetail?>

    @Operation(summary = "刷新返回码在redis的缓存")
    @GET
    @Path("/{messageCode}/refresh")
    fun refreshMessageCodeCache(
        @Parameter(description = "返回码", required = true)
        @PathParam("messageCode")
        messageCode: String
    ): Result<Boolean>

    @Operation(summary = "新增返回码信息")
    @POST
    @Path("/")
    fun addMessageCodeDetail(
        @Parameter(description = "返回码新增请求报文体", required = true)
        addMessageCodeRequest: AddMessageCodeRequest
    ): Result<Boolean>

    @Operation(summary = "更新返回码信息")
    @PUT
    @Path("/{messageCode}")
    fun updateMessageCodeDetail(
        @Parameter(description = "返回码", required = true)
        @PathParam("messageCode")
        messageCode: String,
        @Parameter(description = "返回码更新请求报文体", required = true)
        updateMessageCodeRequest: UpdateMessageCodeRequest
    ): Result<Boolean>
}
