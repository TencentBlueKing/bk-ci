package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-跳转信息")
data class ExperienceJumpInfo(
    @ApiModelProperty("跳转scheme", required = true)
    val scheme: String,
    @ApiModelProperty("跳转url", required = true)
    val url: String
)
