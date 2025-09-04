package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "配额api返回")
data class QuotaInApiRes(
    @get:Schema(title = "个人配额")
    val user: Int? = null,
    @get:Schema(title = "项目配额")
    val project: Int? = null,
    @get:Schema(title = "特殊配额")
    val quotas: Map<String, Int>? = null
)
