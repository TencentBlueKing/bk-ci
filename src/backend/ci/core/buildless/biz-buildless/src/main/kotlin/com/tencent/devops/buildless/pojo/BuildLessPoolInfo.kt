package com.tencent.devops.buildless.pojo

import com.tencent.devops.buildless.utils.ContainerStatus

data class BuildLessPoolInfo(
    val status: ContainerStatus,
    val buildLessTask: BuildLessTask? = null
)
