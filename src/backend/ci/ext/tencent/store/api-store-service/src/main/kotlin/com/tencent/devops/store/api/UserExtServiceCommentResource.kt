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

package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_SERVICE_COMMENT"], description = "服务扩展_评论")
@Path("/user/market/service/comment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceCommentResource {

    @ApiOperation("添加评论")
    @Path("/serviceIds/{serviceId}/serviceCodes/{serviceCodes}/comment")
    @POST
    fun createServiceComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务ID", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam("扩展服务代码", required = true)
        @PathParam("serviceCodes")
        serviceCodes: String,
        @ApiParam("评论信息", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?>

    @ApiOperation("修改评论")
    @Path("/comments/{commentId}")
    @PUT
    fun updateServiceComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String,
        @ApiParam("评论信息", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean>

    @ApiOperation("获取单条评论")
    @Path("/comments/{commentId}")
    @GET
    fun getServiceComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<StoreCommentInfo?>

    @ApiOperation("获取扩展服务所有评论")
    @Path("/serviceCodes/{serviceCode}/comments")
    @GET
    fun getServiceCommentByServiceCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务编码", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?>

    @ApiOperation("获取扩展服务的评分详情")
    @GET
    @Path("/score/serviceCodes/{serviceCode}")
    fun getServiceCommentScoreInfo(
        @ApiParam("扩展服务代码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<StoreCommentScoreInfo>

    @ApiOperation("评论点赞")
    @PUT
    @Path("/praise/{commentId}")
    fun updateStoreCommentPraiseCount(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<Int>
}
