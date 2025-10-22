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
import com.tencent.devops.store.pojo.common.StoreReleaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreCreateResponse
import com.tencent.devops.store.pojo.common.publication.StoreOfflineRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STORE_RELEASE", description = "研发商店-发布管理")
@Path("/user/store/releases")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreReleaseResource {

    @Operation(summary = "新增组件")
    @POST
    @Path("/component/create")
    fun createComponent(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作台-新增组件请求报文体", required = true)
        @Valid
        storeCreateRequest: StoreCreateRequest
    ): Result<StoreCreateResponse?>

    @Operation(summary = "更新组件")
    @PUT
    @Path("/component/update")
    fun updateComponent(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作台-更新组件请求报文体", required = true)
        @Valid
        storeUpdateRequest: StoreUpdateRequest
    ): Result<StoreUpdateResponse?>

    @Operation(summary = "根据组件ID获取版本发布进度")
    @GET
    @Path("/components/{storeId}/process/info")
    fun getProcessInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeId", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<StoreProcessInfo>

    @Operation(summary = "取消版本发布")
    @PUT
    @Path("/components/{storeId}/cancel")
    fun cancelRelease(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeId", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<Boolean>

    @Operation(summary = "确认通过测试")
    @PUT
    @Path("/components/{storeId}/test/pass")
    fun passTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeId", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<Boolean>

    @Operation(summary = "填写信息")
    @PUT
    @Path("/components/{storeId}/release/info/edit")
    fun editReleaseInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "storeId", required = true)
        @PathParam("storeId")
        storeId: String,
        @Parameter(description = "填写信息请求报文体", required = true)
        @Valid
        storeReleaseInfoUpdateRequest: StoreReleaseInfoUpdateRequest
    ): Result<Boolean>

    @Operation(summary = "下架组件")
    @PUT
    @Path("/component/offline")
    fun offlineComponent(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "工作台-下架组件请求报文体", required = true)
        @Valid
        storeOfflineRequest: StoreOfflineRequest
    ): Result<Boolean>

    @Operation(summary = "重新构建")
    @PUT
    @Path("/components/{storeId}/rebuild")
    fun rebuild(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件Id", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<Boolean>

    @Operation(summary = "返回上一步")
    @PUT
    @Path("/components/{storeId}/step/back")
    fun back(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件Id", required = true)
        @PathParam("storeId")
        storeId: String
    ): Result<Boolean>
}
