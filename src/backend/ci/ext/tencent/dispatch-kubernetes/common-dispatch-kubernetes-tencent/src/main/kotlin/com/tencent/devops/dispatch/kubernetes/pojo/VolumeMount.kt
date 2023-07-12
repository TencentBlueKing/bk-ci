package com.tencent.devops.dispatch.kubernetes.pojo

data class VolumeMount(
    val name: String? = null,
    val readOnly: Boolean? = null,
    val mountPath: String? = null,
    val subPath: String? = null,
    val mountPropagation: String? = null
)
