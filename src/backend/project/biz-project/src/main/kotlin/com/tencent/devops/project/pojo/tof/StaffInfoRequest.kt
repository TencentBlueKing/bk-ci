package com.tencent.devops.project.pojo.tof

data class StaffInfoRequest(
    val app_code: String,
    val app_secret: String,
    val operator: String?,
    val login_name: String,
    val bk_ticket: String
)