package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "清理体验")
data class ExperienceClean(
    @get:Schema(title = "体验ID列表", required = false)
    val experienceIds: List<Long>? = emptyList()
)
