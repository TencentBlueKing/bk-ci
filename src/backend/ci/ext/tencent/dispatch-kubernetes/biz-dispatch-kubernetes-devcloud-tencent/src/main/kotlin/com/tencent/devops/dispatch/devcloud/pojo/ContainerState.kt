package com.tencent.devops.dispatch.devcloud.pojo

data class ContainerState(
    val waiting: ContainerStateWaiting,
    val running: ContainerStateRunning,
    val terminated: ContainerStateTerminated
)
