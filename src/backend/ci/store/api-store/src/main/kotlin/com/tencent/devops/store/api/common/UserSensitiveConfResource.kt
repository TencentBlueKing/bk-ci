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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.store.pojo.common.SensitiveConfReq
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_SENSITIVECONF"], description = "敏感数据配置")
@Path("/user/market/{storeType}/component/{storeCode}/sensitiveConf/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSensitiveConfResource {

    @ApiOperation("插件工作台-新增敏感数据")
    @POST
    @Path("/")
    fun create(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("插件工作台-新增敏感数据报文体", required = true)
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean>

    @ApiOperation("插件工作台-编辑敏感数据")
    @PUT
    @Path("/{id}")
    fun edit(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("敏感配置ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam("插件工作台-编辑敏感数据报文体", required = true)
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean>

    @ApiOperation("删除敏感数据")
    @DELETE
    @Path("/")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("敏感数据ID集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("ids")
        ids: String
    ): Result<Boolean>

    @ApiOperation("获取敏感数据")
    @GET
    @Path("/{id}")
    fun get(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("ID", required = true)
        @PathParam("id")
        id: String
    ): Result<SensitiveConfResp?>

    @ApiOperation("获取敏感数据")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String
    ): Result<List<SensitiveConfResp>?>
}