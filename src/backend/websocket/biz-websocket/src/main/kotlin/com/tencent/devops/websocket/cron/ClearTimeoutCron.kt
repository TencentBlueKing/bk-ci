package com.tencent.devops.websocket.cron

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class ClearTimeoutCron(
        private val redisOperation: RedisOperation,
        private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * 每分钟一次，计算session是否已经超时，若超时，剔除该session关联的所有websocket redis信息。
     */
    @Scheduled(cron = "0 */1 * * * ?")
    fun clearTimeoutAllCache() {
        val nowTime = System.currentTimeMillis()
        val sessionTimeoutStr = RedisUtlis.getSessionTimeOutFromRedis(redisOperation)
        if (sessionTimeoutStr != null) {
            val sessionTimeoutMap: MutableMap<String, String> = objectMapper.readValue(sessionTimeoutStr)
            val newSessionMap = mutableMapOf<String, String>()
            sessionTimeoutMap.forEach { (sessionId, key) ->
                val timeout: Long = key.substringBefore("#").toLong()
                val userId = key.substringAfter("#")
                if (nowTime < timeout) {
                    newSessionMap[sessionId] = key
                } else {
                    val sessionPage = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
                    RedisUtlis.cleanSessionPageBySessionId(redisOperation, sessionId)
                    if (sessionPage != null) {
                        RedisUtlis.cleanPageSessionBySessionId(redisOperation, sessionId, sessionPage)
                    }
                    RedisUtlis.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
                    logger.info("[clearTimeOutSession] sessionId:$sessionId,loadPage:$sessionPage,userId:$userId")
                }
            }
            RedisUtlis.saveSessionTimeOutAll(redisOperation, objectMapper.writeValueAsString(newSessionMap))
        }
    }
}