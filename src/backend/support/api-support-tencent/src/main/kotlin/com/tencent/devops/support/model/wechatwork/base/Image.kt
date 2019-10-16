package com.tencent.devops.support.model.wechatwork.base

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("图片")
data class Image(
    @ApiModelProperty("图片内容")
    val media_id: String
)