package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "取消Itsm单据接口")
data class ItsmCancelApplicationInfo(
    @get:Schema(title = "itsm单号")
    val sn: String,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "操作")
    @JsonProperty("action_type")
    val actionType: String,
    @get:Schema(title = "备注信息")
    @JsonProperty("action_message")
    val actionMessage: String? = "cancel ticket"
)
