package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

data class ContainerStatus(
    val name: String,
    val state: ContainerState?,
    val status: String?,
    val lastState: ContainerState?,
    val ready: Boolean?,
    val restartCount: Int?,
    val image: String,
    val imageID: String,
    val containerID: String,
    val started: Boolean?
)
