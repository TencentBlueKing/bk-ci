package com.tencent.devops.project.pojo.tof

data class DeptStaffsRequest(
    val dept_id: String,
    val level: Int,
    val app_code: String,
    val app_secret: String
)
