package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("更新bkticket信息")
data class BkTicketInfo(
    @ApiModelProperty("bkTicket")
    val bkTicket: String,
    @ApiModelProperty("hostName")
    val hostName: String
)
