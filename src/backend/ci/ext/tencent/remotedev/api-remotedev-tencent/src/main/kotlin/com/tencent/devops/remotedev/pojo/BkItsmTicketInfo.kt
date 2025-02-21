package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新bkticket信息")
data class BkItsmTicketInfo(
    @get:Schema(title = "serviceId")
    val serviceId: Int,
    @get:Schema(title = "creator")
    val creator: String,
    @get:Schema(title = "fields")
    val fields: List<Map<String, String>>
)
