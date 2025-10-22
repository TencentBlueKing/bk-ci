package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "撤销工单")
data class ItsmTicketsRevoked(
    @get:Schema(title = "系统标识")
    val system_id: String,
    @get:Schema(title = "工单标识")
    val ticket_id: String
)
