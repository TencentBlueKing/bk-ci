package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验--创建体验--出参")
data class ExperienceCreateResp(
    @get:Schema(title = "体验详情分享页面", required = true)
    val url: String,
    @get:Schema(title = "体验ID", required = true)
    val experienceHashId: String
)
