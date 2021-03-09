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
import com.tencent.devops.store.pojo.common.Reason
import com.tencent.devops.store.pojo.common.ReasonReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
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

@Api(tags = ["OP_STORE_REASON"], description = "OP-STORE-原因")
@Path("/op/store/reason")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpReasonResource {

    @ApiOperation("添加原因")
    @POST
    @Path("/types/{type}")
    fun add(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("原因类型", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @ApiParam(value = "原因信息请求报文体", required = true)
        reasonReq: ReasonReq
    ): Result<Boolean>

    @ApiOperation("更新原因信息")
    @PUT
    @Path("/types/{type}/ids/{id}")
    fun update(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String,
        @ApiParam("类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @ApiParam(value = "原因信息请求报文体", required = true)
        reasonReq: ReasonReq
    ): Result<Boolean>

    @ApiOperation("启用禁用原因")
    @PUT
    @Path("/types/{type}/ids/{id}/enable")
    fun enableReason(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String,
        @ApiParam("类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @ApiParam(value = "是否启用", required = true)
        enable: Boolean
    ): Result<Boolean>

    @ApiOperation("获取原因列表")
    @GET
    @Path("/types/{type}/list")
    fun list(
        @ApiParam("类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum,
        @ApiParam("是否启用", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<List<Reason>>

    @ApiOperation("删除原因")
    @DELETE
    @Path("/types/{type}/ids/{id}")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
