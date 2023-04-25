package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验--创建体验--出参")
data class ExperienceCreateResp(
    @ApiModelProperty("体验详情分享页面", required = true)
    val url: String,
    @ApiModelProperty("体验ID", required = true)
    val experienceHashId: String
)
