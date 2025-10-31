package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class DepartmentInfo(
    @get:Schema(title = "部门Id")
    val departmentId: Int,
    @get:Schema(title = "部门名称")
    val departmentName: String,
    @get:Schema(title = "父级")
    val parent: Int,
    @get:Schema(title = "层级")
    val level: Int,
    @get:Schema(title = "是否有子部门")
    val hasChildren: Boolean
)
