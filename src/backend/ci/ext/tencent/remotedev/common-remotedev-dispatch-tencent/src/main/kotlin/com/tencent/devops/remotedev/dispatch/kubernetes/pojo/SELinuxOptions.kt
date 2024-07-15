package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class SELinuxOptions(
    val user: String,
    val role: String,
    val type: String,
    val level: String
)
