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
import com.tencent.devops.store.pojo.common.reason.Reason
import com.tencent.devops.store.pojo.common.reason.ReasonReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OP_STORE_REASON", description = "OP-STORE-原因")
@Path("/op/store/reason")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpReasonResource {

    @Operation(summary = "添加原因")
    @POST
    @Path("/types/{type}")
    fun add(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "原因类型", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @Parameter(description = "原因信息请求报文体", required = true)
        reasonReq: ReasonReq
    ): Result<Boolean>

    @Operation(summary = "更新原因信息")
    @PUT
    @Path("/types/{type}/ids/{id}")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @Parameter(description = "原因信息请求报文体", required = true)
        reasonReq: ReasonReq
    ): Result<Boolean>

    @Operation(summary = "启用禁用原因")
    @PUT
    @Path("/types/{type}/ids/{id}/enable")
    fun enableReason(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @Parameter(description = "是否启用", required = true)
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "获取原因列表")
    @GET
    @Path("/types/{type}/list")
    fun list(
        @Parameter(description = "类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @Parameter(description = "是否启用", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<List<Reason>>

    @Operation(summary = "删除原因")
    @DELETE
    @Path("/types/{type}/ids/{id}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
