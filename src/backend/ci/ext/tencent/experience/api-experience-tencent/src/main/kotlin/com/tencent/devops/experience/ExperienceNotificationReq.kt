package com.tencent.devops.experience

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验--发送通知")
data class ExperienceNotificationReq(
    @ApiModelProperty("体验ID列表", required = true)
    val experienceIds: List<String>
)
