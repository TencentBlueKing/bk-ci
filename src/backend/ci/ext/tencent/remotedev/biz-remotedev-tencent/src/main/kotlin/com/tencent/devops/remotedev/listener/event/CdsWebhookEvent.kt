package com.tencent.devops.remotedev.listener.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.remotedev.RemoteDevMQ

@Event(RemoteDevMQ.REMOTE_CDS_WEBHOOK)
data class CdsWebhookEvent(
    val userId: String,
    val type: Type,
    val envId: String,
    override var delayMills: Int = 0,
    override var retryTime: Int = 0
) : IEvent() {
    enum class Type {
        LOGIN,
        LOGOUT;

        companion object {
            fun fromString(value: String): Type? {
                return when (value) {
                    "LOGIN" -> LOGIN
                    "LOGOUT" -> LOGOUT
                    else -> null
                }
            }
        }
    }
}
