package com.tencent.devops.notify.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("消息通知的标题和内容")
data class NotifyContext(
    @ApiModelProperty("消息标题", required = true)
    val title: String,
    @ApiModelProperty("消息文本", required = true)
    val body: String
)