package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接请求体")
data class PermissionHandoverDTO(
    @get:Schema(title = "交接项目集合")
    val projectList: List<String>,
    @get:Schema(title = "交接用户")
    val handoverFrom: String,
    @get:Schema(title = "授予用户")
    val handoverToList: List<String>,
    @get:Schema(title = "资源类型")
    val resourceType: String?,
    @get:Schema(title = "是否交接管理员权限")
    val managerPermission: Boolean
)
