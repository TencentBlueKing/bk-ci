package com.tencent.devops.dockerhost.pojo

data class DockerHostLoad(
    val usedContainerNum: Int,
    val averageCpuLoad: Int,
    val averageMemLoad: Int,
    val averageDiskLoad: Int,
    val averageDiskIOLoad: Int
)