package com.tencent.devops.project.pojo.tof

data class DeptStaffsRequest(
    val dept_id: String,
    val level: Int,
    val bk_app_code: String,
    val bk_app_secret: String
)
