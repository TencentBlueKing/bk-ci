package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "取消Itsm单据接口")
data class ItsmCancelApplicationInfo(
    @Schema(name = "itsm单号")
    val sn: String,
    @Schema(name = "操作人")
    val operator: String,
    @Schema(name = "操作")
    @JsonProperty("action_type")
    val actionType: String,
    @Schema(name = "备注信息")
    @JsonProperty("action_message")
    val actionMessage: String? = "cancel ticket"
)
