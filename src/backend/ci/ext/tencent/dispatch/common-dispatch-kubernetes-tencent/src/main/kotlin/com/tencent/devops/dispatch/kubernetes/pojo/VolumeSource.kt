package com.tencent.devops.dispatch.kubernetes.pojo

data class VolumeSource(
    val emptyDir: EmptyDirVolumeSource? = null,
    val dataDisk: DataDiskSource? = null
)
