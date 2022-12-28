package com.tencent.devops.dispatch.devcloud.pojo

data class Volume(
    val name: String,
    val volumeSource: VolumeSource? = null
)
