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
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfReq
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
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

@Tag(name = "USER_MARKET_SENSITIVECONF", description = "敏感数据配置")
@Path("/user/market/{storeType}/component/{storeCode}/sensitiveConf/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSensitiveConfResource {

    @Operation(summary = "插件工作台-新增敏感数据")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "插件工作台-新增敏感数据报文体", required = true)
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean>

    @Operation(summary = "插件工作台-编辑敏感数据")
    @PUT
    @Path("/{id}")
    fun edit(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "敏感配置ID", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "插件工作台-编辑敏感数据报文体", required = true)
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean>

    @Operation(summary = "删除敏感数据")
    @DELETE
    @Path("/")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "敏感数据ID集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("ids")
        ids: String
    ): Result<Boolean>

    @Operation(summary = "获取敏感数据")
    @GET
    @Path("/{id}")
    fun get(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "ID", required = true)
        @PathParam("id")
        id: String
    ): Result<SensitiveConfResp?>

    @Operation(summary = "获取敏感数据")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "字段类型集合，用\",\"分隔进行拼接（如BACKEND,FRONTEND,ALL）", required = false)
        @QueryParam("types")
        types: String?
    ): Result<List<SensitiveConfResp>?>
}
