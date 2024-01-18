package com.tencent.devops.dispatch.kubernetes.pojo

data class SeccompProfile(
    val type: String,
    val localhostProfile: String
)
