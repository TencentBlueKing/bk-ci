package com.tencent.devops.dispatch.devcloud.pojo

data class VolumeSource(
    val emptyDir: EmptyDirVolumeSource,
    val dataDisk: DataDiskSource
)
