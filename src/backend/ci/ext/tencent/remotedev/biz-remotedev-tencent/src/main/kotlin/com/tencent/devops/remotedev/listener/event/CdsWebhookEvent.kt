package com.tencent.devops.remotedev.listener.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.remotedev.RemoteDevMQ

@Event(RemoteDevMQ.REMOTE_CDS_WEBHOOK)
data class CdsWebhookEvent(
    val userId: String,
    val type: Type,
    val envId: String,
    val workspaceName: String? = null,
    val body: Map<String, String> = emptyMap(),
    override var delayMills: Int = 0,
    override var retryTime: Int = 0
) : IEvent() {
    enum class Type {
        CREATE, // 云桌面创建
        ASSIGN, // 云桌面分配拥有人
        LOGIN,  // 云桌面登录
        LOGOUT; // 云桌面退出

        companion object {
            fun fromWebhook(value: String): Type? {
                return when (value) {
                    "CREATE" -> CREATE
                    "LOGIN" -> LOGIN
                    "LOGOUT" -> LOGOUT
                    else -> null
                }
            }
        }
    }
}
