package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreCommentReplyInfo
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE_COMMENT_REPLY"], description = "镜像-镜像评论回复")
@Path("/user/market/image/comment/reply")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageCommentReplyResource {

    @ApiOperation("获取镜像相应评论的回复列表")
    @GET
    @Path("/comments/{commentId}/replys")
    fun getStoreCommentReplysByCommentId(
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String
    ): Result<List<StoreCommentReplyInfo>?>

    @ApiOperation("镜像评论回复")
    @POST
    @Path("/comments/{commentId}/reply")
    fun addStoreCommentReply(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("评论ID", required = true)
        @PathParam("commentId")
        commentId: String,
        @ApiParam("评论回复信息请求报文体", required = true)
        storeCommentReplyRequest: StoreCommentReplyRequest
    ): Result<StoreCommentReplyInfo?>
}