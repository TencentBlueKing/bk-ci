package com.tencent.devops.support.model.wechatwork.base

import com.tencent.devops.support.model.wechatwork.enums.ReceiverType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("接受者")
data class Receiver(
    @ApiModelProperty("接受者类型")
    val type: ReceiverType,
    @ApiModelProperty("会话ID/用户ID/rtx号")
    val id: String
)