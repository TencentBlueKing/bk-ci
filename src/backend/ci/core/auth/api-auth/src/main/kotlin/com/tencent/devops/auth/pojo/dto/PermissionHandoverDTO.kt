package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "权限交接请求体")
data class PermissionHandoverDTO(
    @Schema(name = "交接项目集合")
    val projectList: List<String>,
    @Schema(name = "交接用户")
    val handoverFrom: String,
    @Schema(name = "授予用户")
    val handoverToList: List<String>,
    @Schema(name = "资源类型")
    val resourceType: String?,
    @Schema(name = "是否交接管理员权限")
    val managerPermission: Boolean
)
