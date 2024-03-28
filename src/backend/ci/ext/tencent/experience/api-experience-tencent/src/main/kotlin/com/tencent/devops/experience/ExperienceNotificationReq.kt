package com.tencent.devops.experience

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验--发送通知")
data class ExperienceNotificationReq(
    @get:Schema(title = "体验ID列表", required = true)
    val experienceIds: List<String>
)
