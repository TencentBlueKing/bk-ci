package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目--权限")
data class ProjectWithPermission(
    @Schema(title = "项目名称")
    val projectName: String,
    @Schema(title = "项目英文名称")
    val englishName: String,
    @Schema(title = "权限")
    val permission: Boolean,
    @Schema(title = "环境路由")
    val routerTag: String?
)
