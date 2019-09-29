package com.tencent.devops.common.web.mq.alert

enum class AlertLevel(private val priority: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    fun compare(level: AlertLevel): Boolean {
        return this.priority >= level.priority
    }
}