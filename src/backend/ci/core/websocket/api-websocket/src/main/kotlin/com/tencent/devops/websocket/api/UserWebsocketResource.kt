package com.tencent.devops.websocket.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_WEBSOCKET"], description = "websocket-用户调用")
@Path("/user/websocket/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.TEXT_PLAIN)
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