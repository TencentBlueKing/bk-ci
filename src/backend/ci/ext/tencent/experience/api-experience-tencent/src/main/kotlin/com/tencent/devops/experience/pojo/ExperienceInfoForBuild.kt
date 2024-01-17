package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本体验-体验信息-根据构建")
data class ExperienceInfoForBuild(
    @Schema(title = "体验名称", required = true)
    val experienceName: String,
    @Schema(title = "版本标题", required = true)
    val versionTitle: String,
    @Schema(title = "体验描述", required = true)
    val remark: String,
    @Schema(title = "schema跳转链接", required = true)
    val scheme: String,
    @Schema(title = "体验id", required = true)
    val experienceId: String
)
