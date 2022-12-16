package com.tencent.devops.dispatch.devcloud.pojo

data class ContainerStatus(
    val name: String,
    val state: ContainerState,
    val lastState: ContainerState,
    val ready: Boolean,
    val restartCount: Int,
    val image: String,
    val imageID: String,
    val containerID: String,
    val started: Boolean,
)
