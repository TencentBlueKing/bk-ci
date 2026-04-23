package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按中文名解析出的用户")
data class ResolvedUserByNameVO(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "用户中文显示名")
    val userName: String,
    @get:Schema(title = "部门名称")
    val departmentName: String? = null,
    @get:Schema(title = "是否启用")
    val enabled: Boolean = true,
    @get:Schema(title = "是否离职")
    val departed: Boolean = false
)
