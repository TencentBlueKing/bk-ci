package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目--权限")
data class ProjectWithPermission(
    @Schema(name = "项目名称")
    val projectName: String,
    @Schema(name = "项目英文名称")
    val englishName: String,
    @Schema(name = "权限")
    val permission: Boolean,
    @Schema(name = "环境路由")
    val routerTag: String?
)
