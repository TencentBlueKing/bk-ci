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
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.env.StoreEnvChangeLogInfo
import com.tencent.devops.store.pojo.common.env.StoreEnvVarInfo
import com.tencent.devops.store.pojo.common.env.StoreEnvVarRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STORE_ENV_VAR", description = "环境变量")
@Path("/user/store/env/var")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreEnvVarResource {

    @Operation(summary = "新增环境变量")
    @POST
    @Path("/create")
    fun create(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "环境变量请求报文体", required = true)
        @Valid
        storeEnvVarRequest: StoreEnvVarRequest
    ): Result<Boolean>

    @Operation(summary = "更新环境变量")
    @POST
    @Path("/update")
    fun update(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "变量ID", required = true)
        @BkField(patternStyle = BkStyleEnum.ID_STYLE)
        @QueryParam("variableId")
        variableId: String,
        @Parameter(description = "环境变量请求报文体", required = true)
        @Valid
        storeEnvVarRequest: StoreEnvVarRequest
    ): Result<Boolean>

    @Operation(summary = "删除环境变量")
    @DELETE
    @Path("/types/{storeType}/codes/{storeCode}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "生效范围 TEST：测试 PRD：正式 ALL：所有", required = true)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = true)
        scope: String,
        @Parameter(description = "环境变量名称集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("varNames")
        @BkField(patternStyle = BkStyleEnum.COMMON_STYLE)
        varNames: String
    ): Result<Boolean>

    @Operation(summary = "获取最新的环境变量列表")
    @GET
    @Path("/latest/types/{storeType}/codes/{storeCode}")
    fun getLatestEnvVarList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "生效范围 TEST：测试 PRD：正式 ALL：所有", required = false)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = false)
        scope: String?,
        @Parameter(description = "变量名", required = false)
        @QueryParam("varName")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE, required = false)
        varName: String?
    ): Result<List<StoreEnvVarInfo>?>

    @Operation(summary = "获取环境变量变更记录列表")
    @GET
    @Path("/change/log/types/{storeType}/codes/{storeCode}/vars/{varName}")
    fun getEnvVarChangeLogList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "生效范围 TEST：测试 PRD：正式 ALL：所有", required = true)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = true)
        scope: String,
        @Parameter(description = "变量名", required = true)
        @PathParam("varName")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        varName: String
    ): Result<List<StoreEnvChangeLogInfo>?>
}
