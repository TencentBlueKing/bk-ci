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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.comment.StoreCommentInfo
import com.tencent.devops.store.pojo.common.comment.StoreCommentRequest
import com.tencent.devops.store.pojo.common.comment.StoreCommentScoreInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "USER_STORE_COMMENT", description = "研发商店-组件评论")
@Path("/user/store/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreCommentResource {

    @Operation(summary = "获取组件评论接口")
    @GET
    @Path("/{commentId}/get")
    fun getStoreComment(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "评论ID", required = true)
        @PathParam("commentId")
        @BkField(patternStyle = BkStyleEnum.ID_STYLE)
        commentId: String
    ): Result<StoreCommentInfo?>

    @Operation(summary = "获取组件的评论列表")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/list")
    fun getStoreComments(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        @BkField(patternStyle = BkStyleEnum.NUMBER_STYLE)
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE)
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?>

    @Operation(summary = "获取组件的评分详情")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/score/get")
    fun getStoreCommentScoreInfo(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeType: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String
    ): Result<StoreCommentScoreInfo>

    @Operation(summary = "新增组件评论")
    @POST
    @Path("/ids/{storeId}/codes/{storeCode}/add")
    fun addStoreComment(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件ID", required = true)
        @PathParam("storeId")
        storeId: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        @BkField(patternStyle = BkStyleEnum.CODE_STYLE)
        storeCode: String,
        @Parameter(description = "评论信息请求报文体", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?>

    @Operation(summary = "更新组件评论")
    @PUT
    @Path("/{commentId}/update")
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
    @Path("/{commentId}/praise")
    fun updateStoreCommentPraiseCount(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<Int>
}
