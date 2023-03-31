package com.tencent.devops.dispatch.devcloud.pojo

data class SELinuxOptions(
    val user: String,
    val role: String,
    val type: String,
    val level: String
)
