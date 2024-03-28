package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "V2-体验列表")
data class ExperienceList(
    @get:Schema(title = "内部体验列表")
    val privateExperiences: List<AppExperience>,
    @get:Schema(title = "公开体验列表")
    val publicExperiences: List<AppExperience>,
    @get:Schema(title = "红点个数")
    val redPointCount: Int
)
