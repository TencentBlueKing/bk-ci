package com.tencent.devops.dispatch.listener

import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import com.tencent.devops.dispatch.service.tpaqueue.TPAQueueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TPAQueueListener @Autowired constructor(
    private val tpaQueueService: TPAQueueService
) {
    fun listenTpAgentQueueEvent(event: TPAQueueEvent) {
        tpaQueueService.doQueue(event)
    }
}