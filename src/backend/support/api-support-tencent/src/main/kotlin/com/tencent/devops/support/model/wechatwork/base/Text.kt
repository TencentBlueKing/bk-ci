package com.tencent.devops.support.model.wechatwork.base

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文本")
data class Text(
    @ApiModelProperty("文本内容")
    val content: String
)