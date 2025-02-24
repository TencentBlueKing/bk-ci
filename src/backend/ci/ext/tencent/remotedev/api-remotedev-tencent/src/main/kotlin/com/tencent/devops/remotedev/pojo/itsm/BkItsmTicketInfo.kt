package com.tencent.devops.remotedev.pojo.itsm

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新bkticket信息")
data class BKItsmCreateTicketReq(
    @JsonProperty("service_id")
    val serviceId: Int,
    val creator: String,
    // [{"key": "title", "value": "d" }]
    val fields: List<Map<String, String>>,
    val meta: Map<String, Any>? = null
)

data class BKItsmCreateTicketResp<T>(
    val result: Boolean,
    val message: String,
    val code: String,
    val data: T
)

data class BKItsmCreateTicketRespData(
    val sn: String,
    val id: Int,
    @JsonProperty("ticket_url")
    val ticketUrl: String
)
