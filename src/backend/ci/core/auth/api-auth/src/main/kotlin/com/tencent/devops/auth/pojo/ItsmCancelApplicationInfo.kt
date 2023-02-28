package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("取消Itsm单据接口")
data class ItsmCancelApplicationInfo(
    @ApiModelProperty("itsm单号")
    val sn: String,
    @ApiModelProperty("操作人")
    val operator: String,
    @ApiModelProperty("操作")
    @JsonProperty("action_type")
    val actionType: String,
    @ApiModelProperty("备注信息")
    @JsonProperty("action_message")
    val actionMessage: String? = "cancel ticket"
)
