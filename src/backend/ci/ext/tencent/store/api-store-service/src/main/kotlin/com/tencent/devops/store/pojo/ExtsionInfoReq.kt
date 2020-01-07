package com.tencent.devops.store.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务扩展点--该对象可能会要调整")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtsionInfoReq (
    @ApiModelProperty("页面服务模块")
    val serviceModel: String,
    @ApiModelProperty("页面动作")
    val serviceEvent: String
)