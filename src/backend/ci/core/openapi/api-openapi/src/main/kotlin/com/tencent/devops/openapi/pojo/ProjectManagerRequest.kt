package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "获取项目管理员请求体")
data class ProjectManagerRequest(
    @get:Schema(description = "项目ID", required = true)
    val projectId: String
)
