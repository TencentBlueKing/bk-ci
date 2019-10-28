package com.tencent.devops.monitoring.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("NOC告警通知用户信息")
data class NocNoticeUserInfo(
    @ApiModelProperty("用户名", required = true)
    @JsonProperty("username")
    val username: String,
    @ApiModelProperty("手机号", required = false)
    @JsonProperty("mobile_phone")
    val mobilePhone: String? = null
)