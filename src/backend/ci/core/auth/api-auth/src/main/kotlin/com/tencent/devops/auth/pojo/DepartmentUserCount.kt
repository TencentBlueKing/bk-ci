package com.tencent.devops.auth.pojo

data class DepartmentUserCount(
    val departmentName: String,
    val departmentId: Int,
    val hasChildren: Boolean,
    val userCount: Int
)
