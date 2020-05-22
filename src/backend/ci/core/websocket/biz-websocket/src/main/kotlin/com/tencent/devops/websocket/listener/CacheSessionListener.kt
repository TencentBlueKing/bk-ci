package com.tencent.devops.websocket.listener

import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.websocket.event.ClearSessionEvent
import com.tencent.devops.websocket.servcie.WebsocketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CacheSessionListener @Autowired constructor(
    private val websocketService: WebsocketService
) : Listener<ClearSessionEvent> {
    override fun execute(event: ClearSessionEvent) {
        if (websocketService.isCacheSession(event.sessionId)) {
            logger.info("clear cache session by mq fanout, userId[${event.userId}] sessionId[${event.sessionId}]")
            websocketService.removeCacheSession(event.sessionId)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}