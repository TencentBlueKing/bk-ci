package com.tencent.devops.dispatch.bcs.pojo

data class Volume(
    val name: String,
    val volumeSource: VolumeSource? = null
)
