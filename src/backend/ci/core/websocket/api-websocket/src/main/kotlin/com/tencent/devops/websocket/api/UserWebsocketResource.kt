package com.tencent.devops.websocket.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*


@Api(tags = ["USER_WEBSOCKET"], description = "websocket-用户调用")
@Path("/user/websocket/sessions")
interface UserWebsocketResource {

    @POST
    @Path("/{sessionId}/userIds/{userId}/clear")
    @ApiOperation("页面退出清理session")
    fun clearSession(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("SessionID", required = true)
        @PathParam("sessionId")
        sessionId: String
    ): Result<Boolean>
}