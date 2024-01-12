package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "取消Itsm单据接口")
data class ItsmCancelApplicationInfo(
    @Schema(description = "itsm单号")
    val sn: String,
    @Schema(description = "操作人")
    val operator: String,
    @Schema(description = "操作")
    @JsonProperty("action_type")
    val actionType: String,
    @Schema(description = "备注信息")
    @JsonProperty("action_message")
    val actionMessage: String? = "cancel ticket"
)
