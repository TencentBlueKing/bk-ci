package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新bkticket信息")
data class BkItsmTicketInfo(
    @JsonProperty("service_id")
    @get:Schema(title = "serviceId")
    val serviceId: Int,
    @get:Schema(title = "creator")
    val creator: String,
    @get:Schema(title = "fields")
    val fields: List<Map<String, String>>,
    @get:Schema(title = "meta")
    val meta: Map<String, Any>? = null
)
