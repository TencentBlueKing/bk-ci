package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新bkticket信息")
data class BkTicketInfo(
    @get:Schema(title = "bkTicket")
    val bkTicket: String,
    @get:Schema(title = "hostName")
    val hostName: String,
    @get:Schema(title = "mountType")
    val mountType: WorkspaceMountType
)
