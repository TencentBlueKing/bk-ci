package com.tencent.devops.remotedev.pojo.notification

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "标记已读请求")
data class MarkReadRequest(
    @get:Schema(title = "通知ID列表")
    val notifyIds: List<Long>
)

@Schema(title = "清除通知请求")
data class ClearNotifyRequest(
    @get:Schema(title = "分类筛选")
    val category: String? = null
)
