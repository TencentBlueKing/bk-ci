package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "专家协助工单记录")
data class QuotaInApiRes(
    @get:Schema(title = "个人配额")
    val user: Int? = null,
    @get:Schema(title = "项目配额")
    val project: Int? = null
)
