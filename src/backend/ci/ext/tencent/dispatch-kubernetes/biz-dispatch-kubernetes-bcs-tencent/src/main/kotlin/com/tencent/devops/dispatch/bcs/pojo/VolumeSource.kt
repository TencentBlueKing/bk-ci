package com.tencent.devops.dispatch.bcs.pojo

data class VolumeSource(
    val emptyDir: EmptyDirVolumeSource? = null,
    val dataDisk: DataDiskSource? = null
)
