package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-体验信息-根据构建")
data class ExperienceInfoForBuild(
    @get:Schema(title = "体验名称", required = true)
    val experienceName: String,
    @get:Schema(title = "版本标题", required = true)
    val versionTitle: String,
    @get:Schema(title = "体验描述", required = true)
    val remark: String,
    @get:Schema(title = "schema跳转链接", required = true)
    val scheme: String,
    @get:Schema(title = "体验id", required = true)
    val experienceId: String
)
