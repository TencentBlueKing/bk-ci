package com.tencent.devops.websocket.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.websocket.api.UserWebsocketResource
import com.tencent.devops.websocket.servcie.WebsocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserWebsocketResourceImpl @Autowired constructor(
    val websocketService: WebsocketService,
    val redisOperation: RedisOperation

) : UserWebsocketResource {
    override fun clearSession(userId: String, sessionId: String): Result<Boolean> {
        logger.info("clearSession| $userId| $sessionId")
        val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
        if (page != null) {
            logger.info("$sessionId| ws loginOut fail, page[$page], refresh by interface")
            websocketService.clearUserSession(userId, sessionId, null)
            websocketService.loginOut(userId, sessionId, page)
        }
        return Result(true)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}