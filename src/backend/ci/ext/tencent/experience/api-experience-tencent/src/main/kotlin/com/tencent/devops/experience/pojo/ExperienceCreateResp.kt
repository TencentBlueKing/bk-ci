package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "版本体验--创建体验--出参")
data class ExperienceCreateResp(
    @Schema(description = "体验详情分享页面", required = true)
    val url: String,
    @Schema(description = "体验ID", required = true)
    val experienceHashId: String
)
