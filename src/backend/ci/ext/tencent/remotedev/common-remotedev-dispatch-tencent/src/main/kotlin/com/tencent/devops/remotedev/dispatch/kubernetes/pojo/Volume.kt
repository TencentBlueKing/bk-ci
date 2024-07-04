package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class Volume(
    val name: String,
    val volumeSource: VolumeSource? = null
)
