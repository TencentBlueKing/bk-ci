package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class UserInfo(
    @get:Schema(title = "用户Id")
    val userId: String,
    @get:Schema(title = "用户名")
    val userName: String,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "部门名称")
    val departmentName: String?,
    @get:Schema(title = "部门Id")
    val departmentId: Int?,
    @get:Schema(title = "用户部门")
    val departments: List<BkUserDeptInfo>?,
    @get:Schema(title = "部门路径")
    val path: List<Int>?,
    @get:Schema(title = "是否离职")
    val departed: Boolean
)
