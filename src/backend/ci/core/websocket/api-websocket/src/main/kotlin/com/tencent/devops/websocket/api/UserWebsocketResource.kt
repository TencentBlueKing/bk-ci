package com.tencent.devops.websocket.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Api(tags = ["USER_WEBSOCKET"], description = "websocket-用户调用")
@Path("/user/websocket/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserWebsocketResource {

    @PUT
    @Path("/{sessionId}/userIds/{userId}/clear")
    @ApiOperation("页面退出清理session")
    fun clearSession(
        @ApiParam("用户ID", required = true)
        @PathParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("SessionID", required = true)
        @PathParam("sessionId")
        sessionId: String
    ): Result<Boolean>
}