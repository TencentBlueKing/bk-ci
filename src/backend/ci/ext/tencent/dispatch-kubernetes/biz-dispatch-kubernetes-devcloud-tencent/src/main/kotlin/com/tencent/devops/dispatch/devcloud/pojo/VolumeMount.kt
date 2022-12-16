package com.tencent.devops.dispatch.devcloud.pojo

data class VolumeMount(
    val name: String,
    val readOnly: Boolean,
    val mountPath: String,
    val subPath: String,
    val mountPropagation: String,
)
