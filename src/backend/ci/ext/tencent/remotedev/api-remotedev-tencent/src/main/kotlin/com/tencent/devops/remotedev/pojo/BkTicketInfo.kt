package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "更新bkticket信息")
data class BkTicketInfo(
    @Schema(description = "bkTicket")
    val bkTicket: String,
    @Schema(description = "hostName")
    val hostName: String,
    @Schema(description = "mountType")
    val mountType: WorkspaceMountType
)
