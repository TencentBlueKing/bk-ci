package com.tencent.devops.project.pojo.tof

data class ChildDeptRequest(
    val app_code: String,
    val app_secret: String,
    val parent_dept_id: Int,
    val level: Int
)