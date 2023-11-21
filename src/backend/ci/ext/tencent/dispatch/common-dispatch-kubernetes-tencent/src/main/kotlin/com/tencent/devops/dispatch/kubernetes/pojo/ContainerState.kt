package com.tencent.devops.dispatch.kubernetes.pojo

data class ContainerState(
    val waiting: ContainerStateWaiting? = null,
    val running: ContainerStateRunning? = null,
    val terminated: ContainerStateTerminated? = null
)
