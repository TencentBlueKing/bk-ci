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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.StoreEnvChangeLogInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarInfo
import com.tencent.devops.store.pojo.common.StoreEnvVarRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.PathParam
import javax.ws.rs.GET
import javax.ws.rs.DELETE
import javax.ws.rs.QueryParam
import javax.ws.rs.HeaderParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STORE_ENV_VAR"], description = "环境变量")
@Path("/user/store/env/var")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreEnvVarResource {

    @ApiOperation("新增环境变量")
    @POST
    @Path("/create")
    fun create(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("环境变量请求报文体", required = true)
        @Valid
        storeEnvVarRequest: StoreEnvVarRequest
    ): Result<Boolean>

    @ApiOperation("更新环境变量")
    @POST
    @Path("/update")
    fun update(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("变量ID", required = true)
        @BkField(patternStyle = BkStyleEnum.ID_STYLE)
        @QueryParam("variableId")
        variableId: String,
        @ApiParam("环境变量请求报文体", required = true)
        @Valid
        storeEnvVarRequest: StoreEnvVarRequest
    ): Result<Boolean>

    @ApiOperation("删除环境变量")
    @DELETE
    @Path("/types/{storeType}/codes/{storeCode}")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @ApiParam("生效范围 TEST：测试 PRD：正式 ALL：所有", required = true)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = true)
        scope: String,
        @ApiParam("环境变量名称集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("varNames")
        @BkField(patternStyle = BkStyleEnum.COMMON_STYLE)
        varNames: String
    ): Result<Boolean>

    @ApiOperation("获取最新的环境变量列表")
    @GET
    @Path("/latest/types/{storeType}/codes/{storeCode}")
    fun getLatestEnvVarList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @ApiParam("生效范围 TEST：测试 PRD：正式 ALL：所有", required = false)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = false)
        scope: String?,
        @ApiParam("变量名", required = false)
        @QueryParam("varName")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE, required = false)
        varName: String?
    ): Result<List<StoreEnvVarInfo>?>

    @ApiOperation("获取环境变量变更记录列表")
    @GET
    @Path("/change/log/types/{storeType}/codes/{storeCode}/vars/{varName}")
    fun getEnvVarChangeLogList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @ApiParam("生效范围 TEST：测试 PRD：正式 ALL：所有", required = true)
        @QueryParam("scope")
        @BkField(patternStyle = BkStyleEnum.SCOPE_STYLE, required = true)
        scope: String,
        @ApiParam("变量名", required = true)
        @PathParam("varName")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        varName: String
    ): Result<List<StoreEnvChangeLogInfo>?>
}
