package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class VolumeSource(
    val emptyDir: EmptyDirVolumeSource? = null,
    val dataDisk: DataDiskSource? = null
)
