package com.tencent.devops.remotedev.pojo.notification

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户通知列表请求")
data class UserNotifyListRequest(
    @get:Schema(title = "页码", defaultValue = "1")
    val page: Int = 1,
    @get:Schema(title = "每页数量", defaultValue = "20")
    val pageSize: Int = 20,
    @get:Schema(title = "分类筛选")
    val category: String? = null
)

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
