package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "权限交接请求体")
data class PermissionHandoverDTO(
    @Schema(description = "交接项目集合")
    val projectList: List<String>,
    @Schema(description = "交接用户")
    val handoverFrom: String,
    @Schema(description = "授予用户")
    val handoverToList: List<String>,
    @Schema(description = "资源类型")
    val resourceType: String?,
    @Schema(description = "是否交接管理员权限")
    val managerPermission: Boolean
)
