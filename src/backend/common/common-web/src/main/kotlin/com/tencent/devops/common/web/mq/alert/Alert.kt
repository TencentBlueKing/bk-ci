package com.tencent.devops.common.web.mq.alert

data class Alert(
    val module: String,
    val level: AlertLevel,
    val title: String,
    val message: String
)