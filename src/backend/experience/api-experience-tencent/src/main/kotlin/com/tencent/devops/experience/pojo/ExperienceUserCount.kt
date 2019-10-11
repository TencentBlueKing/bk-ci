package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-用户下载统计")
data class ExperienceUserCount(
    @ApiModelProperty("用户名", required = true)
    val userId: String,
    @ApiModelProperty("下载次数", required = true)
    val times: Int,
    @ApiModelProperty("最近下载时间", required = true)
    val latestTime: Long
)