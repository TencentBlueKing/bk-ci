package com.tencent.bk.codecc.codeccjob.controller

import com.tencent.bk.codecc.codeccjob.pojo.CleanMessage
import com.tencent.devops.common.constant.RedisKeyConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class WebSocketController @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketController::class.java)
    }

    @MessageMapping("/cleanTaskSession")
    fun cleanTaskSession(cleanMessage: CleanMessage){
        logger.info("start to clean task session, task id: ${cleanMessage.taskId}")
        if(0L == cleanMessage.taskId){
            return
        }
        redisTemplate.delete("${RedisKeyConstants.TASK_WEBSOCKET_SESSION_PREFIX}${cleanMessage.taskId}")
    }
}