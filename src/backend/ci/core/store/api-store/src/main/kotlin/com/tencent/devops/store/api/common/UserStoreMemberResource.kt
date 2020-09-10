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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STORE_MEMBER"], description = "STORE-组件-用户")
@Path("/user/market/desk/store/member/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreMemberResource {

    @ApiOperation("获取store组件成员列表")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @ApiParam("store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<List<StoreMemberItem?>>

    @ApiOperation("添加store组件成员")
    @POST
    @Path("/add")
    fun add(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("添加成员请求报文")
        @Valid
        storeMemberReq: StoreMemberReq
    ): Result<Boolean>

    @ApiOperation("删除store组件成员")
    @DELETE
    @Path("/delete")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("成员ID", required = true)
        @QueryParam("id")
        id: String,
        @ApiParam("store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @ApiParam("store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<Boolean>

    @ApiOperation("查看store组件成员信息")
    @GET
    @Path("/view")
    fun view(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @ApiParam("store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<StoreMemberItem?>

    @ApiOperation("修改store组件成员的调试项目")
    @PUT
    @Path("/test/project/change")
    fun changeMemberTestProjectCode(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("store组件成员", required = true)
        @QueryParam("storeMember")
        storeMember: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("store组件标识", required = true)
        @QueryParam("storeCode")
        storeCode: String,
        @ApiParam("store组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum
    ): Result<Boolean>
}