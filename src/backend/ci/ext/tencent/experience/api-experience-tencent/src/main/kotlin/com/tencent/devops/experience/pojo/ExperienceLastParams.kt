package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-上一次参数")
data class ExperienceLastParams(
    @ApiModelProperty("是否存在", required = true)
    val exist: Boolean,
    @ApiModelProperty("参数", required = false)
    val experienceCreate: ExperienceCreate?
)
