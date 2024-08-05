package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class SeccompProfile(
    val type: String,
    val localhostProfile: String
)
