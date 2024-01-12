package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目--权限")
data class ProjectWithPermission(
    @Schema(description = "项目名称")
    val projectName: String,
    @Schema(description = "项目英文名称")
    val englishName: String,
    @Schema(description = "权限")
    val permission: Boolean,
    @Schema(description = "环境路由")
    val routerTag: String?
)
