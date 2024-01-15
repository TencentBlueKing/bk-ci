package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "版本体验-跳转信息")
data class ExperienceJumpInfo(
    @Schema(description = "跳转scheme", required = true)
    val scheme: String,
    @Schema(description = "跳转url", required = true)
    val url: String
)
