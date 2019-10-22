package com.tencent.devops.project.pojo

data class AuthProjectForCreateResult(
    val cc_app_id: Int,
    val client_id: String,
    val client_secret: String,
    val project_code: String,
    val project_id: String
)