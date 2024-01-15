package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "修改项目创建人")
data class ProjectUpdateCreatorDTO(
    @Schema(name = "项目code")
    val projectCode: String,
    @Schema(name = "创建人")
    val creator: String
)
