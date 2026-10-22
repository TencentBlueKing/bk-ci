package com.tencent.devops.common.websocket.v2

import java.util.UUID

data class WsInvalidation(
    val topic: String,
    val eventId: String = UUID.randomUUID().toString(),
    val emittedAt: Long = System.currentTimeMillis()
)

fun interface WsInvalidationPublisher {
    fun publish(event: WsInvalidation)
}
