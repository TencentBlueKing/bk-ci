package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "修改项目创建人")
data class ProjectUpdateCreatorDTO(
    @Schema(description = "项目code")
    val projectCode: String,
    @Schema(description = "创建人")
    val creator: String
)
