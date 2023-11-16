package com.tencent.devops.dispatch.kubernetes.pojo

data class Volume(
    val name: String,
    val volumeSource: VolumeSource? = null
)
