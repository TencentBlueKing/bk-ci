package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-下载链接")
data class Url(
    @ApiModelProperty("下载链接", required = true)
    val url: String
)