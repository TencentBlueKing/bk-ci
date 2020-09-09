package com.tencent.devops.websocket.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.websocket.api.UserWebsocketResource
import com.tencent.devops.websocket.servcie.WebsocketService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserWebsocketResourceImpl @Autowired constructor(
    val websocketService: WebsocketService

) : UserWebsocketResource {
    override fun clearSession(userId: String, sessionId: String): Result<Boolean> {
        return websocketService.clearAllBySession(userId, sessionId)
    }
}