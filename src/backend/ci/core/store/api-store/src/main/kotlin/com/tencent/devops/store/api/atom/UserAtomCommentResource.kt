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

package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "USER_MARKET_ATOM_COMMENT", description = "插件-插件评论")
@Path("/user/market/atom/comment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAtomCommentResource {

    @Operation(summary = "获取插件评论接口")
    @GET
    @Path("/comments/{commentId}")
    fun getStoreComment(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<StoreCommentInfo?>

    @Operation(summary = "获取插件的评论列表")
    @GET
    @Path("/atomCodes/{atomCode}/comments")
    fun getStoreComments(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?>

    @Operation(summary = "获取插件的评分详情")
    @GET
    @Path("/score/atomCodes/{atomCode}")
    fun getAtomCommentScoreInfo(
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        templateCode: String
    ): Result<StoreCommentScoreInfo>

    @Operation(summary = "新增插件评论")
    @POST
    @Path("/atomIds/{atomId}/atomCodes/{atomCode}/comment")
    fun addAtomComment(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @Parameter(description = "插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "评论信息请求报文体", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?>

    @Operation(summary = "更新插件评论")
    @PUT
    @Path("/comments/{commentId}")
    fun updateStoreComment(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "评论ID", required = true)
        @PathParam("commentId")
        commentId: String,
        @Parameter(description = "评论信息请求报文体", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean>

    @Operation(summary = "评论点赞")
    @PUT
    @Path("/praise/{commentId}")
    fun updateStoreCommentPraiseCount(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<Int>
}
