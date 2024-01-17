package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新bkticket信息")
data class BkTicketInfo(
    @Schema(title = "bkTicket")
    val bkTicket: String,
    @Schema(title = "hostName")
    val hostName: String,
    @Schema(title = "mountType")
    val mountType: WorkspaceMountType
)
