package com.tencent.devops.store.api.ideatom

import com.tencent.devops.store.pojo.common.StoreCommentInfo
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import com.tencent.devops.store.pojo.common.StoreCommentScoreInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["USER_MARKET_IDE_ATOM_COMMENT"], description = "IDE插件-IDE插件评论")
@Path("/user/market/ideAtom/comment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserIdeAtomCommentResource {

    @ApiOperation("获取IDE插件评论接口")
    @GET
    @Path("/comments/{commentId}")
    fun getStoreComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<StoreCommentInfo?>

    @ApiOperation("获取IDE插件的评论列表")
    @GET
    @Path("/atomCodes/{atomCode}/comments")
    fun getStoreComments(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("IDE插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<StoreCommentInfo>?>

    @ApiOperation("获取IDE插件的评分详情")
    @GET
    @Path("/score/atomCodes/{atomCode}")
    fun getAtomCommentScoreInfo(
        @ApiParam("IDE插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<StoreCommentScoreInfo>

    @ApiOperation("新增IDE插件评论")
    @POST
    @Path("/atomIds/{atomId}/atomCodes/{atomCode}/comment")
    fun addAtomComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("IDE插件ID", required = true)
        @PathParam("atomId")
        atomId: String,
        @ApiParam("IDE插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("评论信息请求报文体", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<StoreCommentInfo?>

    @ApiOperation("更新IDE插件评论")
    @PUT
    @Path("/comments/{commentId}")
    fun updateStoreComment(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String,
        @ApiParam("评论信息请求报文体", required = true)
        storeCommentRequest: StoreCommentRequest
    ): Result<Boolean>

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