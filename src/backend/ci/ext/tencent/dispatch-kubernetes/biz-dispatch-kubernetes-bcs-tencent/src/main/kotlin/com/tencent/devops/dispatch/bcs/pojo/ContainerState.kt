package com.tencent.devops.dispatch.bcs.pojo

data class ContainerState(
    val waiting: ContainerStateWaiting? = null,
    val running: ContainerStateRunning? = null,
    val terminated: ContainerStateTerminated? = null
)
