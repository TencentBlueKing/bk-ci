package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-下载统计")
data class ExperienceCount(
    @ApiModelProperty("用户数", required = true)
    val downloadUsers: Long,
    @ApiModelProperty("下载数", required = true)
    val downloadTimes: Long
)