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
import com.tencent.devops.store.pojo.common.member.StoreMemberItem
import com.tencent.devops.store.pojo.common.member.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_MEMBER", description = "OP-组件-用户")
@Path("/op/store/member/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreMemberResource {

    @Operation(summary = "获取store组件成员列表")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @Parameter(description = "store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<List<StoreMemberItem?>>

    @Operation(summary = "添加store组件成员")
    @POST
    @Path("/add")
    fun add(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "调试项目", required = true)
        @QueryParam("testProjectCode")
        testProjectCode: String?,
        @Parameter(description = "添加成员请求报文")
        @Valid
        storeMemberReq: StoreMemberReq
    ): Result<Boolean>

    @Operation(summary = "删除store组件成员")
    @DELETE
    @Path("/delete")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "成员ID", required = true)
        @QueryParam("id")
        id: String,
        @Parameter(description = "store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @Parameter(description = "store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<Boolean>

    @Operation(summary = "查看store组件成员信息")
    @GET
    @Path("/view")
    fun view(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "store组件成员", required = true)
        @QueryParam("member")
        member: String,
        @Parameter(description = "store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @Parameter(description = "store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<StoreMemberItem?>
}
