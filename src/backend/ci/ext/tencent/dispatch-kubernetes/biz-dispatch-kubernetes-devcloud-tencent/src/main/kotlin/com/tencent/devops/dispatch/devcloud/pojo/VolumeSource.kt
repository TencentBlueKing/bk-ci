package com.tencent.devops.dispatch.devcloud.pojo

data class VolumeSource(
    val emptyDir: EmptyDirVolumeSource? = null,
    val dataDisk: DataDiskSource? = null
)
