package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创建项目")
data class CreateProjectVO(
    @get:Schema(title = "状态" )
    val status: Boolean,
    @get:Schema(title = "项目ID")
    val projectId: String
)
