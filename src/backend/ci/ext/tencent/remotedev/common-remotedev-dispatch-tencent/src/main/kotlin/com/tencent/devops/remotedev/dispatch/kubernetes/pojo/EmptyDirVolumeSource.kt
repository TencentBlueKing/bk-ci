package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class EmptyDirVolumeSource(
    val medium: String,
    val sizeLimit: Int
)
