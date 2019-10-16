package com.tencent.devops.common.web.mq.alert

data class NotifyUser(
    val userId: String,
    val notifyTypes: List<NotifyType>
)