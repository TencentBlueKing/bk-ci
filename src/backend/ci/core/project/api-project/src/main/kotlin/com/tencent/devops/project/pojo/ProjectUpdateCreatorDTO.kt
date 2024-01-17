package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修改项目创建人")
data class ProjectUpdateCreatorDTO(
    @Schema(title = "项目code")
    val projectCode: String,
    @Schema(title = "创建人")
    val creator: String
)
