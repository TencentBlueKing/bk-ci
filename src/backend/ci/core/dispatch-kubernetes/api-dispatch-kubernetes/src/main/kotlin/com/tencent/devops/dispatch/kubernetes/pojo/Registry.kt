package com.tencent.devops.dispatch.kubernetes.pojo

data class Registry(
    val host: String,
    val username: String?,
    val password: String?
)
